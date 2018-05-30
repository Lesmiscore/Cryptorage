package com.nao20010128nao.Cryptorage.internal

import java.io.OutputStream

internal class SizeLimitedOutputStream(
        limit: Int,
        private val next: ((SizeLimitedOutputStream, Overflow) -> Unit)?,
        private val close: ((SizeLimitedOutputStream) -> Unit)?
) : OutputStream() {
    val buffer: ByteArray
    private var count: Int = 0

    init {
        if (limit < 0) {
            throw IllegalArgumentException("Invalid capacity/limit")
        }
        buffer = ByteArray(limit)
        count = 0
    }

    override fun write(b: Int) {
        if (count >= buffer.size) {
            next?.invoke(this, Overflow(b.toByte()))
            return
        }
        buffer[count++] = b.toByte()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (off < 0 || off > b.size || len < 0 || off + len > b.size
                || off + len < 0) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return
        }

        if (count + len > buffer.size) {
            /* I was so confused so use this way */
            val oldCount = count
            System.arraycopy(b, off, buffer, count, buffer.size - count)
            count = buffer.size
            next?.invoke(this, Overflow(b, off + (buffer.size - oldCount), len - (buffer.size - oldCount)))
            return
        }

        System.arraycopy(b, off, buffer, count, len)
        count += len
    }

    fun size(): Int = count

    override fun close() {
        close?.invoke(this)
    }

    class Overflow(val buffer: ByteArray) {
        constructor(a: Byte) : this(byteArrayOf(a))
        constructor(b: ByteArray, off: Int, len: Int) : this(b.crop(off, len))
    }
}