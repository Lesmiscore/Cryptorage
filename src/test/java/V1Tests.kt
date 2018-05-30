import com.google.common.io.ByteStreams
import com.nao20010128nao.Cryptorage.Cryptorage
import com.nao20010128nao.Cryptorage.file.FileSource
import com.nao20010128nao.Cryptorage.internal.SizeLimitedOutputStream
import com.nao20010128nao.Cryptorage.newMemoryFileSource
import com.nao20010128nao.Cryptorage.withV1Encryption
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

class V1Tests {
    private fun FileSource.openForTest(): Cryptorage = withV1Encryption("test")

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

            println(i.toString() + ": " + String(test))
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
    fun testOverflow() {
        val payload = "It's a small world"
        val first10 = "It's a sma"
        val remain8 = "ll world"
        val stream = SizeLimitedOutputStream(10, { a, b ->
            assertTrue(Arrays.equals(a.buffer, first10.toByteArray()))
            assertTrue(Arrays.equals(b.buffer, remain8.toByteArray()))
        }, null)
        stream.write(payload.toByteArray())
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

        val `is` = cryptorage.open("test", 1000000).openBufferedStream()
        val read = ByteStreams.toByteArray(`is`)
        `is`.close()

        assertEquals(hashed, md.digest(read))
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

        val cryptorageReopen = memory.openForTest()
        assertTrue(cryptorageReopen.list().contains("file1"))
        assertTrue(cryptorageReopen.list().contains("file2"))
    }

    fun assertEquals(a: ByteArray, b: ByteArray) {
        require(Arrays.equals(a, b))
    }

    fun assertEquals(a: Long, b: Long) {
        require(a == b)
    }

    fun assertTrue(value: Boolean) {
        require(value)
    }
}
