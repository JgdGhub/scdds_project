package uk.co.octatec.scdds.net.socket;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.octatec.scdds.net.serialize.DefaultSerializer;
import uk.co.octatec.scdds.net.serialize.Serializer;
import uk.co.octatec.scdds.net.serialize.SerializerUtils;
import uk.co.octatec.scdds.utilities.DataWithDate;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jeromy Drake on 05/05/16
 */
public class InitialUpdateReaderWriterTest {

    private final  Logger log = LoggerFactory.getLogger(InitialUpdateReaderWriterTest.class);

    public static final String DATA_FILE_1 = "test/InitialUpdate.1.dat";
    private static final int    DATA_FILE_1_ENTRY_COUNT = 3;

    public static final String DATA_FILE_2a = "test/InitialUpdate.2a.dat";
    public static final String DATA_FILE_2b = "test/InitialUpdate.2b.dat";
    private static final int    DATA_FILE_2a_ENTRY_COUNT = 3;
    private static final int    DATA_FILE_2b_ENTRY_COUNT = 2;

    public static final String DATA_FILE_3a = "test/InitialUpdate.3a.dat";
    public static final String DATA_FILE_3b = "test/InitialUpdate.3b.dat";
    private static final int    DATA_FILE_3a_ENTRY_COUNT = 3;
    private static final int    DATA_FILE_3b_ENTRY_COUNT = 0;

    static class MockBlocIo implements BlockIO {

        Header header = new Header();
        byte[] restOfBlock;

        Header populateHeader(byte[] buff) {
            header.dataLength = SerializerUtils.readIntFromBytes(buff);
            header.flag = buff[BlockIoImpl.HEADER_FLAG_POS];
            return header;
        }

        @Override
        public byte[] readBlock() throws IOException {
            return new byte[0];
        }

        @Override
        public void writeDataBlock(byte[] buff, int offset, int length) throws IOException {

        }

        @Override
        public void writeDataBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {

        }
        @Override
        public void writeDataBlock_HeaderSpaceIncluded(ByteBuffer buff) throws IOException {

        }

        @Override
        public void writeRemovalBlock_HeaderSpaceIncluded(byte[] buff, int offset, int length) throws IOException {

        }

        @Override
        public void writeString(String s) throws IOException {

        }

        @Override
        public String readString() throws IOException {
            return null;
        }

        @Override
        public void sendHeartbeat() throws IOException {

        }

        @Override
        public void sendStaleNotification() throws IOException {

        }

        @Override
        public void sendActiveNotification() throws IOException {

        }

        @Override
        public Header readBlockHeader() throws IOException {
            return header;
        }

        void populateRestOfBlock(byte[] buff, int offset, int length) {
            restOfBlock = new byte[length];
            System.arraycopy(buff, offset, restOfBlock, 0, length );
        }

        @Override
        public byte[] readRestOfBlock() throws IOException {
            return restOfBlock;
        }

        @Override
        public Header getCurrentHeader() {
            return header;
        }

        @Override
        public int getSizeOfHeader() {
            return BlockIoImpl.HEADER_LENGTH;
        }
    }



    @BeforeClass
    public static void setup() {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    }

