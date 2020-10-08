import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.*
import com.nao20010128nao.Cryptorage.internal.trailing
import com.nao20010128nao.Cryptorage.internal.utf8Bytes
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class V3Tests {
    private fun FileSource.openForTest(): Cryptorage = withV3Encryption("test")
    private fun FileSource.openForTestV1(): Cryptorage = withV1Encryption("test")

    @Test
    fun testSimpleWriting() {
        val cryptorage = newMemoryFileSource().openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testSimpleWriting2() {
        val cryptorage = newMemoryFileSource().openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        val dest = cryptorage.put("test").openBufferedStream()
        for (i in 0..99999)
            dest.write(test)
        dest.close()

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        for (i in 0..99999) {
            var error: Throwable? = null
            try {
                Arrays.fill(test, 0.toByte())
                ByteStreams.readFully(`is`, test)
            } catch (e: Throwable) {
                error = e
            }

            println("$i: ${test.toString(utf8)}")
            if (error != null) {
                throw error
            }
            assertEquals(test, payload.toByteArray())
        }
        `is`.close()
    }

    @Test
    fun testWriteSize() {
        val cryptorage = newMemoryFileSource().openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        val dest = cryptorage.put("test").openBufferedStream()
        for (i in 0..9999)
            dest.write(test)
        dest.close()

        assertEquals((payload.length * 10000).toLong(), cryptorage.size("test"))
    }

    @Test
    fun testOverWrite() {
        val cryptorage = newMemoryFileSource().openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        var dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        sr.nextBytes(test)
        dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)

        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testSkip() {
        val cryptorage = newMemoryFileSource().openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        md.update(test, 1000000, test.size - 1000000)
        val hashed = md.digest()

        val read = cryptorage.open("test", 1000000).openBufferedStream().readBytes()

        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testSkip2() {
        val cryptorage = newMemoryFileSource().openForTest()
        val test = "It's a small world".utf8Bytes()
        val dest = cryptorage.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val last10 = test.trailing(10)

        val skipped = cryptorage.open("test", test.size - last10.size).openBufferedStream().readBytes()
        assertEquals(skipped, "mall world".utf8Bytes())
        assertEquals(last10, "mall world".utf8Bytes())
    }

    @Test
    fun testReopen() {
        val memory = newMemoryFileSource()
        val cryptorage = memory.openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        var dest = cryptorage.put("file1").openBufferedStream()
        dest.write(test)
        dest.close()
        dest = cryptorage.put("file2").openBufferedStream()
        dest.write(test)
        dest.write(test)
        dest.close()
        assertTrue(cryptorage.list().contains("file1"))
        assertTrue(cryptorage.list().contains("file2"))
        cryptorage.close()

        val cryptorageReopen = memory.openForTest()
        assertTrue(cryptorageReopen.list().contains("file1"))
        assertTrue(cryptorageReopen.list().contains("file2"))
    }

    @Test
    fun testGc() {
        val memory = newMemoryFileSource()
        val cryptorage = memory.openForTest()
        val payload = "It's a small world"

        val test = payload.toByteArray()
        // write encrypted files
        run {
            val file = cryptorage.put("file1").openBufferedStream()
            file.write(test)
            file.close()
        }
        run {
            val file = cryptorage.put("file2").openBufferedStream()
            file.write(test)
            file.close()
        }
        run {
            val file = cryptorage.put("file3").openBufferedStream()
            file.write(test)
            file.close()
        }
        cryptorage.commit()
        // write extra files
        run {
            val file = memory.put("file1").openBufferedStream()
            file.write(test)
            file.close()
        }
        run {
            val file = memory.put("file2").openBufferedStream()
            file.write(test)
            file.close()
        }
        run {
            val file = memory.put("file3").openBufferedStream()
            file.write(test)
            file.close()
        }
        // gc now
        cryptorage.gc()
        // check for removed files
        assertTrue(!memory.list().contains("file1"))
        assertTrue(!memory.list().contains("file2"))
        assertTrue(!memory.list().contains("file3"))
        // check for alive files
        run {
            val file1 = cryptorage.open("file1").openStream().readBytes()
            assertEquals(file1, test)
        }
        run {
            val file1 = cryptorage.open("file2").openStream().readBytes()
            assertEquals(file1, test)
        }
        run {
            val file1 = cryptorage.open("file3").openStream().readBytes()
            assertEquals(file1, test)
        }
    }

    @Test
    fun testV1Compat() {
        val fs = newMemoryFileSource()
        val cryptorageV1 = fs.openForTestV1()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorageV1.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)
        cryptorageV1.commit()

        val cryptorage = fs.openForTest()
        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(test, read)
        assertEquals(hashed, md.digest(read))
    }

    @Test
    fun testV1Incompatibility() {
        val fs = newMemoryFileSource()
        val cryptorageV1 = fs.openForTest()
        val sr = SecureRandom()
        val test = ByteArray(1024 * 1024)
        sr.nextBytes(test)
        val dest = cryptorageV1.put("test").openBufferedStream()
        dest.write(test)
        dest.close()
        val md = MessageDigest.getInstance("sha-256")
        val hashed = md.digest(test)
        cryptorageV1.commit()

        val cryptorage = fs.openForTestV1()
        val `is` = cryptorage.open("test", 0).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertNotEquals(test, read)
        assertNotEquals(hashed, md.digest(read))
    }
}
