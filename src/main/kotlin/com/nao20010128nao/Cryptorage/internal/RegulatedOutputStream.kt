package com.nao20010128nao.Cryptorage.internal

import java.io.FilterOutputStream
import java.io.OutputStream
import kotlin.math.min

class RegulatedOutputStream(
        private val base: OutputStream,
        private val splitChunk: Int = 8192,
        private val splitThreshold: Int = 16384
) : FilterOutputStream(base) {
    override fun write(b: ByteArray?) {
        write(b, 0, b!!.size)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        if (len < splitThreshold) {
            base.write(b, off, len)
        } else {
            for (i in (0..len / splitChunk)) {
                val newOffset = off + splitChunk * i
                val newLen = min(splitChunk, len - splitChunk * i)
                if (newLen == 0) {
                    break
                }
                base.write(b, newOffset, newLen)
            }
        }
    }
}