    @Test
    public void smallUpdateWriterTest() throws Exception {

        log.info("## smallUpdateTest");

        DefaultSerializer<String, DataWithDate> serializer = new DefaultSerializer<String, DataWithDate>();

        InitialUpdateWriter  initialUpdateWriter = new InitialUpdateWriter();
        initialUpdateWriter.prepareInitialUpdateMessage();

        DataWithDate d1 = new DataWithDate(1);
        byte[] buff1 = serializer.serialize("d1", d1, 0);
        log.info("... buff1 [{}]", buff1);
        initialUpdateWriter.addInitialUpdateEntry(buff1);

        DataWithDate d2 = new DataWithDate(2);
        byte[] buff2 = serializer.serialize("d2", d2, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff2);

        DataWithDate d3 = new DataWithDate(3);
        byte[] buff3 = serializer.serialize("d3", d3, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff3);


        byte[] buff =   initialUpdateWriter.getInitialUpdateMessage();
        int length =  initialUpdateWriter.getInitialUpdateMessageLength();

        // now check the data in the buffer...

        int numItems = SerializerUtils.readIntFromBytes(buff, BlockIoImpl.HEADER_LENGTH); // was-5
        int lengthCheck = SerializerUtils.readIntFromBytes(buff, 0);

        log.info("header=[{}] numItems=[{}] lengthCheck=[{}]", buff[BlockIoImpl.HEADER_FLAG_POS], numItems, lengthCheck);
        Assert.assertEquals("num of items is correct", 3, numItems);
        Assert.assertEquals("correct length of data", length, lengthCheck+BlockIoImpl.HEADER_LENGTH);
        Assert.assertEquals("header", BlockIoImpl.INITIAL_UPDATE_FLAG, buff[BlockIoImpl.HEADER_FLAG_POS]);

        int posLenDataItem1 =  initialUpdateWriter.getStartingDataPos();
        int lengthOfDataItem1 = SerializerUtils.readIntFromBytes(buff, posLenDataItem1);
        log.info("length of data-item-1 [{}] expected [{}]", lengthOfDataItem1,  buff1.length);
        Assert.assertEquals("correct length of data-item-1", buff1.length, lengthOfDataItem1);
        int posDataItem1 = posLenDataItem1 + BlockIoImpl.HEADER_LENGTH;  // length=4-bytes, flag=1
        byte[] obj1 = new byte[lengthOfDataItem1];
        System.arraycopy(buff, posDataItem1, obj1, 0, lengthOfDataItem1);
        log.info("... obj1 [{}]", obj1);
        Serializer.Pair<String, DataWithDate> pair1 = serializer.deserialize(obj1, 0);
        Assert.assertEquals("deserialized key 1", "d1", pair1.key);
        Assert.assertEquals("deserialized value 1", d1, pair1.value);

        int posLenDataItem2 = posDataItem1 + lengthOfDataItem1 ;
        int lengthOfDataItem2 = SerializerUtils.readIntFromBytes(buff, posLenDataItem2);
        log.info("length of data-item-2 [{}] expected [{}]", lengthOfDataItem2,  buff2.length);
        int posDataItem2 = posLenDataItem2 + BlockIoImpl.HEADER_LENGTH;; // was-5  // length=4-bytes, flag=1
        byte[] obj2 = new byte[lengthOfDataItem2];
        System.arraycopy(buff, posDataItem2, obj2, 0, lengthOfDataItem2);
        log.info("... obj2 [{}]", obj2);
        Serializer.Pair<String, DataWithDate> pair2 = serializer.deserialize(obj2, 0);
        Assert.assertEquals("deserialized key 2", "d2", pair2.key);
        Assert.assertEquals("deserialized value 2", d2, pair2.value);


        int posLenDataItem3 = posDataItem2 + lengthOfDataItem2 ;
        int lengthOfDataItem3 = SerializerUtils.readIntFromBytes(buff, posLenDataItem3);
        log.info("length of data-item-3 [{}] expected [{}]", lengthOfDataItem3,  buff3.length);
        int posDataItem3 = posLenDataItem3 + BlockIoImpl.HEADER_LENGTH;  // length=4-bytes, flag=1  //  was-5
        byte[] obj3 = new byte[lengthOfDataItem3];
        System.arraycopy(buff, posDataItem3, obj3, 0, lengthOfDataItem3);
        log.info("... obj3 [{}]", obj3);
        Serializer.Pair<String, DataWithDate> pair3 = serializer.deserialize(obj3, 0);
        Assert.assertEquals("deserialized key 3", "d3", pair3.key);
        Assert.assertEquals("deserialized value 3", d3, pair3.value);

        log.debug("write-bytes(1) len={} [{}]", length, buff);

        File f = new File(DATA_FILE_1);
        if( !f.exists()) {
            log.info("create [{}] for subsequent tests", DATA_FILE_1);
            FileOutputStream fos = new FileOutputStream(DATA_FILE_1);
            fos.write(buff, 0, length);
            fos.close();
        }
    }

