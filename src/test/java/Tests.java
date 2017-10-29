import com.google.common.io.*;
import com.nao20010128nao.Cryptorage.*;
import com.nao20010128nao.Cryptorage.cryptorage.*;
import com.nao20010128nao.Cryptorage.file.*;
import com.nao20010128nao.Cryptorage.internal.*;
import junit.framework.*;

import java.io.*;
import java.security.*;
import java.util.*;

public class Tests extends TestCase{
    public Tests(){
        if(!System.getProperty("os.name").toLowerCase().contains("linux")){
            throw new IllegalStateException("Run on Linux");
        }
    }

    public void testSimpleWriting()throws Throwable{
        Cryptorage cryptorage=new CryptorageImplV1(new DirectoryFileSource(new File("/tmp/"+ UUID.randomUUID())),"test");
        SecureRandom sr=new SecureRandom();
        byte[] test=new byte[1024*1024];
        sr.nextBytes(test);
        OutputStream dest=cryptorage.put("test").openBufferedStream();
        dest.write(test);
        dest.close();
        MessageDigest md=MessageDigest.getInstance("sha-256");
        byte[] hashed=md.digest(test);

        InputStream is=cryptorage.open("test",0).openBufferedStream();
        byte[] read= ByteStreams.toByteArray(is);
        is.close();

        assertTrue(Arrays.equals(hashed, md.digest(read)));
    }
    public void testSimpleWriting2()throws Throwable{
        String where=UUID.randomUUID().toString();
        System.out.println(where);
        Cryptorage cryptorage=new CryptorageImplV1(new DirectoryFileSource(new File("/tmp/"+ where)),"test");
        String payload="It's a small world";

        byte[] test=payload.getBytes();
        OutputStream dest=cryptorage.put("test").openBufferedStream();
        for(int i=0;i<10000;i++)
            dest.write(test);
        dest.close();

        InputStream is=cryptorage.open("test",0).openBufferedStream();
        for(int i=0;i<10000;i++){
            Throwable error=null;
            try {
                Arrays.fill(test,(byte)0);
                ByteStreams.readFully(is,test);
            }catch (Throwable e){
                error=e;
            }
            System.out.println(i+": "+new String(test));
            if(error!=null){
                throw error;
            }
            assertTrue(Arrays.equals(test, payload.getBytes()));
        }
        is.close();
    }
    public void testWriteSize()throws Throwable{
        String where=UUID.randomUUID().toString();
        System.out.println(where);
        Cryptorage cryptorage=new CryptorageImplV1(new DirectoryFileSource(new File("/tmp/"+ where)),"test");
        String payload="It's a small world";

        byte[] test=payload.getBytes();
        OutputStream dest=cryptorage.put("test").openBufferedStream();
        for(int i=0;i<10000;i++)
            dest.write(test);
        dest.close();

        assertEquals(payload.length()*10000,cryptorage.size("test"));
    }
    public void testOverflow()throws Throwable{
        String payload="It's a small world";
        String first10="It's a sma";
        String remain8="ll world";
        SizeLimitedOutputStream stream=new SizeLimitedOutputStream(10,(a,b)->{
            assertTrue(Arrays.equals(a.getBuffer(), first10.getBytes()));
            assertTrue(Arrays.equals(b.getBuffer(), remain8.getBytes()));
            return null;
        },a-> null);
        stream.write(payload.getBytes());
    }

    public void testOverWrite()throws Throwable{
        Cryptorage cryptorage=new CryptorageImplV1(new DirectoryFileSource(new File("/tmp/"+ UUID.randomUUID())),"test");
        SecureRandom sr=new SecureRandom();
        byte[] test=new byte[1024*1024];
        sr.nextBytes(test);
        OutputStream dest=cryptorage.put("test").openBufferedStream();
        dest.write(test);
        dest.close();
        sr.nextBytes(test);
        dest=cryptorage.put("test").openBufferedStream();
        dest.write(test);
        dest.close();
        MessageDigest md=MessageDigest.getInstance("sha-256");
        byte[] hashed=md.digest(test);

        InputStream is=cryptorage.open("test",0).openBufferedStream();
        byte[] read= ByteStreams.toByteArray(is);
        is.close();

        assertTrue(Arrays.equals(hashed, md.digest(read)));
    }
    public void testSkip()throws Throwable{
        Cryptorage cryptorage=new CryptorageImplV1(new DirectoryFileSource(new File("/tmp/"+ UUID.randomUUID())),"test");
        SecureRandom sr=new SecureRandom();
        byte[] test=new byte[1024*1024];
        sr.nextBytes(test);
        OutputStream dest=cryptorage.put("test").openBufferedStream();
        dest.write(test);
        dest.close();
        MessageDigest md=MessageDigest.getInstance("sha-256");
        md.update(test,1000000,test.length-1000000);
        byte[] hashed=md.digest();

        InputStream is=cryptorage.open("test",1000000).openBufferedStream();
        byte[] read= ByteStreams.toByteArray(is);
        is.close();

        assertTrue(Arrays.equals(hashed, md.digest(read)));
    }

}
