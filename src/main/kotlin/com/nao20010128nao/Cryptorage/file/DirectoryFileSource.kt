package com.nao20010128nao.Cryptorage.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class DirectoryFileSource(private val dir: File) : FileSource {
    /** Lists up file names */
    override fun list(): Array<String> = dir.list() ?: arrayOf()

    /** Deletes file(s) */
    override fun delete(name: String) {
        File(dir, name).delete()
    }

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource = FileByteSource(File(dir, name), offset)

    /** Opens file for writing */
    override fun put(name: String): ByteSink = FileByteSink(File(dir, name))

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = !dir.canWrite()

    private class FileByteSource(private val file: File, private val offset: Int) : ByteSource() {
        override fun openStream(): InputStream = file.inputStream().also {
            ByteStreams.skipFully(it, offset.toLong())
        }
    }

    private class FileByteSink(private val file: File) : ByteSink() {
        init {
            file.parentFile.mkdirs()
        }

        override fun openStream(): OutputStream = file.outputStream()
    }
}