    @Test
    public void smallUpdateWriterWithCompletionTest() throws Exception {

        log.info("## smallUpdateWriterWithCompletionTest");

        DefaultSerializer<String, DataWithDate> serializer = new DefaultSerializer<String, DataWithDate>();

        InitialUpdateWriter  initialUpdateWriter = new InitialUpdateWriter();
        initialUpdateWriter.prepareInitialUpdateMessage();

        DataWithDate d1 = new DataWithDate(1);
        byte[] buff1 = serializer.serialize("d1", d1, 0);
        log.info("... buff1 [{}]", buff1);
        initialUpdateWriter.addInitialUpdateEntry(buff1);

        DataWithDate d2 = new DataWithDate(2);
        byte[] buff2 = serializer.serialize("d2", d2, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff2);

        DataWithDate d3 = new DataWithDate(3);
        byte[] buff3 = serializer.serialize("d3", d3, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff3);


        byte[] buff =   initialUpdateWriter.getInitialUpdateMessage();
        int length =  initialUpdateWriter.getInitialUpdateMessageLength();

        // now basic check items are ok...

        int numItems = SerializerUtils.readIntFromBytes(buff, BlockIoImpl.HEADER_LENGTH); // was-5
        int lengthCheck = SerializerUtils.readIntFromBytes(buff, 0);

        log.info("(1)hdr=[{}] numItems=[{}] lengthCheck=[{}]", buff[BlockIoImpl.HEADER_FLAG_POS], numItems, lengthCheck);

        Assert.assertEquals("(1)header", BlockIoImpl.INITIAL_UPDATE_FLAG, buff[BlockIoImpl.HEADER_FLAG_POS]);
        Assert.assertEquals("(1)num of items is correct", 3, numItems);
        Assert.assertEquals("(1)correct length of data", length, lengthCheck+BlockIoImpl.HEADER_LENGTH);

        File f = new File(DATA_FILE_2a);
        if( !f.exists()) {
            log.info("create [{}] for subsequent tests", DATA_FILE_2a);
            FileOutputStream fos = new FileOutputStream(DATA_FILE_2a);
            fos.write(buff, 0, length);
            fos.close();
        }

        // now send 2 more items

        initialUpdateWriter.prepareInitialUpdateMessage();

        d1 = new DataWithDate(10);
        buff1 = serializer.serialize("dd1", d1, 0);
        log.info("... buff1 [{}]", buff1);
        initialUpdateWriter.addInitialUpdateEntry(buff1);

        d2 = new DataWithDate(20);
        buff2 = serializer.serialize("dd2", d2, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff2);


        initialUpdateWriter.completeInitialUpdateMessage();

        buff =   initialUpdateWriter.getInitialUpdateMessage();
        length =  initialUpdateWriter.getInitialUpdateMessageLength();

        // now basic check items are ok...

        numItems = SerializerUtils.readIntFromBytes(buff, BlockIoImpl.HEADER_LENGTH); //was-5
        lengthCheck = SerializerUtils.readIntFromBytes(buff, 0);

        log.info("(2) hdr=[{}] numItems=[{}] lengthCheck=[{}]", buff[BlockIoImpl.HEADER_FLAG_POS], numItems, lengthCheck);

        Assert.assertEquals("(2)header", BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG, buff[BlockIoImpl.HEADER_FLAG_POS]);
        Assert.assertEquals("(2)num of items is correct", 2, numItems);
        Assert.assertEquals("(2)correct length of data", length, lengthCheck+BlockIoImpl.HEADER_LENGTH);


        int posLenDataItem1 = initialUpdateWriter.getStartingDataPos();
        int lengthOfDataItem1 = SerializerUtils.readIntFromBytes(buff, posLenDataItem1);
        log.info("(2)length of data-item-1 [{}] expected [{}]", lengthOfDataItem1,  buff1.length);
        Assert.assertEquals("(2)correct length of data-item-1", buff1.length, lengthOfDataItem1);
        int posDataItem1 = posLenDataItem1 + BlockIoImpl.HEADER_LENGTH;  // was-5 // length=4-bytes, flag=1
        byte[] obj1 = new byte[lengthOfDataItem1];
        System.arraycopy(buff, posDataItem1, obj1, 0, lengthOfDataItem1);
        log.info("... obj1 [{}]", obj1);
        Serializer.Pair<String, DataWithDate> pair1 = serializer.deserialize(obj1, 0);
        Assert.assertEquals("(2)deserialized key 1", "dd1", pair1.key);
        Assert.assertEquals("(2)deserialized value 1", d1, pair1.value);

        int posLenDataItem2 = posDataItem1 + lengthOfDataItem1 ;
        int lengthOfDataItem2 = SerializerUtils.readIntFromBytes(buff, posLenDataItem2);
        log.info("(2)length of data-item-2 [{}] expected [{}]", lengthOfDataItem2,  buff2.length);
        int posDataItem2 = posLenDataItem2 + BlockIoImpl.HEADER_LENGTH; // was-5 // length=4-bytes, flag=1
        byte[] obj2 = new byte[lengthOfDataItem2];
        System.arraycopy(buff, posDataItem2, obj2, 0, lengthOfDataItem2);
        log.info("... obj2 [{}]", obj2);
        Serializer.Pair<String, DataWithDate> pair2 = serializer.deserialize(obj2, 0);
        Assert.assertEquals("(2)deserialized key 2", "dd2", pair2.key);
        Assert.assertEquals("(2)deserialized value 2", d2, pair2.value);

        f = new File(DATA_FILE_2b);
        if( !f.exists()) {
            log.info("create [{}] for subsequent tests", DATA_FILE_2b);
            FileOutputStream fos = new FileOutputStream(DATA_FILE_2b);
            fos.write(buff, 0, length);
            fos.close();
        }
    }


