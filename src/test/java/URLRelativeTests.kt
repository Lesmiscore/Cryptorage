import com.nao20010128nao.Cryptorage.internal.relativeURL
import junit.framework.TestCase
import org.junit.Test
import java.net.URL

class URLRelativeTests {
    @Test
    fun testSimpleRelative() {
        TestCase.assertEquals(URL("http://example.com/hello"), URL("http://example.com/").relativeURL("hello"))
    }

    @Test
    fun testRelativeWithQuery() {
        TestCase.assertEquals(URL("http://example.com/hello?foo=bar"), URL("http://example.com/?foo=bar").relativeURL("hello"))
    }

    @Test
    fun testSimpleRelativeHttps() {
        TestCase.assertEquals(URL("https://example.com/hello"), URL("https://example.com/").relativeURL("hello"))
    }

    @Test
    fun testRelativeWithQueryHttps() {
        TestCase.assertEquals(URL("https://example.com/hello?foo=bar"), URL("https://example.com/?foo=bar").relativeURL("hello"))
    }
}
