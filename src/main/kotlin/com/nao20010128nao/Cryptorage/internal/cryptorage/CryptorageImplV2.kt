package com.nao20010128nao.Cryptorage.internal.cryptorage

import com.beust.klaxon.JsonObject
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import org.bitcoinj.core.ECKey
import java.io.FileNotFoundException
import java.security.SecureRandom
import java.util.*

internal class CryptorageImplV2(private val source: FileSource, password: String) : Cryptorage {
    companion object {
        const val NONCE_MANIFEST = 130625071L
        const val SPLIT_SIZE_DEFAULT: Int = 100 * 1024 /* 100kb */ - 16 /* Final block size */

        internal fun deriveKeys(pwSha: ByteArray, nonce: Long = NONCE_MANIFEST): AesKeys {
            val ec = ECKey.fromPrivate(pwSha)
            val ecPublic = ec.pubKeyPoint
            val ecMultiplied = ecPublic.multiply(nonce.toBigInteger())
            val compressed = ecMultiplied.getEncoded(true)
            val compressedDigest = compressed.digest().digest()
            return compressedDigest.leading(16) to compressedDigest.trailing(16)
        }

        internal fun deriveManifestFilename(pwSha: ByteArray, index: Long): String {
            val indexSha = "$index".utf8Bytes().digest()
            val ec = ECKey.fromPrivate(pwSha)
            val ecPublic = ec.pubKeyPoint
            val ecMultiplied = ecPublic.multiply(pwSha.toBigInteger()).multiply(indexSha.toBigInteger())
            val compressed = ecMultiplied.getEncoded(true)
            val compressedDigest = compressed.digest().digest()
            val names = compressedDigest.leading(16).toUUIDHashed() + compressedDigest.trailing(16).toUUIDHashed()
            return names.replace("-", "")
        }
    }

    private val pwSha = password.utf8Bytes().digest()
    private val keysManifest = deriveKeys(pwSha)
    private val random = SecureRandom()

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
            ChainedDecryptorStaticKey(source, deriveKeys(pwSha, file.nonce), file.files.drop(indexOffset), fileOffset, file.size - offset)
        }
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        return notClosed {
            if (has(name)) {
                delete(name)
            }
            val splitSize = splitSize()
            val nonce = random.nextLong()
            val file = CryptorageFile(splitSize = splitSize, nonce = nonce)
            index.files[name] = file
            ChainedEncryptor(source, splitSize, deriveKeys(pwSha, nonce), file, { })
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
            removal.forEach {
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
                throw FileNotFoundException(name)
            }
            index.files[name]!!.size
        }
    }

    /** Removes unused files */
    override fun gc() {
        notClosed {
            val ls = source.list()
            val unused = ls - index.files.flatMap { it.value.files } - manifestFilenameIterator().takeWhile { source.has(it) }
            unused.forEach {
                source.delete(it)
            }
        }
    }

    override fun meta(key: String): String? = notClosed { index.meta[key] }

    override fun meta(key: String, value: String) {
        notClosed {
            index.meta[key] = value
        }
    }

    override fun commit() {
        notClosed {
            cleanManifest()
            val root = JsonObject()
            root["files"] = index.files.mapValues { it.value.toJsonMap() }
            root["meta"] = index.meta
            val iter = manifestFilenameIterator().iterator()
            val dummyFile = CryptorageFile(nonce = NONCE_MANIFEST)
            ChainedEncryptor(source, splitSize(), keysManifest, dummyFile, {}, { iter.next() })
                    .write(root.toJsonString(false).utf8Bytes())
            source.commit()
        }
    }

    override fun close() {
        commit()
        source.close()
        Arrays.fill(pwSha, 0)
        Arrays.fill(keysManifest.first, 0)
        Arrays.fill(keysManifest.second, 0)
        index.files.clear()
        index.meta.clear()
        hasClosed = true
    }


    private fun splitSize(): Int = index.meta[META_SPLIT_SIZE]?.toInt() ?: SPLIT_SIZE_DEFAULT

    private fun manifestFilenameIterator() = generateSequence(0) { it + 1 }.map { deriveManifestFilename(pwSha, it.toLong()) }

    private fun cleanManifest() {
        manifestFilenameIterator().takeWhile { source.has(it) }.forEach { source.delete(it) }
    }

    private fun readIndex(): Index {
        if (!source.has(deriveManifestFilename(pwSha, 0)))
            return Index(hashMapOf(), hashMapOf())
        if (source.isReadOnly)
            error("Cannot create Cryptorage")
        val nameIter = manifestFilenameIterator().takeWhile { source.has(it) }.iterator()
        val reader = ChainedDecryptorStaticKey(source, keysManifest, nameIter)
        val data = parseJson(reader.asCharSource().openStream())
        val files = data.obj("files")!!.mapValues { CryptorageFile(it.value as JsonObject) }
        val meta = data.obj("meta")!!.mapValues { "${it.value}" }
        return Index(files.toMutableMap(), meta.toMutableMap())
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
            "size" to size,
            "nonce" to nonce
    )

    private data class Index(
            val files: MutableMap<String, CryptorageFile>,
            val meta: MutableMap<String, String>
    )

    private data class CryptorageFile(
            val files: MutableList<String> = ArrayList(),
            val splitSize: Int = 0,
            var lastModified: Long = 0,
            var size: Long = 0,
            val nonce: Long = 0
    ) {
        constructor(file: JsonObject) :
                this(
                        file.array<String>("files")!!.toMutableList(),
                        file.int("splitSize")!!,
                        file.long("lastModified")!!,
                        file.long("size")!!,
                        file.long("nonce")!!
                )
    }

    private class ChainedEncryptor(
            private val source: FileSource,
            size: Int,
            private val keys: AesKeys,
            private val file: CryptorageFile,
            private val commitR: () -> Unit,
            private val filenameResolver: (() -> String?)? = null
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
            file.size += size
        }

        override fun commit() {
            commitR()
        }

        override fun generateFileName(): String = filenameResolver?.invoke() ?: super.generateFileName()
        override fun getNextKey(): AesKeys = keys
    }
}

