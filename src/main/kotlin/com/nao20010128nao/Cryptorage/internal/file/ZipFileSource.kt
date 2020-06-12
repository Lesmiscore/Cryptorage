@file:Suppress("UnstableApiUsage")

package com.nao20010128nao.Cryptorage.internal.file

import com.google.common.base.Optional
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.unsupported
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

internal class ZipFileSource(file: File) : FileSource {
    private val zf = ZipFile(file)
    override val isReadOnly: Boolean = true

    override fun close() {
        zf.close()
    }

    override fun commit() {
    }

    override fun list(): List<String> = zf.entries().asSequence().map { it.name }.toList()

    override fun open(name: String, offset: Int): ByteSource = object : ByteSource() {
        val entry = zf.getEntry(name)!!
        override fun openStream(): InputStream = zf.getInputStream(entry)!!
        override fun sizeIfKnown(): Optional<Long> = if (entry.size < 0L) {
            Optional.absent()
        } else {
            Optional.of(entry.size)
        }
    }

    override fun lastModified(name: String): Long = zf.getEntry(name)?.lastModifiedTime?.toMillis() ?: -1

    override fun size(name: String): Long = zf.getEntry(name)?.size ?: -1

    override fun put(name: String): ByteSink = unsupported("FileSource", "put")

    override fun delete(name: String) = unsupported("FileSource", "delete")
}