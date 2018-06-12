package com.nao20010128nao.Cryptorage.internal.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.internal.readOnly
import com.nao20010128nao.Cryptorage.internal.unsupported
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal class UrlFileSource(private val url: URL) : FileSource {
    override fun has(name: String): Boolean =
            URL(url.protocol, url.host, url.port, "${url.path}/$name${if (url.query.isNullOrBlank()) "" else "?${url.query}"}")
                    .openConnection()!!.also {
                        (it as HttpURLConnection).requestMethod = "HEAD"
                    }.let {
                        try {
                            it.connect()
                            it.inputStream.close()/* HEAD mustn't have body so no bytes to read. */
                            true
                        } catch (e: Throwable) {
                            false
                        }
                    }

    /** Deletes file(s) */
    override fun delete(name: String): Unit = readOnly("FileSource")

    /** Lists up file names */
    override fun list(): Array<String> = unsupported("FileSource","list")

    /** Opens file for reading */
    override fun open(name: String, offset: Int): ByteSource = UrlByteSource(url, name, offset)

    /** Opens file for writing */
    override fun put(name: String): ByteSink = readOnly("FileSource")

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean = true

    private class UrlByteSource(private val url: URL, private val relative: String, private val offset: Int) : ByteSource() {
        override fun openStream(): InputStream = URL(url.protocol, url.host, url.port, "${url.path}/$relative?${url.query}").openStream().also {
            ByteStreams.skipFully(it, offset.toLong())
        }
    }
}