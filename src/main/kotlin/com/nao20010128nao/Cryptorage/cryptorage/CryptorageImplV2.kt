package com.nao20010128nao.Cryptorage.cryptorage

import com.beust.klaxon.*
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.Cryptorage.Companion.META_SPLIT_SIZE
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.*
import org.bitcoinj.core.ECKey
import java.io.FileNotFoundException
import java.security.SecureRandom

internal class CryptorageImplV2(private val source: FileSource, private val password: String) : Cryptorage {
    companion object {
        const val MANIFEST: String = "manifest"
        const val SPLIT_SIZE_DEFAULT: Int = 100 * 1024 /* 100kb */ - 16 /* Final block size */

        internal fun deriveKeys(password: String, nonce: Long = MANIFEST.hashCode().toLong()): AesKeys {
            val pwSha = password.utf8Bytes().digest()
            val ec = ECKey.fromPrivate(pwSha)
            val ecPublic = ec.pubKeyPoint
            val ecMultiplied = ecPublic.multiply(nonce.toBigInteger())
            val compressed = ecMultiplied.getEncoded(true)
            val compressedDigest = compressed.digest().digest()
            return compressedDigest.leading(16) to compressedDigest.trailing(16)
        }

        internal fun deriveManifestFilename(password: String, index: Long): String {
            val pwSha = password.utf8Bytes().digest()
            val ec = ECKey.fromPrivate(pwSha)
            val ecPublic = ec.pubKeyPoint
            val ecMultiplied = ecPublic.multiply(pwSha.toBigInteger()).multiply(index.toBigInteger())
            val compressed = ecMultiplied.getEncoded(true)
            val compressedDigest = compressed.digest().digest()
            val names = compressedDigest.leading(16).toUUIDHashed() + compressedDigest.trailing(16).toUUIDHashed()
            return names.replace("-", "")
        }
    }

    private val keysManifest = deriveKeys(password)
    private val random = SecureRandom()

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
        return ChainedDecryptor(source, deriveKeys(password, file.nonce), file.files.drop(indexOffset), fileOffset)
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink {
        if (has(name)) {
            delete(name)
        }
        val splitSize = splitSize()
        val nonce = random.nextLong()
        val file = CryptorageFile(splitSize = splitSize, nonce = nonce)
        index.files[name] = file
        commit()
        return ChainedEncryptor(source, splitSize, deriveKeys(password, nonce), file, { commit() })
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

    private fun splitSize(): Int = (index.meta[META_SPLIT_SIZE] ?: "$SPLIT_SIZE_DEFAULT").toInt()

    private fun manifestFilenameIterator() = generateSequence(0) { it + 1 }.map { deriveManifestFilename(password, it.toLong()) }

    private fun cleanManifest() {
        manifestFilenameIterator().takeWhile { source.has(it) }.forEach { source.delete(it) }
    }

    private fun readIndex(): Index {
        if (!source.has(deriveManifestFilename(password, 0)))
            return Index(hashMapOf(), hashMapOf())
        val nameIter = manifestFilenameIterator().takeWhile { source.has(it) }.iterator()
        val reader = ChainedDecryptor(source, keysManifest, nameIter)
        val data = (Parser().parse(reader.asCharSource().openStream()) as JsonObject)
        val files = data.obj("files")!!.mapValues { CryptorageFile(it.value as JsonObject) }
        val meta = data.obj("meta")!!.mapValues { "${it.value}" }
        return Index(files.toMutableMap(), meta.toMutableMap())
    }

    private fun commit() {
        cleanManifest()
        val root = JsonObject()
        root["files"] = index.files.mapValues { it.value.toJsonMap() }
        root["meta"] = index.meta
        val iter = manifestFilenameIterator().iterator()
        val dummyFile = CryptorageFile(nonce = MANIFEST.hashCode().toLong())
        ChainedEncryptor(source, splitSize(), keysManifest, dummyFile, {}, { iter.next() })
                .write(root.toJsonString(false).utf8Bytes())
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
            var files: MutableList<String> = ArrayList(),
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
            keys: AesKeys,
            private val file: CryptorageFile,
            private val commitR: () -> Unit,
            private val filenameResolver: (() -> String?)? = null
    ) : ChainedEncryptorBase(source, size, keys) {
        override fun onRewrite() {
            file.files.forEach {
                source.delete(it)
            }
            file.files = ArrayList()
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

        override fun generateFileName(): String = filenameResolver?.invoke() ?: super.generateFileName()
    }
}
