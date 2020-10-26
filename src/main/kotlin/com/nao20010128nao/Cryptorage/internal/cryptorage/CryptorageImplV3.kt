package com.nao20010128nao.Cryptorage.internal.cryptorage

import com.beust.klaxon.JsonObject
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_LAST_NONCE
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_NONCE_MODE
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.NONCE_MODE_RANDOM
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.NONCE_MODE_SEQUENTIAL
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayList

internal class CryptorageImplV3(private val source: FileSource, private val keys: AesKeys, private val preferManifestNonce: Boolean) : Cryptorage {
    companion object {
        const val MANIFEST: String = "manifest"
        const val MANIFEST_NONCE = "manifest_nonce"
        const val SPLIT_SIZE_DEFAULT: Int = 100 * 1024 /* 100kb */ - 16 /* Final block size */

        private fun populateKeys(password: String): AesKeys {
            val utf8Bytes1 = password.utf8Bytes()
            val utf8Bytes2 = "$password$password".utf8Bytes()
            return Pair(utf8Bytes1.digest().digest().leading(16), utf8Bytes2.digest().digest().trailing(16))
        }
    }

    constructor(source: FileSource, password: String, preferManifestNonce: Boolean) : this(source, populateKeys(password), preferManifestNonce)

    private val index: Index = readIndex()
    private var hasClosed: Boolean = false

    /** Lists up file names */
    override fun list(): List<String> = notClosed { index.files.keys.toList() }

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource {
        return notClosed {
            if (!has(name)) {
                fileNotFound(name)
            }
            val file = index.files[name]!!
            val indexOffset: Int = offset / file.splitSize
            val fileOffset: Int = offset % file.splitSize
            ChainedDecryptor(source, keys, file.nonce.drop(indexOffset), file.files.drop(indexOffset), fileOffset, file.size - offset)
        }
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        return notClosed {
            if (has(name)) {
                delete(name)
            }
            val splitSize = index.meta[META_SPLIT_SIZE]?.toInt() ?: SPLIT_SIZE_DEFAULT
            var lastNonce = index.meta[META_LAST_NONCE]?.toBigIntegerOrNull() ?: BigInteger.ONE
            val nonceMode = index.meta[META_NONCE_MODE] ?: NONCE_MODE_SEQUENTIAL
            val file = CryptorageFile(splitSize = splitSize)
            index.files[name] = file
            ChainedEncryptor(source, splitSize, keys, file, { }, {
                val nextNonce = when (nonceMode) {
                    NONCE_MODE_SEQUENTIAL -> {
                        val ln = lastNonce
                        lastNonce += BigInteger.ONE
                        index.meta[META_LAST_NONCE] = "$lastNonce"
                        ln
                    }
                    NONCE_MODE_RANDOM -> {
                        ByteArray(keys.second.size).also {
                            sr.nextBytes(it)
                        }.toBigInteger()
                    }
                    else -> BigInteger.ZERO
                }
                keys.copy(second = keys.second xor nextNonce)
            })
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
            index.files.remove(from)
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
            val ls = source.list()
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
            val manifestKeys = if (source.has(MANIFEST_NONCE) || preferManifestNonce) {
                val iv = ByteArray(keys.second.size).also {
                    sr.nextBytes(it)
                }
                source.put(MANIFEST_NONCE).write(iv)
                keys.copy(second = iv)
            } else {
                keys
            }

            val root = JsonObject()
            root["files"] = index.files.mapValues { it.value.toJsonMap() }
            root["meta"] = index.meta
            AesEncryptorByteSink(source.put(MANIFEST), manifestKeys)
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

    private fun readIndex(): Index = if (source.has(MANIFEST)) {
        val manifestKeys = if (source.has(MANIFEST_NONCE)) {
            val iv = source.open(MANIFEST_NONCE).read()
            keys.copy(second = iv)
        } else {
            keys
        }
        val data = parseJson(AesDecryptorByteSource(source.open(MANIFEST), manifestKeys).asCharSource().openStream())
        val files = data.obj("files")!!.mapValues { CryptorageFile(it.value as JsonObject) }
        val meta = data.obj("meta")!!.mapValues { "${it.value}" }
        Index(files.toMutableMap(), meta.toMutableMap())
    } else if (!source.isReadOnly) {
        Index(hashMapOf(), hashMapOf())
    } else {
        error("Cannot create Cryptorage")
    }

    private inline fun <T> notClosed(f: () -> T): T {
        if (hasClosed)
            closed("Cryptorage")
        return f()
    }

    private fun CryptorageFile.toJsonMap(): Map<String, Any> = mapOf(
            "files" to files,
            "nonce" to nonce.map { "$it" },
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

    private data class CryptorageFile(
            val files: MutableList<String> = ArrayList(),
            val nonce: MutableList<BigInteger> = ArrayList(),
            val splitSize: Int = 0,
            var lastModified: Long = 0,
            var size: Long = 0
    ) {
        constructor(file: JsonObject) :
                this(
                        file.array<String>("files")!!.toMutableList(),
                        // allow reading V1 cryptorage (V1 is also acceptable as V3, with all nonce are zero)
                        (file.array<String>("nonce")?.map { it.toBigInteger() }
                                ?: List<BigInteger>(file.array<String>("files")!!.size) { BigInteger.ZERO })
                                .toMutableList(),
                        file.int("splitSize")!!,
                        file.long("lastModified")!!,
                        file.long("size")!!
                )
    }

    private class ChainedEncryptor(
            private val source: FileSource,
            size: Int,
            private val baseKeys: AesKeys,
            private val file: CryptorageFile,
            private val commitR: () -> Unit,
            private val nextKey: () -> AesKeys,
    ) : ChainedEncryptorBase(source, size) {
        override fun onRewrite() {
            file.files.forEach {
                source.delete(it)
            }
            file.files.clear()
        }

        override fun onStartWrite() {
            file.lastModified = System.currentTimeMillis()
        }

        override fun onFileEnded(name: String, size: Int, keys: AesKeys) {
            file.files.add(name)
            val xor = (keys.second xor baseKeys.second)
            file.nonce.add(xor.toBigInteger())
            file.size += size
        }

        override fun commit() {
            commitR()
        }

        override fun getNextKey(): AesKeys = nextKey()
    }

    private class ChainedDecryptor(
            source: FileSource,
            val keys: AesKeys,
            val ivNonce: List<BigInteger>,
            val files: List<String>,
            bytesToSkip: Int,
            totalSize: Long?,
    ) : ChainedDecryptorBase(source, files, bytesToSkip, totalSize) {
        override fun getKeyForFile(file: String): AesKeys {
            val nonce = ivNonce[files.indexOf(file)]
            return keys.copy(second = keys.second xor nonce)
        }
    }
}
