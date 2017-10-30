package com.nao20010128nao.Cryptorage.file

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.google.common.io.ByteStreams
import java.io.InputStream
import java.io.OutputStream
import java.lang.UnsupportedOperationException
import java.net.*

internal class UrlFileSource(private val url: URL): FileSource {
    override fun has(name: String): Boolean = 
        URL(url.protocol,url.host,url.port,"${url.path}/$name?${url.query}").openStream().also {
            (it as HttpURLConnection).requestMethod = "HEAD"
        }.let {
            try{
                openStream()
                true
            }catch(e: Throwable){
                false
            }
        }

    /** Deletes file(s) */
    override fun delete(name: String): Unit = throw UnsupportedOperationException("Not writable")

    /** Lists up file names */
    override fun list(): Array<String> = throw UnsupportedOperationException("Not writable")

    /** Opens file for reading */
    override fun open(name: String,offset: Int): ByteSource = UrlByteSource(url,name,offset)

    /** Opens file for writing */
    override fun put(name: String): ByteSink = UrlByteSink()

    /** Checks Cryptorage is read-only */
    override val isReadOnly: Boolean
        get() = true

    private class UrlByteSource(private val url: URL,private val relative:String,private val offset: Int): ByteSource(){
        override fun openStream(): InputStream = URL(url.protocol,url.host,url.port,"${url.path}/$relative?${url.query}").openStream().also {
            ByteStreams.skipFully(it,offset.toLong())
        }
    }

    private class UrlByteSink: ByteSink() {
        override fun openStream(): OutputStream = throw UnsupportedOperationException("Not writable")
    }
}