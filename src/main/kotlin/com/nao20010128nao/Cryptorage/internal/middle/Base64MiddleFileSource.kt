package com.nao20010128nao.Cryptorage.internal.middle

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.FileSource
import com.nao20010128nao.Cryptorage.internal.skip
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.commons.codec.binary.Base64OutputStream
import java.io.InputStream
import java.io.OutputStream

class Base64MiddleFileSource(override val source: FileSource) : MiddleFileSource {

    override fun list(): List<String> = source.list()

    override fun open(name: String, offset: Int): ByteSource = object : ByteSource() {
        val baseSource = source.open(name)
        override fun openStream(): InputStream = Base64InputStream(baseSource.openStream()).skip(offset)
    }

    override fun put(name: String): ByteSink = object : ByteSink() {
        override fun openStream(): OutputStream = Base64OutputStream(source.put(name).openStream())
    }

    override fun delete(name: String) {
        source.delete(name)
    }

    override val isReadOnly: Boolean
        get() = source.isReadOnly

    override fun lastModified(name: String): Long = source.lastModified(name)

    override fun size(name: String): Long = (source.size(name) * 3).shr(2)
}