    @Test
    public void smallUpdateWriterWithEmptyCompletionTest() throws Exception {

        log.info("## smallUpdateWriterWithEmptyCompletionTest");

        DefaultSerializer<String, DataWithDate> serializer = new DefaultSerializer<String, DataWithDate>();

        InitialUpdateWriter  initialUpdateWriter = new InitialUpdateWriter();
        initialUpdateWriter.prepareInitialUpdateMessage();

        DataWithDate d1 = new DataWithDate(1);
        byte[] buff1 = serializer.serialize("d1", d1, 0);
        log.info("... buff1 [{}]", buff1);
        initialUpdateWriter.addInitialUpdateEntry(buff1);

        DataWithDate d2 = new DataWithDate(2);
        byte[] buff2 = serializer.serialize("d2", d2, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff2);

        DataWithDate d3 = new DataWithDate(3);
        byte[] buff3 = serializer.serialize("d3", d3, 0);
        initialUpdateWriter.addInitialUpdateEntry(buff3);


        byte[] buff =   initialUpdateWriter.getInitialUpdateMessage();
        int length =  initialUpdateWriter.getInitialUpdateMessageLength();

        // now basic check items are ok...

        int numItems = SerializerUtils.readIntFromBytes(buff, BlockIoImpl.HEADER_LENGTH);  // was-5
        int lengthCheck = SerializerUtils.readIntFromBytes(buff, 0);

        log.info("(1)hdr=[{}] numItems=[{}] lengthCheck=[{}]", buff[BlockIoImpl.HEADER_FLAG_POS], numItems, lengthCheck);

        Assert.assertEquals("(3)header", BlockIoImpl.INITIAL_UPDATE_FLAG, buff[BlockIoImpl.HEADER_FLAG_POS]);
        Assert.assertEquals("(3)num of items is correct", 3, numItems);
        Assert.assertEquals("(3)correct length of data", length, lengthCheck+BlockIoImpl.HEADER_LENGTH);

        File f = new File(DATA_FILE_3a);
        if( !f.exists()) {
            log.info("create [{}] for subsequent tests", DATA_FILE_3a);
            FileOutputStream fos = new FileOutputStream(DATA_FILE_3a);
            fos.write(buff, 0, length);
            fos.close();
        }

        // now there are no more items to send...

        initialUpdateWriter.prepareInitialUpdateMessage();

        initialUpdateWriter.completeInitialUpdateMessage();

        buff =   initialUpdateWriter.getInitialUpdateMessage();
        length =  initialUpdateWriter.getInitialUpdateMessageLength();

        // now basic check items are ok...

        numItems = SerializerUtils.readIntFromBytes(buff, BlockIoImpl.HEADER_LENGTH); // was-5
        lengthCheck = SerializerUtils.readIntFromBytes(buff, 0);

        log.info("(2) hdr=[{}] numItems=[{}] lengthCheck=[{}] total-leng-of-buff=[{}]", buff[BlockIoImpl.HEADER_FLAG_POS], numItems, lengthCheck, buff.length);

        Assert.assertEquals("(3)header", BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG, buff[BlockIoImpl.HEADER_FLAG_POS]);
        Assert.assertEquals("(3)num of items is correct", 0, numItems);
        Assert.assertEquals("(3)correct length of data", length, lengthCheck+BlockIoImpl.HEADER_LENGTH);

        f = new File(DATA_FILE_3b);
        if( !f.exists()) {
            log.info("create [{}] for subsequent tests", DATA_FILE_3b);
            FileOutputStream fos = new FileOutputStream(DATA_FILE_3b);
            fos.write(buff, 0, length);
            fos.close();
        }
    }


