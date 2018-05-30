package com.nao20010128nao.Cryptorage.internal

import java.io.IOException
import java.io.InputStream

/**
 * SequenceInputStream that takes Iterator as input
 * */
internal class ConcatenatedInputStream(private val e: Iterator<InputStream>) : InputStream() {
    private var current: InputStream? = null

    init {
        try {
            this.nextStream()
        } catch (var3: IOException) {
            throw Error("panic")
        }
    }

    private fun nextStream() {
        if (this.current != null) {
            this.current!!.close()
        }

        if (this.e.hasNext()) {
            this.current = this.e.next()
            if (this.current == null) {
                throw NullPointerException()
            }
        } else {
            this.current = null
        }
    }

    override fun available(): Int {
        return if (this.current == null) 0 else this.current!!.available()
    }

    override fun read(): Int {
        while (this.current != null) {
            val var1 = this.current!!.read()
            if (var1 != -1) {
                return var1
            }
            this.nextStream()
        }
        return -1
    }

    override fun read(buf: ByteArray, off: Int, len: Int): Int {
        if (this.current == null) {
            return -1
        } else if (off >= 0 && len >= 0 && len <= buf.size - off) {
            if (len == 0) {
                return 0
            } else {
                do {
                    val var4 = this.current!!.read(buf, off, len)
                    if (var4 > 0) {
                        return var4
                    }

                    this.nextStream()
                } while (this.current != null)

                return -1
            }
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    override fun close() {
        do {
            this.nextStream()
        } while (this.current != null)
    }
}
