package com.nao20010128nao.Cryptorage.cryptorage

import com.beust.klaxon.*
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import java.io.FileNotFoundException

internal class CryptorageImplV1(private val source: FileSource, private val keys: AesKeys) : Cryptorage {
    companion object {
        const val MANIFEST: String = "manifest"
        const val SPLIT_SIZE_DEFAULT: Int = 100 * 1024 /* 100kb */ - 16 /* Final block size */

        private fun populateKeys(password: String): AesKeys {
            val utf8Bytes1 = password.utf8Bytes()
            val utf8Bytes2 = "$password$password".utf8Bytes()
            return Pair(utf8Bytes1.digest().digest().leading(16), utf8Bytes2.digest().digest().trailing(16))
        }
    }

    constructor(source: FileSource, password: String) : this(source, populateKeys(password))

    private val index: Index = readIndex()

    /** Lists up file names */
    override fun list(): Array<String> = index.files.keys.sorted().toTypedArray()

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource {
        if (!has(name)) {
            throw FileNotFoundException(name)
        }
        val file = index.files[name]!!
        val indexOffset: Int = offset / file.splitSize
        val fileOffset: Int = offset % file.splitSize
        return ChainedDecryptor(source, keys, file.files.drop(indexOffset), fileOffset)
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        if (has(name)) {
            delete(name)
        }
        val splitSize = (index.meta[META_SPLIT_SIZE] ?: "$SPLIT_SIZE_DEFAULT").toInt()
        val file = CryptorageFile(splitSize = splitSize)
        index.files[name] = file
        commit()
        return ChainedEncryptor(source, splitSize, keys, file) { commit() }
    }

    /** Moves file */
    override fun mv(from: String, to: String) {
        if (!has(from)) {
            return
        }
        if (has(to)) {
            delete(to)
        }
        index.files[to] = index.files[from]!!
        commit()
    }

    /** Deletes a file */
    override fun delete(name: String) {
        if (!has(name)) {
            return
        }
        val removal = index.files[name]!!.files
        removal.forEach {
            source.delete(it)
        }
        index.files.remove(name)
        commit()
    }

    /** Checks last modified date and time */
    override fun lastModified(name: String): Long = when {
        has(name) -> index.files[name]!!.lastModified
        else -> 0
    }

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = source.isReadOnly

    override fun size(name: String): Long {
        if (!has(name)) {
            throw FileNotFoundException(name)
        }
        return index.files[name]!!.size
    }

    /** Removes unused files */
    override fun gc() {
        val ls = source.list().asList()
        val unused = ls - index.files.flatMap { it.value.files } - MANIFEST
        unused.forEach {
            source.delete(it)
        }
    }

    override fun meta(key: String): String? = index.meta[key]

    override fun meta(key: String, value: String) {
        index.meta[key] = value
    }


    private fun readIndex(): Index {
        if (!source.has(MANIFEST))
            return Index(hashMapOf(), hashMapOf())
        val data = (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST), keys).asCharSource().openStream()) as JsonObject)
        val files = data.obj("files")!!.mapValues { CryptorageFile(it.value as JsonObject) }
        val meta = data.obj("meta")!!.mapValues { "${it.value}" }
        return Index(files.toMutableMap(), meta.toMutableMap())
    }

    private fun commit() {
        val root = JsonObject()
        root["files"] = index.files.mapValues { it.value.toJsonMap() }
        root["meta"] = index.meta
        AesEncryptorByteSink(source.put(MANIFEST), keys)
                .write(root.toJsonString(false).utf8Bytes())
    }

    private fun CryptorageFile.toJsonMap(): Map<String, Any> = mapOf(
            "files" to files,
            "splitSize" to splitSize,
            "lastModified" to lastModified,
            "size" to size
    )

    private data class Index(
            val files: MutableMap<String, CryptorageFile>,
            val meta: MutableMap<String, String>
    )

    private data class CryptorageFile(val files: MutableList<String> = ArrayList(), val splitSize: Int = 0, var lastModified: Long = 0, var size: Long = 0) {
        constructor(file: JsonObject) :
                this(file.array<String>("files")!!.toMutableList(), file.int("splitSize")!!, file.long("lastModified")!!, file.long("size")!!)
    }

    private class ChainedEncryptor(
            private val source: FileSource,
            size: Int,
            keys: AesKeys,
            private val file: CryptorageFile,
            private val commitR: () -> Unit
    ) : ChainedEncryptorBase(source, size, keys) {
        override fun onRewrite() {
            file.files.forEach {
                source.delete(it)
            }
            file.files.clear()
        }

        override fun onStartWrite() {
            file.lastModified = System.currentTimeMillis()
        }

        override fun onFileEnded(name: String, size: Int) {
            file.files.add(name)
            file.size += size
        }

        override fun commit() {
            commitR()
        }
    }
}
