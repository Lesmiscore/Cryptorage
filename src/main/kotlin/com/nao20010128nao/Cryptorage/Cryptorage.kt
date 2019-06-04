@file:Suppress("unused")

package com.nao20010128nao.Cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import java.io.Closeable

/** Encrypted storage */
interface Cryptorage : FileSource, Closeable {
    companion object {
        /** Split size for larger files */
        const val META_SPLIT_SIZE: String = "split_size"
    }

    /** Lists up file names */
    override fun list(): Array<String>

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource

    /** Opens file for writing */
    override fun put(name: String): ByteSink

    /** Moves file */
    fun mv(from: String, to: String)

    /** Deletes a file */
    override fun delete(name: String)

    /** Checks file exist */
    override fun has(name: String): Boolean = list().contains(name)

    /** Gets last modified date and time */
    override fun lastModified(name: String): Long

    /** Gets the size */
    override fun size(name: String): Long

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean

    /**
     *  Gets metadata(option) to Cryptorage
     *  @see META_SPLIT_SIZE
     *  */
    fun meta(key: String): String?

    /**
     *  Gets metadata(option) to Cryptorage
     *  @see META_SPLIT_SIZE
     *  */
    fun meta(key: String, value: String)

    /**
     *  Sets metadata(option) to Cryptorage
     *  @see META_SPLIT_SIZE
     *  */
    fun meta(pair: Pair<String, String>) {
        meta(pair.first, pair.second)
    }

    /** Removes unused files */
    fun gc()

    /** Flushes all content to FileSource */
    override fun commit()

    /** Closes Cryptorage and release objects */
    override fun close()
}
