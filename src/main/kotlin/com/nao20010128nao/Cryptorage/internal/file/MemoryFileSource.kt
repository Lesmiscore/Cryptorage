package com.nao20010128nao.Cryptorage.internal.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.crop
import java.io.ByteArrayOutputStream
import java.io.OutputStream

internal class MemoryFileSource(private val map: MutableMap<String, ByteArray> = HashMap()) : FileSource {

    /** Lists up file names */
    override fun list(): List<String> = map.keys.toList()

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource = map[name]!!.let {
        ByteSource.wrap(it.crop(offset, it.size - offset))
    }

    /** Opens file for writing */
    override fun put(name: String): ByteSink = object : ByteSink() {
        override fun openStream(): OutputStream = object : ByteArrayOutputStream() {
            override fun close() {
                map[name] = toByteArray()
            }
        }
    }

    /** Deletes a file */
    override fun delete(name: String) {
        map.remove(name)
    }

    override fun close() {
    }

    override fun commit() {
    }

    override fun lastModified(name: String): Long = -1

    override fun size(name: String): Long = map[name]?.size?.toLong() ?: -1

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean = false
}