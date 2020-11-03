package com.nao20010128nao.Cryptorage

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

interface UrlFetcher {
    /** Perform HEAD request to that URL. Return true if it exists. */
    fun doHead(url: URL): Boolean

    /** Perform GET request to that URL. */
    fun doGet(url: URL): InputStream
}

object JavaNetUrlFetcher : UrlFetcher {
    override fun doHead(url: URL): Boolean {
        val conn = url.openConnection()!!.also {
            (it as HttpURLConnection).requestMethod = "HEAD"
        }
        return try {
            conn.connect()
            conn.inputStream.close()/* HEAD mustn't have body so no bytes to read. */
            try {
                url.openStream().close()
                true
            } catch (e: Throwable) {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    override fun doGet(url: URL): InputStream = url.openStream()
}
