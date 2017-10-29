package com.nao20010128nao.Cryptorage.cryptorage

import com.beust.klaxon.*
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import java.security.MessageDigest

class CryptorageImplV1(private val source: FileSource, password: String): Cryptorage{
    companion object {
        const val MANIFEST:String = "manifest"
    }

    private val keys=populateKeys(password)
    private val files=readFiles()

    /** Lists up file names */
    override fun list(): Array<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Opens file for reading */
    override fun open(name: String,offset:Int): ByteSource {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Moves file */
    override fun mv(from: String, to: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Deletes file(s) */
    override fun delete(name: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Checks last modified date and time */
    override fun lastModified(name: String): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = source.isReadOnly

    /** Removes unused files */
    override fun gc() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private fun populateKeys(password: String): AesKeys{
        val utf8Bytes1=password.utf8Bytes()
        val utf8Bytes2="$password$password".utf8Bytes()
        val hash=MessageDigest.getInstance("sha-256")
        return Pair(hash.digest(hash.digest(utf8Bytes1)).takePrimitive(16),hash.digest(hash.digest(utf8Bytes2)).tailPrimitive(16))
    }

    private data class CryptorageFile(var files: List<String> = ArrayList(),var splitSize: Int = 0,var lastModified: Long = 0) {
        constructor(file: JsonObject):
            this(file.array<String>("files")!!.toList(), file.int("splitSize")!!, file.long("lastModified")!!)
    }

    private fun readFiles():Map<String,CryptorageFile>{
        val map:MutableMap<String,CryptorageFile> = HashMap()
        (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST),keys).asCharSource().openStream()) as JsonObject)
                .obj("files")?.forEach{name,file->
            map[name]=CryptorageFile(file as JsonObject)
        }
        return map
    }
}
