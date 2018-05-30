import com.nao20010128nao.Cryptorage.internal.crop
import org.junit.Test

class ByteCropTest {
    @Test
    fun testCrop() {
        val data = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9)
        val expected = byteArrayOf(3, 4, 5)
        assertEquals(data.crop(3, 3), expected)
    }
}