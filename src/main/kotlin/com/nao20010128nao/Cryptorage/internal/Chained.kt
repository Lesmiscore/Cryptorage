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

internal class ChainedDecryptor(private val source: FileSource, private val keys: AesKeys, private val files: Iterator<String>, private val bytesToSkip: Int = 0, private val totalSize: Long? = null) : ByteSource() {
    constructor(source: FileSource, keys: AesKeys, files: List<String>, bytesToSkip: Int = 0, totalSize: Long? = null) : this(source, keys, files.iterator(), bytesToSkip, totalSize)

    override fun openStream(): InputStream = ConcatenatedInputStream(
            files.asSequence()
                    .map { source.open(it) }
                    .map { AesDecryptorByteSource(it, keys) }
                    .map { it.openStream() }
                    .iterator()
    ).also {
        ByteStreams.skipFully(it, bytesToSkip.toLong())
    }

    override fun sizeIfKnown(): Optional<Long> = Optional.fromNullable(totalSize)
}

internal abstract class ChainedEncryptorBase(private val source: FileSource, private val size: Int, private val keys: AesKeys) : ByteSink() {
    private var current: OutputStream? = null

    abstract fun onRewrite()
    abstract fun onStartWrite()
    abstract fun onFileEnded(name: String, size: Int)
    abstract fun commit()
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
                AesEncryptorByteSink(source.put(randName), keys).openStream().also {
                    it.write(me.buffer, 0, me.size())
                    it.close()
                }
                onFileEnded(randName, me.size())
                commit()
            }
        }
        return current!!
    }
}
