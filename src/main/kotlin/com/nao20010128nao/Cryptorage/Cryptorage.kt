package com.nao20010128nao.Cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.file.FileSource

/** Encrypted storage */
interface Cryptorage : FileSource/* allow itself to be a FileSource */ {
    /** Lists up file names */
    override fun list(): Array<String>
    /** Opens file for reading */
    override fun open(name: String,offset:Int): ByteSource
    /** Opens file for writing */
    override fun put(name: String): ByteSink
    /** Moves file */
    fun mv(from: String,to: String)
    /** Deletes file(s) */
    override fun delete(name: String)
    /** Checks file exist */
    fun has(name: String): Boolean = list().contains(name)
    /** Checks last modified date and time */
    fun lastModified(name: String): Long
    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
    /** Removes unused files */
    fun gc()
}
