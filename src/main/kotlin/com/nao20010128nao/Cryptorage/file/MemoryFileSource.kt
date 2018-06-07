package com.nao20010128nao.Cryptorage.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.crop
import java.io.ByteArrayOutputStream
import java.io.OutputStream

internal class MemoryFileSource(private val map: MutableMap<String, ByteArray> = HashMap()) : FileSource {

    /** Lists up file names */
    override fun list(): Array<String> = map.keys.toTypedArray()

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

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean = false
}