package com.nao20010128nao.Cryptorage.internal

import java.io.IOException
import java.io.InputStream

/**
 * SequenceInputStream that takes Iterator as input
 * */
class ConcatenatedInputStream(private val e: Iterator<InputStream>) : InputStream() {
    private var current: InputStream? = null

    init {
        try {
            nextStream()
        } catch (e: IOException) {
            throw Error("panic")
        }
    }

    private fun nextStream() {
        current?.close()

        current = if (e.hasNext()) {
            e.next()
        } else {
            null
        }
    }

    override fun available(): Int = if (current != null) current!!.available() else 0

    override fun read(): Int {
        while (current != null) {
            val r = current!!.read()
            if (r != -1) {
                return r
            }
            nextStream()
        }
        return -1
    }

    override fun read(buf: ByteArray, off: Int, len: Int): Int {
        if (current == null) {
            return -1
        } else if (off >= 0 && len >= 0 && len <= buf.size - off) {
            if (len != 0) {
                do {
                    val r = current!!.read(buf, off, len)
                    if (r > 0) {
                        return r
                    }
                    nextStream()
                } while (current != null)
                return -1
            } else {
                return 0
            }
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    override fun close() {
        do {
            nextStream()
        } while (current != null)
    }
}
