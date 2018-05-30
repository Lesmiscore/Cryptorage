import com.nao20010128nao.Cryptorage.internal.SizeLimitedOutputStream
import org.junit.*;


class SizeLimitedOutputStreamTest{
    @Test
    fun testOverflow() {
        val payload = "It's a small world"
        val first10 = "It's a sma"
        val remain8 = "ll world"
        val stream = SizeLimitedOutputStream(10, { a, b ->
            assertEquals(a.buffer, first10.toByteArray())
            assertEquals(b.buffer, remain8.toByteArray())
        }, null)
        stream.write(payload.toByteArray())
    }
}
