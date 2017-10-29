package com.nao20010128nao.Cryptorage.cryptorage

import com.beust.klaxon.*
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import kotlin.collections.ArrayList

class CryptorageImplV1(private val source: FileSource, password: String): Cryptorage{
    companion object {
        const val MANIFEST:String = "manifest"
        const val SPLIT_SIZE:String = "split_size"
        const val SPLIT_SIZE_DEFAULT:Int = 100*1024 /* 100kb */
    }

    private val keys=populateKeys(password)
    private val files:MutableMap<String,CryptorageFile> =readFiles()
    private val meta:MutableMap<String,String> = readMeta()

    /** Lists up file names */
    override fun list(): Array<String> = files.keys.toTypedArray()

    /** Opens file for reading */
    override fun open(name: String,offset:Int): ByteSource {
        if(!has(name)){
            throw FileNotFoundException(name)
        }
        val file=files[name]!!
        val indexOffset:Int=offset/file.splitSize
        val fileOffset:Int=offset%file.splitSize
        return ChainedDecryptor(source,keys,file.files.drop(indexOffset),fileOffset)
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        if(has(name)){
            delete(name)
        }
        val splitSize=(meta[SPLIT_SIZE]?:"$SPLIT_SIZE_DEFAULT").toInt()
        val file= CryptorageFile(ArrayList(),splitSize,System.currentTimeMillis())
        return ChainedEncryptor(source,splitSize,keys,file)
    }

    /** Moves file */
    override fun mv(from: String, to: String) {
        if(!has(from)){
            return
        }
        if(has(to)){
            delete(to)
        }
        files[to] = files[from]!!
    }

    /** Deletes a file */
    override fun delete(name: String) {
        if(!has(name)){
            return
        }
        val removal=files[name]!!.files
        removal.forEach {
            source.delete(it)
        }
    }

    /** Checks last modified date and time */
    override fun lastModified(name: String): Long = when {
        has(name) -> files[name]!!.lastModified
        else -> 0
    }

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = source.isReadOnly

    /** Removes unused files */
    override fun gc() {
        val ls=source.list().toList()
        val managed:MutableList<String> = arrayListOf()
        files.forEach { _,file->
            managed.addAll(file.files)
        }
        val unused= ls - managed - MANIFEST
        unused.forEach {
            source.delete(it)
        }
    }


    private fun populateKeys(password: String): AesKeys{
        val utf8Bytes1=password.utf8Bytes()
        val utf8Bytes2="$password$password".utf8Bytes()
        val hash=MessageDigest.getInstance("sha-256")
        return Pair(hash.digest(hash.digest(utf8Bytes1)).takePrimitive(16),hash.digest(hash.digest(utf8Bytes2)).tailPrimitive(16))
    }

    private data class CryptorageFile(var files: MutableList<String> = ArrayList(),var splitSize: Int = 0,var lastModified: Long = 0) {
        constructor(file: JsonObject):
            this(file.array<String>("files")!!.toMutableList(), file.int("splitSize")!!, file.long("lastModified")!!)
    }

    private fun readFiles():MutableMap<String,CryptorageFile> = when {
        source.has(MANIFEST) -> {
            val map:MutableMap<String,CryptorageFile> = HashMap()
            (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST),keys).asCharSource().openStream()) as JsonObject)
                    .obj("files")?.forEach{name,file->
                map[name]=CryptorageFile(file as JsonObject)
            }
            map
        }
        else -> HashMap()
    }

    private fun readMeta(): MutableMap<String,String>  = when {
        source.has(MANIFEST) -> {
            val map:MutableMap<String,String> = HashMap()
            (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST),keys).asCharSource().openStream()) as JsonObject)
                    .obj("meta")?.forEach{name,value->
                map[name]="$value"
            }
            map
        }
        else -> HashMap()
    }

    private class ChainedDecryptor(private var source: FileSource,private var keys: AesKeys,private var files:List<String>,private var bytesToSkip: Int=0): ByteSource(){
        override fun openStream(): InputStream = SequenceInputStream(
               Collections.enumeration(files.map { source.open(it) }.map { AesDecryptorByteSource(it,keys) }.map { it.openStream() })
        ).also {
            ByteStreams.skipFully(it,bytesToSkip.toLong())
        }
    }

    private class ChainedEncryptor(private var source: FileSource,private val size: Int,private var keys: AesKeys,private var file: CryptorageFile): ByteSink(){
        var current: OutputStream? = null

        override fun openStream(): OutputStream {
            if(current!=null){
                current!!.close()
                file.files.forEach {
                    source.delete(it)
                }
                file.files=ArrayList()
            }
            current=object: OutputStream(){
                var filling: OutputStream? = null

                init {
                    next(null)
                }

                fun next(overflow: SizeLimitedOutputStream.OverflowError?) {
                    val cipher= Cipher.getInstance("AES/CBC/Pkcs7Padding")
                    val (key,iv) = keys.forCrypto()
                    cipher.init(Cipher.ENCRYPT_MODE,key,iv)
                    filling=CipherOutputStream(SizeLimitedOutputStream(size,{ me, overflow->
                        closeCurrent(me)
                        next(overflow)
                    },{
                        closeCurrent(it)
                    }),cipher)

                    if(overflow!=null){
                        filling!!.write(overflow.buffer)
                    }
                }

                fun closeCurrent(me:SizeLimitedOutputStream){
                    val randName= generateRandomName()
                    source.put(randName).openStream().also {
                        it.write(me.buffer)
                        it.close()
                    }
                    file.files.add(randName)
                }

                override fun write(p0: Int) {
                    filling!!.write(p0)
                }

                override fun write(p0: ByteArray?) {
                    filling!!.write(p0)
                }

                override fun write(p0: ByteArray?, p1: Int, p2: Int) {
                    filling!!.write(p0, p1, p2)
                }
            }
            return current!!
        }
    }
}