    @Test
    public void smallUpdateReaderTest() throws Exception {

        log.info("## smallUpdateReaderTest");

        byte[] buff = loadDataFromFile(DATA_FILE_1);
        if( buff == null ) {
            log.warn("smallUpdateReaderTest is ignored untill dependency has run");
            return;
        }

        log.info("read initial update data size=[{}] from [{}]", buff.length, DATA_FILE_1) ;
        log.debug("read-bytes(1) len={} [{}]", buff.length, buff);

        MockBlocIo bIO = new MockBlocIo();
        BlockIO.Header header = bIO.populateHeader(buff);
        Assert.assertEquals("Mock IO header populated correctly", BlockIoImpl.INITIAL_UPDATE_FLAG, header.flag);
        bIO.populateRestOfBlock(buff, BlockIoImpl.HEADER_LENGTH, buff.length-BlockIoImpl.HEADER_LENGTH);

        Serializer<String, DataWithDate> serializer = new DefaultSerializer<>();
        InitialUpdateReader  initialUpdateReader = new InitialUpdateReader(bIO, serializer);
        Map<String, DataWithDate> map = new  HashMap<String, DataWithDate>();
        initialUpdateReader.readInitialUpdate(map);

        log.info("map populated: key-set {}", map==null ? null : map.keySet());

        Assert.assertEquals("correct number of entries in map ", DATA_FILE_1_ENTRY_COUNT, map.size());
        DataWithDate d1 = map.get("d1");
        Assert.assertNotNull("d1 exists in map", d1);
        DataWithDate d2 = map.get("d2");
        Assert.assertNotNull("d2 exists in map", d2);
        DataWithDate d3 = map.get("d3");
        Assert.assertNotNull("d3 exists in map", d3);
    }

    @Test
    public void smallUpdateReaderWithCompletionTest() throws Exception {

        log.info("## smallUpdateReaderWithCompletionTest");

        byte[] buff = loadDataFromFile(DATA_FILE_2a);
        if( buff == null ) {
            log.warn("smallUpdateReaderWithCompletionTest is ignored until dependency has run");
            return;
        }

        log.info("read initial update-a data size=[{}] from [{}]", buff.length, DATA_FILE_2a) ;

        MockBlocIo bIO = new MockBlocIo();
        BlockIO.Header header = bIO.populateHeader(buff);
        Assert.assertEquals("Mock IO header populated correctly", BlockIoImpl.INITIAL_UPDATE_FLAG, header.flag);
        bIO.populateRestOfBlock(buff, BlockIoImpl.HEADER_LENGTH, buff.length-BlockIoImpl.HEADER_LENGTH);

        Serializer<String, DataWithDate> serializer = new DefaultSerializer<>();
        InitialUpdateReader  initialUpdateReader = new InitialUpdateReader(bIO, serializer);
        Map<String, DataWithDate> map = new  HashMap<String, DataWithDate>();
        initialUpdateReader.readInitialUpdate(map);

        log.info("map populated: key-set(a) {}", map==null ? null : map.keySet());

        Assert.assertEquals("correct number of entries in map ", DATA_FILE_2a_ENTRY_COUNT, map.size());
        DataWithDate d1 = map.get("d1");
        Assert.assertNotNull("d1 exists in map", d1);
        DataWithDate d2 = map.get("d2");
        Assert.assertNotNull("d2 exists in map", d2);
        DataWithDate d3 = map.get("d3");
        Assert.assertNotNull("d3 exists in map", d3);

        // 2nd block of data

        buff = loadDataFromFile(DATA_FILE_2b);
        if( buff == null ) {
            log.warn("smallUpdateReaderWithCompletionTest is ignored until dependency has run");
            return;
        }

        log.info("read initial update-b data size=[{}] from [{}]", buff.length, DATA_FILE_2b) ;

        header = bIO.populateHeader(buff);
        Assert.assertEquals("Mock IO header populated correctly", BlockIoImpl.INITIAL_UPDATE_COMPLETED_FLAG, header.flag);
        bIO.populateRestOfBlock(buff, BlockIoImpl.HEADER_LENGTH, buff.length-BlockIoImpl.HEADER_LENGTH);
        initialUpdateReader.readInitialUpdate(map);

        log.info("map populated: key-set(b) {}", map==null ? null : map.keySet());

        Assert.assertEquals("correct number of entries in map ", DATA_FILE_2a_ENTRY_COUNT+DATA_FILE_2b_ENTRY_COUNT, map.size());
        DataWithDate dd1 = map.get("dd1");
        Assert.assertNotNull("dd1 exists in map", dd1);
        DataWithDate dd2 = map.get("dd2");
        Assert.assertNotNull("dd2 exists in map", dd2);
    }


