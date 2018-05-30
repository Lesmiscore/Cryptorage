package com.nao20010128nao.Cryptorage.cryptorage

import com.beust.klaxon.*
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.*
import kotlin.collections.ArrayList

internal class CryptorageImplV1(private val source: FileSource, private val keys: AesKeys) : Cryptorage {
    companion object {
        const val MANIFEST: String = "manifest"
        const val SPLIT_SIZE_DEFAULT: Int = 100 * 1024 /* 100kb */ - 16 /* Final block size */

        private fun populateKeys(password: String): AesKeys {
            val utf8Bytes1 = password.utf8Bytes()
            val utf8Bytes2 = "$password$password".utf8Bytes()
            val hash = MessageDigest.getInstance("sha-256")
            return Pair(hash.digest(hash.digest(utf8Bytes1)).takePrimitive(16), hash.digest(hash.digest(utf8Bytes2)).tailPrimitive(16))
        }
    }

    constructor(source: FileSource, password: String) : this(source, populateKeys(password))

    private val files: MutableMap<String, CryptorageFile> = readFiles()
    private val meta: MutableMap<String, String> = readMeta()

    /** Lists up file names */
    override fun list(): Array<String> = files.keys.sorted().toTypedArray()

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource {
        if (!has(name)) {
            throw FileNotFoundException(name)
        }
        val file = files[name]!!
        val indexOffset: Int = offset / file.splitSize
        val fileOffset: Int = offset % file.splitSize
        return ChainedDecryptor(source, keys, file.files.drop(indexOffset), fileOffset)
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        if (has(name)) {
            delete(name)
        }
        val splitSize = (meta[META_SPLIT_SIZE] ?: "$SPLIT_SIZE_DEFAULT").toInt()
        val file = CryptorageFile(ArrayList(), splitSize)
        files[name] = file
        commit()
        return ChainedEncryptor(source, splitSize, keys, file, { commit() })
    }

    /** Moves file */
    override fun mv(from: String, to: String) {
        if (!has(from)) {
            return
        }
        if (has(to)) {
            delete(to)
        }
        files[to] = files[from]!!
        commit()
    }

    /** Deletes a file */
    override fun delete(name: String) {
        if (!has(name)) {
            return
        }
        val removal = files[name]!!.files
        removal.forEach {
            source.delete(it)
        }
        files.remove(name)
        commit()
    }

    /** Checks last modified date and time */
    override fun lastModified(name: String): Long = when {
        has(name) -> files[name]!!.lastModified
        else -> 0
    }

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = source.isReadOnly

    override fun size(name: String): Long {
        if (!has(name)) {
            throw FileNotFoundException(name)
        }
        return files[name]!!.size
    }

    /** Removes unused files */
    override fun gc() {
        val ls = source.list().toList()
        val managed: MutableList<String> = arrayListOf()
        files.forEach { _, file ->
            managed.addAll(file.files)
        }
        val unused = ls - managed - MANIFEST
        unused.forEach {
            source.delete(it)
        }
    }

    override fun meta(key: String): String? = meta[key]

    override fun meta(key: String, value: String) {
        meta[key] = value
    }

    private data class CryptorageFile(var files: MutableList<String> = ArrayList(), var splitSize: Int = 0, var lastModified: Long = 0, var size: Long = 0) {
        constructor(file: JsonObject) :
                this(file.array<String>("files")!!.toMutableList(), file.int("splitSize")!!, file.long("lastModified")!!, file.long("size")!!)
    }

    private fun readFiles(): MutableMap<String, CryptorageFile> = when {
        source.has(MANIFEST) -> {
            val map: MutableMap<String, CryptorageFile> = HashMap()
            (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST), keys).asCharSource().openStream()) as JsonObject)
                    .obj("files")?.forEach { name, file ->
                        map[name] = CryptorageFile(file as JsonObject)
                    }
            map
        }
        else -> HashMap()
    }

    private fun readMeta(): MutableMap<String, String> = when {
        source.has(MANIFEST) -> {
            val map: MutableMap<String, String> = HashMap()
            (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST), keys).asCharSource().openStream()) as JsonObject)
                    .obj("meta")?.forEach { name, value ->
                        map[name] = "$value"
                    }
            map
        }
        else -> HashMap()
    }

    private fun commit() {
        val root = JsonObject()
        root["files"] = files.mapValues {
            mapOf(
                    "files" to it.value.files,
                    "splitSize" to it.value.splitSize,
                    "lastModified" to it.value.lastModified,
                    "size" to it.value.size
            )
        }
        root["meta"] = meta
        AesEncryptorByteSink(source.put("manifest"), keys).write(root.toJsonString(false).utf8Bytes())
    }

    private class ChainedDecryptor(
            private val source: FileSource,
            private val keys: AesKeys,
            private val files: List<String>,
            private val bytesToSkip: Int = 0
    ) : ByteSource() {
        override fun openStream(): InputStream = SequenceInputStream(
                files.asSequence()
                        .map { source.open(it) }
                        .map { AesDecryptorByteSource(it, keys) }
                        .map { it.openStream() }
                        .enumeration()
        ).also {
            ByteStreams.skipFully(it, bytesToSkip.toLong())
        }
    }

    private class ChainedEncryptor(
            private val source: FileSource,
            private val size: Int,
            private val keys: AesKeys,
            private val file: CryptorageFile,
            private val commit: () -> Unit
    ) : ByteSink() {
        var current: OutputStream? = null

        override fun openStream(): OutputStream {
            if (current != null) {
                current!!.close()
                file.files.forEach {
                    source.delete(it)
                }
                file.files = ArrayList()
            }
            file.lastModified = System.currentTimeMillis()
            commit()
            current = object : OutputStream() {
                var filling: OutputStream? = null

                init {
                    next(null)
                }

                private fun next(overflow: SizeLimitedOutputStream.OverflowError?) {
                    filling = SizeLimitedOutputStream(size, { me, next ->
                        closeCurrent(me)
                        next(next)
                    }, {
                        closeCurrent(it)
                    })
                    if (overflow != null) {
                        filling!!.write(overflow.buffer)
                    }
                }

                private fun closeCurrent(me: SizeLimitedOutputStream) {
                    val randName = generateRandomName()
                    AesEncryptorByteSink(source.put(randName), keys).openStream().also {
                        it.write(me.buffer, 0, me.size())
                        it.close()
                    }
                    file.files.add(randName)
                    file.size += me.size()
                    commit()
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

                override fun close() {
                    filling!!.close()
                }
            }
            return current!!
        }
    }
}
