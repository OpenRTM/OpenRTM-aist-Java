package jp.go.aist.rtm.RTC.buffer;

import jp.go.aist.rtm.RTC.buffer.RingBuffer;
import jp.go.aist.rtm.RTC.util.DataRef;
import jp.go.aist.rtm.RTC.util.Properties;
import junit.framework.TestCase;

import java.util.*;

/**
 * <p>RingBufferのためのテストケースです。</p>
 */
public class RingBufferTest extends TestCase {

    private static final int ITNUM = 1025;
    
    private RingBuffer<Double> m_double;
    private RingBuffer<String> m_string;
    private RingBuffer<Double> m_double_s;
    private RingBuffer<String> m_string_s;
    
    protected void setUp() throws Exception {
        super.setUp();

        this.m_double = new RingBuffer<Double>(17);
        this.m_string = new RingBuffer<String>(17);
        this.m_double_s = new RingBuffer<Double>(2);
        this.m_string_s = new RingBuffer<String>(2);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * <p>バッファ長取得のチェック
     * <ul>
     * <li>コンストラクタで指定されたバッファ長が正しく取得できるか？</li>
     * </ul>
     * </p>
     */
    public void test_length() {

        RingBuffer<Integer> buff1 = new RingBuffer<Integer>(123);
        assertEquals(123, buff1.length());
        
        RingBuffer<Integer> buff2 = new RingBuffer<Integer>(456);
        assertEquals(456, buff2.length());
    }
    
    /**
     * <p>バッファ長取得のチェック
     * <ul>
     * <li>バッファ初期化直後、空ではないと判定されるか？</li>
     * <li>最後にデータが読み取られた後、新しいデータが書き込みされていない場合、空と判定されるか？</li>
     * <li>最後にデータが読み取られた後、新しいデータが書き込みされた場合、空ではないと判定されるか？</li>
     * </ul>
     * </p>
     */
    public void test_empty2() {
        			
        int length = 10;
        RingBuffer<Integer> buff = new RingBuffer<Integer>(length);

        // Check immediately after initialization
        int initialValue = 12345;
        for (int i=0; i < buff.length(); ++i) {
            buff.write(initialValue);
        }
        assertEquals(false, buff.empty());			
			
        
        // Check after data is read
        DataRef<Integer> readValue = new DataRef<Integer>(0);
        assertEquals(ReturnCode.BUFFER_OK, buff.read(readValue));
        assertEquals(false, buff.empty());
			
        // Check after data is written
        int writeValue = 98765;
        assertEquals(ReturnCode.BUFFER_OK, buff.write(writeValue));
        assertEquals(false, buff.empty());
    }
    /**
     * <p>isEmpty()メソッドのテスト
     * <ul>
     * <li>バッファ初期化直後、空ではないと判定されるか？</li>
     * <li>最後にデータが読み取られた後、新しいデータが書き込みされていない場合、空と判定されるか？</li>
     * <li>最後にデータが読み取られた後、新しいデータが書き込みされた場合、空ではないと判定されるか？</li>
     * 　　※OpenRTM v0.4.0ではRingBufferの不具合により本テストはNGとなる。v0.4.1ではOKとなるはず。
     * </ul>
     * </p>
     */
    public void test_empty() throws Exception {

        int length = 10;
        Integer value = new Integer(123);
        
        RingBuffer<Integer> buff = new RingBuffer<Integer>(length);
        for (int i=0; i < buff.length(); ++i) {
            buff.write(value);
        }

        DataRef<Integer> data = new DataRef<Integer>(0);
        
        // 最後の１データを残して読み取る
        for (int i = 0; i < length - 1; i++) {
            // 正しく読み取れていることを確認する
            data.v = null; // 前回のデータが残らないようにクリアしておく
            assertEquals(ReturnCode.BUFFER_OK,buff.read(data));
            assertEquals(value.intValue(), data.v.intValue());
            
            // まだ空ではないはず
            assertFalse(buff.empty());
        }
        
        // 最後の１データを読み取る
        data.v = null; // 前回のデータが残らないようにクリアしておく
        assertEquals(ReturnCode.BUFFER_OK,buff.read(data));
        assertEquals(value, data.v);
        
        // 空になったはず
        assertTrue(buff.empty());
    }
    

    /**
     * <p>データ書き込み/読み込みチェック
     * <ul>
     * <li>データの書き込み/読み込みが正常にできるか？(double型データ)</li>
     * </ul>
     * </p>
     */
    public void test_wr_double() throws Exception {
        
        for (int i = 0; i < ITNUM; i++) {
            // データを書き込む
            assertEquals(ReturnCode.BUFFER_OK,this.m_double.write((double) i));
            
            // データを読み出して、書き込んだデータと一致することを確認する
            DataRef<Double> dvar = new DataRef<Double>(null);
            this.m_double.read(dvar);
            assertEquals((double) i, dvar.v);
        }
    }

    /**
     * <p>データ書き込み/読み込みチェック
     * <ul>
     * <li>データの書き込み/読み込みが正常にできるか？(String型データ)</li>
     * </ul>
     * </p>
     */
    public void test_wr_string() throws Exception {
        
        for (int i = 0; i < ITNUM; i++) {
            
            StringBuffer str = new StringBuffer();
            str.append("Hogehoge").append(i);
            
            // String型データを書き込む
            assertEquals(ReturnCode.BUFFER_OK,this.m_string.write(str.toString()));
            
            // データを読み出して、書き込んだデータと一致することを確認する
            DataRef<String> strvar = new DataRef<String>(null);
            this.m_string.read(strvar);
            assertEquals(str.toString(), strvar.v);
        }
    }

    /**
     * <p>データ書き込み/読み込みチェック
     * <ul>
     * <li>短バッファ長バッファへのデータの書き込み/読み込みが正常にできるか？(double型データ)</li>
     * </ul>
     * </p>
     */
    public void test_wr_double_s() throws Exception {

        for (int i = 0; i < ITNUM; i++) {
            
            // データ書き込みが成功することを確認する
            assertEquals(ReturnCode.BUFFER_OK,this.m_double_s.write((double) i));
            
            // データを読み出して、書き込んだデータと一致することを確認する
            DataRef<Double> dvar = new DataRef<Double>(0d);
            this.m_double_s.read(dvar);
            assertEquals((double) i, dvar.v);
        }
    }

    /**
     * <p>データ書き込み/読み込みチェック
     * <ul>
     * <li>短バッファ長バッファへのデータの書き込み/読み込みが正常にできるか？(String型データ)</li>
     * </ul>
     * </p>
     */
    public void test_wr_string_s() throws Exception {
        
        for (int i = 0; i < ITNUM; i++) {
            
            StringBuffer str = new StringBuffer();
            str.append("Hogehoge").append(i);
            
            // データ書き込みが成功することを確認する
            assertEquals(ReturnCode.BUFFER_OK,this.m_string_s.write(str.toString()));
            
            // データを読み出して、書き込んだデータと一致することを確認する
            DataRef<String> strvar = new DataRef<String>("");
            this.m_string_s.read(strvar);
            assertEquals(str.toString(), strvar.v);
        }
    }

    /**
     * <p>未読み出しデータ有無判定チェック
     * <ul>
     * <li>書き込んだデータを正常に読み出せるか？</li>
     * </ul>
     * </p>
     */
    public void test_isNew_double() throws Exception {
        
        for (int i = 0; i < ITNUM; i++) {
            
            // データを書き込む
            double data = (double) i * 3.14159265;
            this.m_double.write(data);
            
            if ((i % 13) == 0) {

                // 書き込んだデータを正しく読み出せることを確認する
                DataRef<Double> dvar = new DataRef<Double>(0d);
                this.m_double.read(dvar);
                assertEquals("case 1:"+i+":",data, dvar.v.doubleValue());
            }
            
            else if ((i % 7) == 0) {
                
                DataRef<Double> dvar = new DataRef<Double>(0d);

                // 書き込んだデータを正しく読み出せることを確認する
                this.m_double.read(dvar);
                assertEquals("case 2:"+i+":",data, dvar.v.doubleValue());

                this.m_double.read(dvar);
                assertEquals("case 3:"+i+":",data, dvar.v.doubleValue());

                this.m_double.read(dvar);
                assertEquals("case 4:"+i+":",data, dvar.v.doubleValue());
            }
            else {
                DataRef<Double> dvar = new DataRef<Double>(0d);
                this.m_double.read(dvar);
            }
        }
    }

    /**
     * <p>未読み出しデータ有無判定チェック
     * <ul>
     * <li>書き込んだデータを正常に読み出せるか？</li>
     * </ul>
     * </p>
     */
    public void test_isNew_string() throws Exception {

        for (int i = 0; i < ITNUM; i++) {
            
            // データを書き込む
            StringBuffer str = new StringBuffer();
            str.append("Hogehoge").append(i);
            this.m_string.write(str.toString());
            
            if ((i % 13) == 0) {
                
                // 書き込んだデータを正しく読み出せることを確認する
                DataRef<String> strvar = new DataRef<String>("");
                this.m_string.read(strvar);
                assertEquals("case 1:"+i+":",str.toString(), strvar.v);
            }

            else if ((i % 7) == 0) {
                DataRef<String> strvar = new DataRef<String>("");

                // 書き込んだデータを正しく読み出せることを確認する
                this.m_string.read(strvar);
                assertEquals("case 2:"+i+":",str.toString(), strvar.v);
  
                this.m_string.read(strvar);
                assertEquals("case 3:"+i+":",str.toString(), strvar.v);
                
                this.m_string.read(strvar);
                assertEquals("case 4:"+i+":",str.toString(), strvar.v);
            }
            else {
                DataRef<String> strvar = new DataRef<String>("");
                this.m_string.read(strvar);
            }
        }
    }
    /**
     * <p>isFull()メソッドのテスト
     * <ul>
     * <li>バッファが空の場合、フル判定は偽となるか？</li>
     * <li>全バッファにデータが書き込まれている状態でも、フル判定は偽となるか？</li>
     * <li>バッファに幾分データが書き込まれている状態で、フル判定は偽となるか？</li>
     * </ul>
     * </p>
     */
    public void test_full() {
        // (1) バッファが空の場合、フル判定は偽となるか？
        int length1 = 10;
        RingBuffer<Integer> buff1 = new RingBuffer<Integer>(length1);
        assertFalse(buff1.full());
        
        // (2) 全バッファにデータが書き込まれている状態でも、フル判定は偽となるか？
        int length2 = 10;
        RingBuffer<Integer> buff2 = new RingBuffer<Integer>(length2);
        for (int i = 0; i < length2; i++) {
            buff2.write(i);
        }
        assertTrue(buff2.full());
        
        // (3) バッファに幾分データが書き込まれている状態で、フル判定は偽となるか？
        int length3 = 10;
        RingBuffer<Integer> buff3 = new RingBuffer<Integer>(length3);
        for (int i = 0; i < length3 / 2; i++) {
            buff3.write(i);
        }
        assertFalse(buff3.full());
    }
    /**
     * <p>init()メソッドのテスト
     * <ul>
     * <li>あらかじめデータで初期化した後、設定したデータを正しく読み出せるか？</li>
     * </ul>
     * </p>
     */
    public void test_init() {
        // バッファを作成して、init()で初期化する
        int length = 10;
        RingBuffer<Integer> buff = new RingBuffer<Integer>(length);
        
        int value = 12345;
        for (int i=0; i < buff.length(); ++i) {
            buff.write(value);
        }
        
        // 設定したデータを正しく読み出せるか？
        int expected = 12345;
        DataRef<Integer> actual = new DataRef<Integer>(0);
        for (int i = 0; i < length; i++) {
            buff.read(actual);
            assertEquals(expected, actual.v.intValue());
        }
    }
    /**
     * <p>write()メソッドおよびread()メソッドのテスト
     * <ul>
     * <li>バッファ空状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？</li>
     * <li>全バッファにデータが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？</li>
     * <li>全バッファに幾分データが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？</li>
     * </ul>
     * </p>
     */
    public void test_write_read() {
        // (1) In an empty state of the buffer, 
        // can one written data be correctly read?
        // The buffer is made, and it does like being empty.
        int length1 = 3;
        RingBuffer<Integer> buff1 = new RingBuffer<Integer>(length1);
        int length = 3;
        Properties prop = new Properties();
        prop.setProperty("write.full_policy","block");
        prop.setProperty("write.timeout","5.0");
        prop.setProperty("read.empty_policy","block");
        prop.setProperty("read.timeout","5.0");

        buff1.init(prop);
        // One data is written, and one data is read. 
//        for (int writeValue = 0; writeValue < 100; writeValue++) {
        for (int writeValue = 0; writeValue < 10; writeValue++) {
            // Writing
            if (buff1.full()) {
                System.out.println("### FULL ###");
                DataRef<Integer> readValue = new DataRef<Integer>(0);

                if (writeValue % 5 == 0) {
                    while (!buff1.empty()) {
                        System.out.println("read timeout: 5");
                        buff1.read(readValue, 5);
                        System.out.println("readt: "+readValue);
                    }
                    System.out.println("read timeout: 5");
                    System.out.println("waiting 5 sec");
                    System.out.println("read ret: "+buff1.read(readValue, 5));
                    System.out.println("read: "+readValue);
                }
                else {
                    buff1.read(readValue);
                    System.out.println("read: "+readValue);
                }

                if (buff1.full()) {
                    System.out.println("??? still full");
                }
                else {
                    System.out.println("buffer full was blown over.");
                }
            }
            if (buff1.empty()) {
                System.out.println("### EMPTY ###");
            }

            System.out.println("write ret: "
                               + buff1.write(writeValue,writeValue));
            // Reading
            DataRef<Integer> readValue = new DataRef<Integer>(0);
	    buff1.get(readValue);
			
            System.out.println(writeValue+" == "+readValue.v.intValue());

            buff1.read(readValue);
            //Was the written data able to be read correctly?
            assertEquals(writeValue, readValue.v.intValue());
            try {
                Thread.sleep(1000);
            }
            catch(InterruptedException e) {
            }
        }
        return;
    }

    
    public void test_write_read2() {
        //(2)Can the buffer correctly read one written data 
        //in the state of full?
        int length2 = 10;
        RingBuffer<Integer> buff2 = new RingBuffer<Integer>(length2);
        DataRef<Integer> readValue = new DataRef<Integer>(0);
        for (int i = 0; i < length2; i++) {
            buff2.write(i + 123);
        }
            
        // １データ書込・読出を行う
        for (int writeValue = 0; writeValue < 100; writeValue++) {
            // 書込み
            buff2.write(writeValue);
            
            // 読出し
            buff2.read(readValue);
            
            // 書き込んだデータを正しく読み出せたか？
            if(writeValue<9){
                assertEquals("1:"+writeValue+":",
                             writeValue+123+1, 
                             readValue.v.intValue());
            }
            else {
                assertEquals("1:"+writeValue+":",
                             writeValue-9, 
                             readValue.v.intValue());
            }
        }
            
        // (3) バッファに幾分データが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？
        int length3 = 10;
        RingBuffer<Integer> buff3 = new RingBuffer<Integer>(length3);
        for (int i = 0; i < length3 / 2; i++) {
            buff3.write(i + 123);
        }
        
        // １データ書込・読出を行う
        for (int writeValue = 0; writeValue < 100; writeValue++) {
            // 書込み
            buff3.write(writeValue);
            
            // 読出し
            buff3.read(readValue);
            
            // 書き込んだデータを正しく読み出せたか？
            if(writeValue<(length3 / 2)) {
                assertEquals("2:"+writeValue+":",
                             writeValue+123, 
                             readValue.v.intValue());
            }
            else {
                assertEquals("2:"+writeValue+":",
                             writeValue-(length3 / 2), 
                             readValue.v.intValue());
            }
        }
    }


    class writing_thread extends Thread {
        RingBuffer<Integer> m_buff;
        int m_loopcnt;
        public writing_thread(RingBuffer<Integer> buff, int loop) {
            m_buff = buff;
            m_loopcnt = loop;
        }
        
        public void start() {
            super.start();
        }
        
        public void run() {
            //DataRef<Integer> readValue = new DataRef<Integer>(0);
            for (int i = 0; i < m_loopcnt; ++i) {
                //m_buff.read(readValue);
                m_buff.write(i);
            }
        }
        
        ArrayList m_data = new ArrayList();
        public ArrayList getData() {
            return m_data;
        }
    }

    public void test_write_read3() {
        int loopcnt = 1000000;
        Properties prop = new Properties();
        prop.setProperty("write.full_policy","block");
        prop.setProperty("write.timeout","5.0");
        prop.setProperty("read.empty_policy","block");
        prop.setProperty("read.timeout","5.0");
        RingBuffer<Integer> buff = new RingBuffer<Integer>(8);
        DataRef<Integer> readValue = new DataRef<Integer>(0);
        buff.init(prop);
        writing_thread wt = new writing_thread(buff, loopcnt);
        wt.start();
        try {
            Thread.sleep(10);
        }
        catch(InterruptedException e) {
        }
        ArrayList rdata = new ArrayList();
        for (int i = 0; i < loopcnt; i++) {
            buff.read(readValue);
            rdata.add(i);
            //buff.write(i);
        }
        try {
            wt.join();
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        ArrayList wdata = wt.getData();
        Iterator w_ite = wdata.iterator();
        Iterator r_ite = rdata.iterator();
        while(w_ite.hasNext()) { 
            assertEquals("rdata==wdata",w_ite.next(),r_ite.next());
        }
    }
    
    
    /**
     * <p>write()メソッドおよびread()メソッドのテスト（バッファ長２の場合）
     * <ul>
     * <li>バッファ空状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？</li>
     * <li>全バッファにデータが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？</li>
     * <li>バッファに幾分データが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？</li>
     * </ul>
     * </p>
     */
    public void test_write_read_with_small_length() {
        // (1) バッファ空状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？
        // バッファ作成し、空のままにする
        int length1 = 2;
        RingBuffer<Integer> buff1 = new RingBuffer<Integer>(length1);
        DataRef<Integer> readValue = new DataRef<Integer>(0);
        
        // １データ書込・読出を行う
        for (int writeValue = 0; writeValue < 100; writeValue++) {
            // 書込み
            buff1.write(writeValue);
            
            // 読出し
            buff1.read(readValue);
            
            // 書き込んだデータを正しく読み出せたか？
            assertEquals("(1)",writeValue, readValue.v.intValue());
        }
			
        // (2) 全バッファにデータが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？
        // バッファ作成し、フル状態にする
        int length2 = 2;
        RingBuffer<Integer> buff2 = new RingBuffer<Integer>(length2);
        Properties prop = new Properties();
        prop.setProperty("write.full_policy","overwrite");
        buff2.init(prop);

        for (int i = 0; i < length2; i++) {
            buff2.write(i);
        }
        // １データ書込・読出を行う
        for (int writeValue = 2; writeValue < 102; writeValue++) {
            // 書込み
            buff2.write(writeValue);
            // 読出し
            buff2.read(readValue);
          
            // 書き込んだデータを正しく読み出せたか？
            assertEquals("(2)-1",writeValue, readValue.v.intValue()+1);
        }
			
        //Writing
        //When Policy writes it with Overwrite in the state of Full,
        //Old data is overwrited, 
        //and the increment does the pointer on the reading side.
	buff2.write(0);
				
        assertEquals("(2)-2",true, buff2.full());
        //Readinfg
        // Because it reads out data from the reading side and 
        // the number of pointers is increased, 
        // the buffer is not in the state of full.
	buff2.read(readValue);

        assertEquals("(2)-3",false, buff2.full());
        // Can the written data be correctly read?
	assertEquals("(2)-4",101, readValue.v.intValue());
			
        // (3) バッファに幾分データが書き込まれている状態で１データ書込・読出を行い、書き込んだデータを正しく読み出せるか？
        int length3 = 2;
        RingBuffer<Integer> buff3 = new RingBuffer<Integer>(length3);
        Properties prop3 = new Properties();
        prop3.setProperty("write.full_policy", "overwrite");
        buff3.init(prop3);
        for (int i = 0; i < 1; i++) {
            buff3.write(i + 123);
        }
        
        {	
            //Writing
	    buff3.write(-1);
				
	    // Reading
	    buff3.read(readValue);
				
            // Can the written data be correctly read?
	    assertEquals("(3)-1",123, readValue.v.intValue());
        }
        // １データ書込・読出を行う
        for (int writeValue = 0; writeValue < 100; writeValue++) {
            // 書込み
            buff3.write(writeValue);
            
            // 読出し
            buff3.read(readValue);
            
            // 書き込んだデータを正しく読み出せたか？
            assertEquals("(3)-2",writeValue-1, readValue.v.intValue());
        }
    }
    /**
     *  <p> Test of reset() method .</p>
     */
    public void test_reset() {
        int[] idata = {123,456,789,321,654,987,1234,3456,5678,7890};
        RingBuffer<Integer> buff = new RingBuffer<Integer>(10);

        for(int ic=0;ic<8;++ic) {
            buff.put(idata[ic]);
            buff.advanceWptr();
        }
        buff.advanceRptr(3);
        assertEquals(buff.get().intValue(), idata[3]);
        assertEquals(buff.readable(), 5);

        buff.reset();
        assertTrue(buff.empty());
        assertEquals(buff.get().intValue(), idata[0]);
        buff.put(idata[9]);
        assertEquals(buff.get().intValue(), idata[9]);
        assertEquals(buff.readable(), 0);
    }
    /**
     *  <p> Test of wptr() and put(). </p>
     * 
     */
    public void test_wptr_put() {
        int[] idata = {123,456,789,321,654,987,1234,3456,5678,7890};
        RingBuffer<Integer> buff = new RingBuffer<Integer>(10);
        for(int ic=0;ic<10;++ic) {
            buff.put(idata[ic]);
            buff.advanceWptr();
        }
        buff.reset();
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[ic],buff.wptr(ic).intValue());
        }
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[(-ic+10)%10],buff.wptr(-ic).intValue());
        }
        buff.advanceWptr(5);
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[(5+ic)%10],buff.wptr(ic).intValue());
        }
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[(5-ic+10)%10],buff.wptr(-ic).intValue());
        }
    }
    /**
     *  <p> Test of advanceWptr(). </p>
     * 
     */
    public void test_advanceWptr() {
        int[] idata = {123,456,789,321,654,987,1234,3456,5678,7890};
        RingBuffer<Integer> buff = new RingBuffer<Integer>(10);

        assertEquals(buff.advanceWptr(-5),ReturnCode.PRECONDITION_NOT_MET);
        assertEquals(buff.advanceWptr(5),ReturnCode.BUFFER_OK);
        assertEquals(buff.advanceWptr(8),ReturnCode.PRECONDITION_NOT_MET);
        assertEquals(buff.advanceWptr(-5),ReturnCode.BUFFER_OK);
        buff.reset();
        for(int ic=0;ic<10;++ic) {
            buff.put(idata[ic]);
            buff.advanceWptr();
        }
        buff.reset();
        assertEquals(buff.advanceWptr(5),ReturnCode.BUFFER_OK);
        buff.advanceRptr(5);
        assertEquals(buff.advanceWptr(-5),ReturnCode.PRECONDITION_NOT_MET);
        assertEquals(buff.advanceWptr(8),ReturnCode.BUFFER_OK);
        assertEquals(idata[3],buff.wptr().intValue());
        assertEquals(8,buff.readable());
        assertEquals(buff.advanceWptr(-5),ReturnCode.BUFFER_OK);
        assertEquals(idata[8],buff.wptr().intValue());
        assertEquals(3,buff.readable());
    }
    /**
     *  <p> Test of rptr() and get(). </p>
     * 
     */
    public void test_rptr_get() {
        int[] idata = {123,456,789,321,654,987,1234,3456,5678,7890};
        RingBuffer<Integer> buff = new RingBuffer<Integer>(10);

        for(int ic=0;ic<10;++ic) {  
            buff.put(idata[ic]);
            buff.advanceWptr();
        }
        buff.reset();
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[ic],buff.rptr(ic).intValue());
        }
        for(int ic=0;ic<10;++ic) { 
            assertEquals(idata[(-ic+10)%10],buff.rptr(-ic).intValue());
        }
        buff.advanceWptr(5);
        buff.advanceRptr(5);
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[(5+ic)%10],buff.rptr(ic).intValue());
        }
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[(5-ic+10)%10],buff.rptr(-ic).intValue());
        }
        buff.reset();
        buff.advanceWptr(10);
        for(int ic=0;ic<10;++ic) {
            assertEquals(idata[ic],buff.get().intValue());
            DataRef<Integer> ret = new DataRef<Integer>(0);
            buff.get(ret);
            assertEquals(idata[ic],ret.v.intValue());
            buff.advanceRptr();
        }

    }
    /**
     *  <p> Test of advanceRptr(). </p>
     * 
     */
    public void test_advanceRptr() {
        int[] idata = {123,456,789,321,654,987,1234,3456,5678,7890};
        RingBuffer<Integer> buff = new RingBuffer<Integer>(10);

        buff.advanceWptr(5);
        assertEquals(buff.advanceRptr(-6),ReturnCode.PRECONDITION_NOT_MET);
        assertEquals(buff.advanceRptr(5),ReturnCode.BUFFER_OK);
        assertEquals(buff.advanceRptr(8),ReturnCode.PRECONDITION_NOT_MET);
        assertEquals(buff.advanceRptr(-5),ReturnCode.BUFFER_OK);
        buff.reset();
        buff.advanceWptr(5);
        buff.advanceRptr(5);
        for(int ic=0;ic<10;++ic) {
            buff.put(idata[ic]);
            buff.advanceWptr();
        }
        assertEquals(buff.advanceRptr(-6),ReturnCode.PRECONDITION_NOT_MET);
        assertEquals(buff.advanceRptr(8),ReturnCode.BUFFER_OK);
        assertEquals(idata[8],buff.rptr().intValue());
        assertEquals(8,buff.writable());
        assertEquals(buff.advanceRptr(-5),ReturnCode.BUFFER_OK);
        assertEquals(idata[3],buff.rptr().intValue());
        assertEquals(3,buff.writable());
    }
}