    public static byte[]  loadDataFromFile(String filename) {
        Logger log = LoggerFactory.getLogger("Data-Loader");
        try {
            File f = new File(filename);
            if (!f.exists()) {
                log.error("No SimpleData File [{}]", filename);
            }
            byte[] buff = new byte[(int) f.length()];
            FileInputStream fis = new FileInputStream(filename);
            fis.read(buff);
            fis.close();
            return buff;
        }
        catch( Exception e ) {
            log.error("can't open file that contains test data [{}]", filename);
            log.warn("this means the 'Reader' tests in InitialUpdateReaderWriterTest have not yet run");
            log.warn("those tests create data files used by some other tests");
            return null;
        }
    }

    @Test
    public void  noReAllocTest() {
        log.info("##noReAllocTest");
        doRreallocTest(10);
    }

    @Test
    public void  reAllocTestMedium() {
        log.info("##reAllocTestMedium");
        doRreallocTest(0);
    }

    @Test
    public void  reAllocTestLarge() {
        log.info("##reAllocTestLarge");
        doRreallocTest(-1);
    }

    public void  doRreallocTest(int useObjSize) {

        log.info("doReallocTest useObjSize={}", useObjSize);

        InitialUpdateWriter writer = new InitialUpdateWriter();
        int bufSize = writer.getBuff().length;

        int objSize = useObjSize;
        if( useObjSize == 0 ) {
            objSize  = bufSize-10;   // small enough for 1 message to fit in the buffer, but not 2
        }
        else if( useObjSize == -1 ) {
            objSize  = bufSize+128;   // too big for the initial buffer
        }
        log.info("objSize [{}]", objSize );

        writer.prepareInitialUpdateMessage();

        byte[] obj1 = new byte[objSize];
        for(int i=0; i<obj1.length; i++) {
            obj1[i] = 'A';
        }
        obj1[0] = '@';
        obj1[obj1.length-1] ='#';
        writer.addInitialUpdateEntry(obj1);

        byte[] obj2 = new byte[objSize]; // force a re-alloc
        for(int i=0; i<obj2.length; i++) {
            obj2[i] = 'B';
        }
        obj2[0] = '$';
        obj2[obj1.length-1] ='%';
        writer.addInitialUpdateEntry(obj2);

        int n = writer.getInitialUpdateMessageLength();
        byte[] buff = writer.getInitialUpdateMessage();
        log.info("buff length [{}] size={} required={}", buff.length, n, (2*(obj1.length+BlockIoImpl.HEADER_LENGTH)));
        Assert.assertTrue("buffer big enough", buff.length >= (2*obj1.length)+BlockIoImpl.HEADER_LENGTH);
        Assert.assertEquals("buffer exact size", (2*(obj1.length+BlockIoImpl.HEADER_LENGTH))+writer.getStartingDataPos(), n);

        int startObj1 =  writer.getStartingDataPos()+ BlockIoImpl.HEADER_LENGTH;
        int endObj1 =  startObj1+obj1.length-1;
        Assert.assertEquals("start-flag correct obj-1", '@', buff[startObj1]);
        Assert.assertEquals("end-flag correct obj-1", '#', buff[endObj1]);


        int startObj2 =  endObj1 + BlockIoImpl.HEADER_LENGTH+1;
        int endObj2 =  startObj2+obj2.length-1;
        Assert.assertEquals("start-flag correct obj-2", '$', buff[startObj2]);
        Assert.assertEquals("end-flag correct obj-2", '%', buff[endObj2]);

        int len1 = endObj1-startObj1+1;
        int len2 = endObj2-startObj2+1;
        log.info("obj1 [{}] = [{}] ...  [{}] = [{}] len={}?{}", startObj1, (char)buff[startObj1], endObj1, (char)buff[endObj1], len1 , obj1.length  ) ;
        log.info("obj2 [{}] = [{}] ...  [{}] = [{}] len={}?{}", startObj2, (char)buff[startObj2], endObj2, (char)buff[endObj2], len2 , obj2.length);

        Assert.assertEquals("obj-1 correct length", obj1.length, len1);
        Assert.assertEquals("obj-2 correct length", obj2.length, len2);

        for(int i=startObj1+1; i<startObj1+obj1.length-1; i++) {
            Assert.assertEquals("obj1-value "+i, 'A', buff[i]);
        }

        for(int i=startObj2+1; i<startObj2+obj2.length-1; i++) {
            Assert.assertEquals("obj2-value "+i, 'B', buff[i]);
        }
    }
}
