package com.nao20010128nao.Cryptorage.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource

/** Source of files (e.g. file system, web server...) */
interface FileSource {
    /** Lists up file names */
    fun list(): Array<String>
    /** Opens file for reading */
    fun open(name: String,offset:Int =0): ByteSource
    /** Opens file for writing */
    fun put(name: String): ByteSink
    /** Deletes a file */
    fun delete(name: String)
    /** Checks file exist */
    fun has(name: String): Boolean = list().contains(name)
    /** Checks Cryptorage is read-only */
    val isReadOnly: Boolean
}
