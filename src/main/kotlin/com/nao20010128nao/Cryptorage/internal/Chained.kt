package com.nao20010128nao.Cryptorage.internal

import com.google.common.base.Optional
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.AesKeys
import com.nao20010128nao.Cryptorage.FileSource
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream

internal abstract class ChainedDecryptorBase(private val source: FileSource, private val files: Iterator<String>, private val bytesToSkip: Int = 0, private val totalSize: Long? = null) : ByteSource() {
    constructor(source: FileSource, files: List<String>, bytesToSkip: Int = 0, totalSize: Long? = null) : this(source, files.iterator(), bytesToSkip, totalSize)

    override fun openStream(): InputStream = ConcatenatedInputStream(
            files.asSequence()
                    .map { source.open(it) to getKeyForFile(it) }
                    .map { AesDecryptorByteSource(it.first, it.second) }
                    .map { it.openStream() }
                    .iterator()
    ).also {
        ByteStreams.skipFully(it, bytesToSkip.toLong())
    }

    override fun sizeIfKnown(): Optional<Long> = Optional.fromNullable(totalSize)

    abstract fun getKeyForFile(file: String): AesKeys
}

internal class ChainedDecryptorStaticKey(source: FileSource, private val keys: AesKeys, files: Iterator<String>, bytesToSkip: Int = 0, totalSize: Long? = null) : ChainedDecryptorBase(source, files, bytesToSkip, totalSize) {
    constructor(source: FileSource, keys: AesKeys, files: List<String>, bytesToSkip: Int = 0, totalSize: Long? = null) : this(source, keys, files.iterator(), bytesToSkip, totalSize)

    override fun getKeyForFile(file: String): AesKeys = keys
}

internal abstract class ChainedEncryptorBase(private val source: FileSource, private val size: Int) : ByteSink() {
    private var current: OutputStream? = null

    abstract fun onRewrite()
    abstract fun onStartWrite()
    abstract fun onFileEnded(name: String, size: Int, keys: AesKeys)
    abstract fun commit()
    abstract fun getNextKey(): AesKeys
    open fun generateFileName(): String = generateRandomName()

    override fun openStream(): OutputStream {
        if (current != null) {
            current!!.close()
            onRewrite()
        }
        onStartWrite()
        commit()
        current = object : FilterOutputStream(null) {
            init {
                next(null)
            }

            private fun next(overflow: SizeLimitedOutputStream.Overflow?) {
                this.out = SizeLimitedOutputStream(size, { me, next ->
                    closeCurrent(me)
                    next(next)
                }, {
                    closeCurrent(it)
                }).regulated()
                if (overflow != null) {
                    write(overflow.buffer)
                }
            }

            private fun closeCurrent(me: SizeLimitedOutputStream) {
                val randName = generateFileName()
                val key = getNextKey()
                AesEncryptorByteSink(source.put(randName), key).openStream().also {
                    it.write(me.buffer, 0, me.size())
                    it.close()
                }
                onFileEnded(randName, me.size(), key)
                commit()
            }
        }
        return current!!
    }
}
