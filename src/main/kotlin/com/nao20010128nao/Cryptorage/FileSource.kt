package com.nao20010128nao.Cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import java.io.Closeable

/** Source of files (e.g. file system, web server...) */
interface FileSource : Closeable {
    /** Lists up file names */
    fun list(): List<String>

    /** Opens file for reading with offset and less overhead */
    fun open(name: String, offset: Int): ByteSource

    /** Opens file for reading */
    fun open(name: String): ByteSource = open(name, 0)

    /** Opens file for writing */
    fun put(name: String): ByteSink

    /** Deletes a file */
    fun delete(name: String)

    /** Checks file exist */
    fun has(name: String): Boolean = list().contains(name)

    /** Flushes all content */
    fun commit()

    /** Closes FileSource and release objects */
    override fun close()

    /** Checks Cryptorage is read-only */
    val isReadOnly: Boolean

    /** Gets last modified date and time */
    fun lastModified(name: String): Long

    /** Gets the size */
    fun size(name: String): Long
}
