package com.nao20010128nao.Cryptorage.internal.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.readOnly
import com.nao20010128nao.Cryptorage.internal.relativeURL
import com.nao20010128nao.Cryptorage.internal.unsupported
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal class UrlFileSource(private val url: URL) : FileSource {
    override fun has(name: String): Boolean =
            url.relativeURL(name).openConnection()!!.also {
                (it as HttpURLConnection).requestMethod = "HEAD"
            }.let {
                try {
                    it.connect()
                    it.inputStream.close()/* HEAD mustn't have body so no bytes to read. */
                    try {
                        open(name).openStream().close()
                        true
                    } catch (e: Throwable) {
                        false
                    }
                } catch (e: Throwable) {
                    false
                }
            }

    /** Deletes file(s) */
    override fun delete(name: String): Unit = readOnly("FileSource")

    /** Lists up file names */
    override fun list(): List<String> = unsupported("FileSource", "list")

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource = UrlByteSource(url, name, offset)

    /** Opens file for writing */
    override fun put(name: String): ByteSink = readOnly("FileSource")

    /** Checks if Cryptorage is read-only */
    override val isReadOnly: Boolean = true

    override fun close() = Unit

    override fun commit() = Unit

    override fun lastModified(name: String): Long = -1

    override fun size(name: String): Long = -1

    private class UrlByteSource(private val url: URL, private val relative: String, private val offset: Int) : ByteSource() {
        override fun openStream(): InputStream = url.relativeURL(relative).openStream().also {
            ByteStreams.skipFully(it, offset.toLong())
        }
    }
}
