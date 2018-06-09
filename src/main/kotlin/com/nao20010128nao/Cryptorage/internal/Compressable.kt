package com.nao20010128nao.Cryptorage.internal

/**
 * Marks Cryptorage "compressable".<br>
 * "Compressable" means that there's a any way to shrink used space without data loss.
 */
interface Compressable {
    fun doCompress()
}