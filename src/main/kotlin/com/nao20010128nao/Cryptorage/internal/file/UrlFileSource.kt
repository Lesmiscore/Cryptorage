package com.nao20010128nao.Cryptorage.internal.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.UrlFetcher
import com.nao20010128nao.Cryptorage.internal.readOnly
import com.nao20010128nao.Cryptorage.internal.relativeURL
import com.nao20010128nao.Cryptorage.internal.unsupported
import java.io.InputStream
import java.net.URL

internal class UrlFileSource(private val url: URL, private val fetcher: UrlFetcher) : FileSource {
    override fun has(name: String): Boolean = fetcher.doHead(url.relativeURL(name))

    override fun delete(name: String): Unit = readOnly("FileSource")

    override fun list(): List<String> = unsupported("FileSource", "list")

    override fun open(name: String, offset: Int): ByteSource = UrlByteSource(name, offset)

    override fun put(name: String): ByteSink = readOnly("FileSource")

    override val isReadOnly: Boolean = true

    override fun close() = Unit

    override fun commit() = Unit

    override fun lastModified(name: String): Long = -1

    override fun size(name: String): Long = -1

    private inner class UrlByteSource(private val relative: String, private val offset: Int) : ByteSource() {
        override fun openStream(): InputStream = fetcher.doGet(url.relativeURL(relative)).also {
            ByteStreams.skipFully(it, offset.toLong())
        }
    }
}
