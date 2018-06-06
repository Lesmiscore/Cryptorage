import com.nao20010128nao.Cryptorage.internal.crop
import com.nao20010128nao.Cryptorage.internal.leading
import com.nao20010128nao.Cryptorage.internal.trailing
import org.junit.Test

class ByteCropTest {
    @Test
    fun testCrop() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9)
        val expected = byteArrayOf(3, 4, 5)
        assertEquals(data.crop(3, 3), expected)
    }

    @Test
    fun testLeading() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9)
        val expected = byteArrayOf(0, 1, 2, 3)
        assertEquals(data.leading(4), expected)
    }

    @Test
    fun testTrailing() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9)
        val expected = byteArrayOf(6, 7, 8, 9)
        assertEquals(data.trailing(4), expected)
    }
}