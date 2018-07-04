package com.nao20010128nao.Cryptorage.internal.cryptorage

import com.beust.klaxon.*
import com.google.common.collect.HashMultimap
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.internal.*
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.util.*

internal class CryptorageImplV1(private val source: FileSource, private val keys: AesKeys) : Cryptorage, Compressable {
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
    private var hasClosed: Boolean = false

    /** Lists up file names */
    override fun list(): Array<String> = notClosed { index.files.keys.sorted().toTypedArray() }

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource {
        return notClosed {
            if (!has(name)) {
                fileNotFound(name)
            }
            val file = index.files[name]!!
            val indexOffset: Int = offset / file.splitSize
            val fileOffset: Int = offset % file.splitSize
            ChainedDecryptor(source, keys, file.files.drop(indexOffset), fileOffset, file.size - offset)
        }
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        return notClosed {
            if (has(name)) {
                delete(name)
            }
            val splitSize = index.meta[META_SPLIT_SIZE]?.toInt() ?: SPLIT_SIZE_DEFAULT
            val file = CryptorageFile(splitSize = splitSize)
            index.files[name] = file
            ChainedEncryptor(source, splitSize, keys, file) { }
        }
    }

    /** Moves file */
    override fun mv(from: String, to: String) {
        notClosed {
            if (!has(from)) {
                fileNotFound(from)
            }
            if (from == to) {
                return
            }
            if (has(to)) {
                delete(to)
            }
            index.files[to] = index.files[from]!!
        }
    }

    /** Deletes a file */
    override fun delete(name: String) {
        notClosed {
            if (!has(name)) {
                fileNotFound(name)
            }
            val removal = index.files[name]!!.files
            val usedByOther = encryptedFilesIDontOwn(name)
            removal.forEach {
                if (it in usedByOther) {
                    return@forEach
                }
                source.delete(it)
            }
            index.files.remove(name)
        }
    }

    /** Checks last modified date and time */
    override fun lastModified(name: String): Long = notClosed {
        when {
            has(name) -> index.files[name]!!.lastModified
            else -> fileNotFound(name)
        }
    }

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = notClosed { source.isReadOnly }

    override fun size(name: String): Long {
        return notClosed {
            if (!has(name)) {
                fileNotFound(name)
            }
            index.files[name]!!.size
        }
    }

    /** Removes unused files */
    override fun gc() {
        notClosed {
            val ls = source.list().asList()
            val unused = ls - index.files.flatMap { it.value.files } - MANIFEST
            unused.forEach {
                source.delete(it)
            }
        }
    }

    override fun meta(key: String): String? = notClosed { index.meta[key] }

    override fun meta(key: String, value: String) {
        notClosed { index.meta[key] = value }
    }

    override fun commit() {
        notClosed {
            val root = JsonObject()
            root["files"] = index.files.mapValues { it.value.toJsonMap() }
            root["meta"] = index.meta
            AesEncryptorByteSink(source.put(MANIFEST), keys)
                    .write(root.toJsonString(false).utf8Bytes())
            source.commit()
        }
    }

    override fun close() {
        commit()
        source.close()
        Arrays.fill(keys.first, 0)
        Arrays.fill(keys.second, 0)
        index.files.clear()
        index.meta.clear()
        hasClosed = true
    }

    override fun doCompress() {
        notClosed {
            val allFiles = index.files.flatMap { it.value.files }
            val hashes = HashMultimap.create<String, String>()
            // calculate hash for all files
            allFiles.forEach {
                hashes[source.open(it).openStream().digest()] = it
            }
            // create table to replace
            val replaceTable = hashMapOf<String, String>()
            hashes.keys().elementSet().forEach { value ->
                val values = hashes[value].toList()
                val leader = values.first()
                val files = values.drop(1)
                files.forEach {
                    replaceTable[it] = leader
                }
            }
            // update all file list with first one: we wont re-write files
            index.files.forEach { _, cryptorageFile ->
                cryptorageFile.files.replaceAll { replaceTable[it] ?: it }
            }
            // remove unreferenced files
            replaceTable.keys.forEach {
                source.delete(it)
            }
        }
    }


    private fun readIndex(): Index = if (source.has(MANIFEST)) {
        val data = (Parser().parse(AesDecryptorByteSource(source.open(MANIFEST), keys).asCharSource().openStream()) as JsonObject)
        val files = data.obj("files")!!.mapValues { CryptorageFile(it.value as JsonObject) }
        val meta = data.obj("meta")!!.mapValues { "${it.value}" }
        Index(files.toMutableMap(), meta.toMutableMap())
    } else {
        Index(hashMapOf(), hashMapOf())
    }

    private inline fun <T> notClosed(f: () -> T): T {
        if (hasClosed)
            closed("Cryptorage")
        return f()
    }

    private fun CryptorageFile.toJsonMap(): Map<String, Any> = mapOf(
            "files" to files,
            "splitSize" to splitSize,
            "lastModified" to lastModified,
            "size" to size
    )

    private fun encryptedFilesIDontOwn(nonEncrypted: String): SortedSet<String> =
            index.files.asSequence().filter { it.key != nonEncrypted }.flatMap { it.value.files.asSequence() }.toSortedSet()

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
