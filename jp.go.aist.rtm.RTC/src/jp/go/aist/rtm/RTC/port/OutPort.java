package jp.go.aist.rtm.RTC.port;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import java.util.Vector;

import jp.go.aist.rtm.RTC.buffer.BufferBase;
import jp.go.aist.rtm.RTC.buffer.RingBuffer;
import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.port.publisher.PublisherBase;
import jp.go.aist.rtm.RTC.util.DataRef;
import jp.go.aist.rtm.RTC.util.NVUtil;
import jp.go.aist.rtm.RTC.util.ORBUtil;
import jp.go.aist.rtm.RTC.util.CORBA_SeqUtil;

import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

import _SDOPackage.NVListHolder;
/**
 * {@.ja 出力ポートの実装。}
 * {@.en Implementation of OutPort}
 * <p>
 * {@.ja さらに上位の出力ポートのベース実装として利用される。}
 * {@.en This class is used as the base Implementation of a high-ranking output
 * port.}
 *
 * @param <DataType> 
 *   {@.ja データ型を指定する。}
 *   {@.en The data type is specified}
 */
public class OutPort<DataType> extends OutPortBase {

    /**
     * {@.ja }
     * {@.en }
     */
/*
    private class getInfo{
        private HashMap<String, Class> types 
            = new HashMap<String, Class>();

        private HashMap<String, Integer> nums = new HashMap<String, Integer>();

        public getInfo() {
            types.put("byte",byte.class);
            types.put("char",char.class);
            types.put("short",short.class);
            types.put("int",int.class);
            types.put("long",long.class);
            types.put("float",float.class);
            types.put("double",double.class);

            nums.put("byte",0);
            nums.put("char",1);
            nums.put("short",2);
            nums.put("int",3);
            nums.put("long",4);
            nums.put("float",5);
            nums.put("double",6);
        }
        public Class getType(String className) {
            return (Class) types.get(className);
        } 
        public int getNumber(String className) {
            return nums.get(className);
        } 
    }
*/
    /**
     * <p> toTypeCode </p>
     * <p> This function gets TypeCode of data. </p>
     *
     * @param value data
     * @return TypeCdoe(String)
     */
    private static <DataType> String toTypeCode(DataRef<DataType> value) { 
        String typeName = value.v.getClass().getSimpleName();
        return typeName;

    }
    /**
     * <p> toRepositoryId </p>
     * <p> This function gets IFR ID of data. </p>
     *
     * @param value data
     * @return TypeCdoe(String)
     */
    private static <DataType> String toRepositoryId(DataRef<DataType> value){
        String id =  new String();
        Class cl = value.v.getClass();
        String str = cl.getName();
        try {
            Class helper = Class.forName(str+"Helper");

            Method method = helper.getMethod("id");
            id =  (String)method.invoke(
                null // invoke static method.
                );
        }
        catch(java.lang.Exception e){
//            rtcout.println(Logbuf.WARN, 
//                   "Exception caught."+e.toString());
        }
        return id;
    }
    /**
     * {@.ja OutputStreamに整列化する}
     * {@.en Marshals to ostream the data.}
     * 
     *
     * @param data
     *   {@.ja データ}
     *   {@.en data}
     * @param cdr   
     *   {@.ja OutputStream}
     *   {@.en OutputStream}
     */
    public void write_stream(DataType data,OutputStream cdr) {
        try {
            m_field.set(m_streamable,data);
            m_streamable._write(cdr);

        }
        catch(IllegalAccessException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(IllegalArgumentException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }

    }
    /**
     * {@.ja タイムスタンプを設定する。}
     * {@.en Sets timestamp.}
     * <p>
     * {@.ja データポートのデータに対してタイムスタンプをセットする。
     * データポートのデータは構造体のメンバーとして 
     * tm.sec, tm.nsec を持つ必要がある。}
     * {@.en This function sets timestamp to data of data port. This data should
     * have tm.sec, tm.nsec as members of the structure.}
     * </p>
     * @param data 
     *   {@.ja タイムスタンプをセットするデータ。実行後実行時のタイムス
     *   タンプがセットされる}
     *   {@.en Data to be set timestamp. After executing this
     *   function, current timestamp is set to the data.}
     */
    public void setTimestamp(DataType data) {
        long nanotime = System.nanoTime();
        RTC.Time tm = new RTC.Time((int)(nanotime/1000000000),
                                   (int)(nanotime%1000000000));
        Class cl = data.getClass();
        String str = cl.getName();
        try {
            cl.getField("tm").set(data,tm);
        }
        catch(NoSuchFieldException e){
            //getField throws
        }
        catch(IllegalAccessException e){
            //set throws
        }
         
    }
    private void set_timestamp(DataType data) {
        long nanotime = System.nanoTime();
        RTC.Time tm = new RTC.Time((int)(nanotime/1000000000),
                                   (int)(nanotime%1000000000));
        Class cl = data.getClass();
        String str = cl.getName();
        try {
            cl.getField("tm").set(data,tm);
        }
        catch(NoSuchFieldException e){
            //getField throws
        }
        catch(IllegalAccessException e){
            //set throws
        }
         
    }
    /**
     * {@.ja タイムスタンプを取得する。}
     * {@.en Gets the timestamp. }
     *
     * @return
     *   {@.ja 取得したTime}
     *   {@.en RTC.Time gotten}
     */
    public static RTC.Time get_timestamp() {
        long nanotime = System.nanoTime();
        RTC.Time tm = new RTC.Time((int)(nanotime/1000000000),
                                   (int)(nanotime%1000000000));
        return tm; 
    }
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 内部的にバッファが生成されて割り当てられる。}
     * {@.en Internally creates and allcoates the buffer}
     * 
     * @param name 
     *   {@.ja ポート名}
     *   {@.en Name of Port}
     * @param valueRef 
     *   {@.ja 本ポートにバインドするデータ変数を内包するDataRefオブジェクト}
     *   {@.en DataRef object}
     */
    public OutPort(final String name, DataRef<DataType> valueRef) {
        this(new RingBuffer<DataType>(8), name, valueRef);
    }
    
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたデータ長で内部的にバッファが生成されて割り当てられvyる。}
     * {@.en Internally creates and allocates the buffer by the specified data
     * length}
     * 
     * @param name 
     *   {@.ja ポート名}
     *   {@.en the port name}
     * @param valueRef 
     *   {@.ja 本ポートにバインドするデータ変数を内包するDataRefオブジェクト}
     *   {@.en DataRef object}
     * @param length 
     *   {@.ja バッファ長}
     *   {@.en Buffer length}
     */
    public OutPort(final String name, DataRef<DataType> valueRef, int length) {
        this(new RingBuffer<DataType>(length), name, valueRef);
    }
    
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたバッファを割り当てる。}
     * {@.en Allocates the specified buffer.}
     * 
     * @param buffer 
     *   {@.ja 割り当てるバッファ}
     *   {@.en buffer}
     * @param name 
     *   {@.ja ポート名}
     *   {@.en Port name}
     * @param valueRef 
     *   {@.ja 本ポートにバインドするデータ変数を内包するDataRefオブジェクト}
     *   {@.en DataRef object}
     */
    public OutPort(BufferBase<DataType> buffer,
            final String name, DataRef<DataType> valueRef) {
        
        super(name,toRepositoryId(valueRef));
        
        this.m_value = valueRef;
        this.m_timeoutTick = 1000; // [usec]
        this.m_readBlock = false;
        this.m_readTimeout = 0; // [usec]
        this.m_writeBlock = false;
        this.m_writeTimeout = 0; // [usec]
        
        this.m_OnWrite = null;
        this.m_OnWriteConvert = null;
        this.m_OnRead = null;
        this.m_OnReadConvert = null;
        this.m_OnOverflow = null;
        this.m_OnUnderflow = null;


        Class cl = valueRef.v.getClass();
        String str = cl.getName();
        try {
            Class holder = Class.forName(str+"Holder",
                                         true,
                                         this.getClass().getClassLoader());
            m_streamable = (Streamable)holder.newInstance();
            m_field = m_streamable.getClass().getField("value");

        }
        catch(java.lang.InstantiationException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
         }

        catch(ClassNotFoundException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(NoSuchFieldException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(IllegalAccessException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(IllegalArgumentException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
    }
    
    /**
     * {@.ja データ書き込み}
     * {@.en Write data}
     *
     * <p>
     * {@.ja ポートへデータを書き込む。
     * <ul>
     * <li> コールバックファンクタ OnWrite がセットされている場合、
     *      OutPort が保持するバッファに書き込む前に OnWrite が呼ばれる。</li>
     * <li> OutPort が保持するバッファがオーバーフローを検出できるバッファであ
     *      り、かつ、書き込む際にバッファがオーバーフローを検出した場合、
     *      コールバックファンクタ OnOverflow が呼ばれる。</li>
     * <li> コールバックファンクタ OnWriteConvert がセットされている場合、
     *      バッファ書き込み時に、 OnWriteConvert の operator() の戻り値が
     *      バッファに書き込まれる。</li>
     * </ul>}
     * {@.en Write data in the port.
     * <ul>
     * <li> When callback functor OnWrite is already set, OnWrite will be
     *      invoked before writing into the buffer held by OutPort.
     * <li> When the buffer held by OutPort can detect the overflow,
     *      and when it detected the overflow at writing, callback functor
     *      OnOverflow will be invoked.
     * <li> When callback functor OnWriteConvert is already set, 
     *      the return value of operator() of OnWriteConvert will be written
     *      into the buffer at the writing.
     * </ul>}
     * </p>
     * @param value 
     *   {@.ja 書き込み対象データ}
     *   {@.en The target data for writing}
     *
     * @return 
     *   {@.ja 書き込み処理結果(書き込み成功:true、書き込み失敗:false)}
     *   {@.en Writing result (Successful:true, Failed:false)}
     *
     */
    public boolean write(final DataType value) {
        rtcout.println(Logbuf.TRACE, "DataType write()");

        if (m_OnWrite != null) {
            m_OnWrite.run(value);
            rtcout.println(Logbuf.TRACE, "OnWrite called");
        }

        // 1) direct connection
        synchronized (m_directNewDataMutex){
            rtcout.println(Logbuf.DEBUG, "Direct data transfer");
            if (m_OnWriteConvert != null) {
                m_value.v = m_OnReadConvert.run(m_value.v);
                rtcout.println(Logbuf.DEBUG, 
                     "OnReadConvert for direct data called");
                m_OnWriteConvert.run(value);
                m_value.v = value;
                return true;
            }
        }
        // 2) network connection
        boolean result = true;
        Vector<String> disconnect_ids = new Vector<String>();
        synchronized (m_connectorsMutex){

            synchronized (m_connectors){
                // check number of connectors
                int conn_size = m_connectors.size();
                if (!(conn_size > 0)) { 
                    return false; 
                }
        
                // set timestamp
//                set_timestamp(value);

                m_status.setSize(conn_size);

                for (int i=0, len=conn_size; i < len; ++i) {
                    ReturnCode ret;
                    // data -> (conversion) -> CDR stream
                    if (m_OnWriteConvert != null) {
                        rtcout.println(Logbuf.DEBUG, 
                                "m_connectors.OnWriteConvert called");
                        ret = m_connectors.elementAt(i).write(
                                                m_OnWriteConvert.run(value));
                    }
                    else{
                        rtcout.println(Logbuf.DEBUG, 
                                "m_connectors.write called");
                        ret = m_connectors.elementAt(i).write(value);
                    }

                    m_status.add(i, ret);
                    if (ret.equals(ReturnCode.PORT_OK)) {
                        continue;
                    }

                    result = false;
                    String id = m_connectors.elementAt(i).id();

                    if (ret.equals(ReturnCode.CONNECTION_LOST)) {
                        rtcout.println(Logbuf.TRACE, "connection_lost id: "+id);
                        synchronized (m_onConnectionLost_mutex){
                            if(m_onConnectionLost != null) {
                                RTC.ConnectorProfile prof = findConnProfile(id);
                                RTC.ConnectorProfileHolder holder 
                                    = new RTC.ConnectorProfileHolder(prof);
                                m_onConnectionLost.run(holder);
                            }
                        }
                        disconnect_ids.add(id);
                    }
                }
            }
        }
        
        java.util.Iterator it = disconnect_ids.iterator();
        while (it.hasNext()) {
            disconnect((String)it.next());
        }
        return result;
    }
    
    /**
     * {@.ja データを書き込む。}
     * {@.en Writes the data}
     * <p>
     * {@.ja 本ポートにバインドされているデータ変数の値が書き込まれる。}
     * {@.en Writes the value in the variable to which bind is done. }
     * 
     * @return 
     *   {@.ja データを書き込めた場合はtrueを、さもなくばfalseを返す。}
     *   {@.en "True" is returned when succeeding}
     */
    public boolean write() {
        return this.write(m_value.v);
    }
    
    
    /**
     * {@.ja データ書き込み}
     * {@.en Write data}
     * <p>
     * {@.ja ポートへデータを書き込む。設定された値をポートに書き込む。}
     * {@.en  Write data to the port. Write the set value to the port.}
     * </p>
     * @param value 
     *   {@.ja 書き込み対象データ}
     *   {@.en The target data for writing}
     *
     * @return  
     *   {@.ja 書き込み処理結果(書き込み成功:true、書き込み失敗:false)}
     *   {@.en Writing result (Successful:true, Failed:false)}
     *
     */
    public boolean insert(DataType value) {
        return write(value);
    }

    /**
     * {@.ja データ読み出し時における、ブロック/非ブロックモードを指定する。}
     * {@.en Specifies the block mode in the data reading.}
     * <p>
     * {@.ja ブロックモードを指定した場合は、
     * 読み出せるデータを受信するかタイムアウトになるまで、
     * read()メソッドの呼び出しがブロックされる。}
     * {@.en When the block mode is specified, the read() method is blocked
     * until data is read or until the time-out.}
     * 
     * @param block
     *   {@.ja ブロックモードを指定する場合はtrue、さもなくばfalse}
     *   {@.en True when block mode is specified}
     */
    public void setReadBlock(boolean block) {
        this.m_readBlock = block;
    }
    
    /**
     * {@.ja データ書き込み時における、ブロック/非ブロックモードを指定する。}
     * {@.en Specifies the block mode in the data writing.} 
     * <p>
     * {@.ja ブロックモードを指定した場合は、
     * バッファに書き込む余地ができるかタイムアウトになるまで、
     * write()メソッドの呼び出しがブロックされる。}
     * {@.en When the block mode is specified, the write() method is blocked.}
     * 
     * @param block
     *   {@.ja ブロックモードを指定する場合はtrue、さもなくばfalse}
     *   {@.en True when block mode is specified}
     */
    public void setWriteBlock(boolean block) {
        this.m_writeBlock = block;
    }

    /**
     * {@.ja 読み出しがブロックモード指定されている場合のタイムアウト時間を
     * 設定する。}
     * {@.en Sets the timeout period of the block mode of reading.}
     * 
     * @param timeout 
     *   {@.ja タイムアウト時間 [usec]}
     *   {@.en Timeout[usec]}
     */
    public void setReadTimeout(long timeout) {
        this.m_readTimeout = timeout;
    }
    
    /**
     * {@.ja 書き込みがブロックモード指定されている場合のタイムアウト時間を
     * 設定する。}
     * {@.en Sets the timeout period of the block mode of writing.}
     * 
     * @param timeout 
     *   {@.ja タイムアウト時間 [usec]}
     *   {@.en Timeout[usec]}
     */
    public void setWriteTimeout(long timeout) {
        this.m_writeTimeout = timeout;
    }
    
    /**
     * {@.ja 特定のコネクタへの書き込みステータスを得る}
     * {@.en brief Getting specified connector's writing status}
     *
     * <p>
     * {@.ja OutPort は接続ごとに Connector と呼ばれる仮想データチャネルを持
     * つ。write() 関数はこれら Connector に対してデータを書き込むが、
     * 各 Connector は書き込みごとにステータスを返す。write() 関数では、
     * すべての Connector が正常終了したときのみ true を返し、それ以外
     * では false を返却する。この関数は write() が false の場合ステー
     * タスを調べるのに使用することができる。}
     * {@.en An OutPort has Connectors that are virtual data stream channel
     * for each connection.  "write()" function write into these
     * Connectors, and each Connector returns writing-status.  write()
     * function will return a true value if all Connectors return
     * normal status, and a false value will be returned if at least
     * one Connector failed.  This function can be used to inspect
     * each return status}
     * </p>
     * 
     * @param index 
     *   {@.ja Connector の index}
     *   {@.en Connector index}
     * @return 
     *   {@.ja ステータス}
     *   {@.en Writing status}
     *
     */
    ReturnCode getStatus(int index) {
        return m_status.elementAt(index);
    }

    /**
     * {@.ja 特定のコネクタへの書き込みステータスリストを得る}
     * {@.en Getting specified connector's writing status list}
     *
     * <p>
     * {@.ja OutPort は接続ごとに Connector と呼ばれる仮想データチャネルを持
     * つ。write() 関数はこれら Connector に対してデータを書き込むが、
     * 各 Connector は書き込みごとにステータスを返す。write() 関数では、
     * すべての Connector が正常終了したときのみ true を返し、それ以外
     * では false を返却する。この関数は write() が false の場合ステー
     * タスを調べるのに使用することができる。}
     * {@.en An OutPort has Connectors that are virtual data stream channel
     * for each connection.  "write()" function write into these
     * Connectors, and each Connector returns writing-status.  write()
     * function will return a true value if all Connectors return
     * normal status, and a false value will be returned if at least
     * one Connector failed.  This function can be used to inspect
     * each return status}
     * </p>
     * 
     * @return 
     *   {@.ja ステータスリスト}
     *   {@.en Writing status list}
     */
    Vector<ReturnCode> getStatusList() {
        return m_status;
    }

    /**
     * {@.ja データ書き込み直前に呼び出されるコールバックインタフェースを
     * 設定する。}
     * {@.en Sets the callback interface called immediately before writing of
     * data.}
     * 
     * @param onWrite 
     *   {@.ja OnWriteコールバックインタフェースを持つオブジェクト}
     *   {@.en Object with OnWrite callback interface}
     */
    public void setOnWrite(OnWrite<DataType> onWrite) {
        this.m_OnWrite = onWrite;
    }
    
    /**
     * {@.ja データ書き込み時に呼び出されるコールバックインタフェースを
     * 設定する。}
     * {@.en sets the callback interface called when data is written.}
     * 
     * <p>
     * {@.ja 設定されたコールバックの戻り値のデータが書き込まれる。
     * これにより、書き込みデータのフィルタリングを行うことがでる。}
     * 
     * @param onWriteConvert 
     *   {@.ja OutWriteConvertコールバックインタフェースを持つオブジェクト}
     *   {@.en Object with OutWriteConvert callback interface}
     */
    public void setOnWriteConvert(OnWriteConvert<DataType> onWriteConvert) {
        this.m_OnWriteConvert = onWriteConvert;
    }
    
    /**
     * {@.ja バッファフルにデータ書き込みできない場合に呼び出される
     * コールバックインタフェースを設定する。}
     * {@.en sets the callback interface called when data cannot be written in
     * a full buffer.}
     * 
     * @param onOverflow 
     *   {@.ja OutOverflowコールバックインタフェースを持つオブジェクト}
     *   {@.en Object with OutOverflow callback interface}
     */
    public void setOnOverflow(OnOverflow<DataType> onOverflow) {
        this.m_OnOverflow = onOverflow;
    }
    
    /**
     * {@.ja データ読み出しの直前に呼び出されるコールバックインタフェースを
     * 設定する。}
     * {@.en sets the callback interface called immediately before reading of
     * data.}
     * 
     * @param onRead 
     *   {@.ja OutReadコールバックインタフェースを持つオブジェクト}
     *   {@.en Object with OutRead callback interface}
     */
    public void setOnRead(OnRead<DataType> onRead) {
        this.m_OnRead = onRead;
    }
    
    /**
     * {@.ja データ読み出し時に呼び出されるコールバックインタフェースを
     * 設定する。}
     * {@.en sets the callback interface called when data is read.}
     * 
     * <p>
     * {@.ja 設定されたコールバックの戻りデータ値が読み出される。
     * これにより、読み出しデータをフィルタリングすることができる。}
     * 
     * @param onReadConvert 
     *   {@.ja OutReadConvertコールバックインタフェースを持つオブジェクト}
     *   {@.en Object with OutReadConvert callback interface}
     */
    public void setOnReadConvert(OnReadConvert<DataType> onReadConvert) {
        this.m_OnReadConvert = onReadConvert;
    }
    
    /**
     * {@.ja バッファ空により読み取れるデータがない場合に呼び出される
     * コールバックインタフェースを設定する。}
     * {@.en sets the callback interface called when there is no data that can
     * be read in an empty buffer more. }
     * 
     * @param onUnderflow 
     *   {@.ja OutUnderflowコールバックインタフェースを持つオブジェクト}
     *   {@.en Object with OutUnderflow callback interface}
     */
    public void setOnUnderflow(OnUnderflow<DataType> onUnderflow) {
        this.m_OnUnderflow = onUnderflow;
    }
    

    /**
     * <p> Set OnConnect callback </p>
     * 
     */
    public void setOnConnect(OnConnect on_connect) {
        m_OnConnect = on_connect;
    }
    /**
     * <p> Set OnDisConnect callback </p>
     * 
     */
    public void setOnDisconnect(OnDisconnect on_disconnect) {
        m_OnDisconnect = on_disconnect;
    }
    /**
     * 
     */
    public void onConnect(final String id, PublisherBase publisher) {
        rtcout.println(Logbuf.TRACE, "onConnect(id = "+id+")");
    }
    /**
     * 
     */
    public void onDisconnect(final String id) {
        rtcout.println(Logbuf.TRACE, "onDisconnect(id = "+id+")");
    }

    /**
     * {@.ja CDR化で使用するStreamableを設定する}
     * {@.en Sets Streamable. }
     *
     * <p> 
     * {@.ja 与えられたStreamableをCDR化で使用するStreamableへ設定する。
     * また、与えられたStreamableからvalueフィールドを取得し保持する。}
     * {@.en This method sets Streamable used when making it to CDR. 
     * Moreover, this method acquires the value field from Streamable. }
     *
     * @param holderStreamable
     *   {@.ja  HolderクラスのStreamable}
     *   {@.en  Streamable of Holder class}
     * @return 
     *   {@.ja false 失敗}
     *   {@.en false Failure}
     * 
     *
     */
    public boolean setStreamable(Streamable holderStreamable) {
        try {
            m_streamable = holderStreamable;
            m_field = m_streamable.getClass().getField("value");
            return true;
        }
        catch(NoSuchFieldException e){
            //getField throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        return false;
    }

    /**
     * {@.ja 値を読み出す}
     * {@.en Readout the value}
     * <p>
     */
    public void read(DataRef<DataType> data) {
        rtcout.println(Logbuf.TRACE, "read()");
        synchronized (m_value){
            data.v  = m_value.v;
        }

    }

    private BufferBase<DataType> m_buffer;
    private DataRef<DataType> m_value;
    private long m_timeoutTick;
    private boolean m_readBlock;
    private long m_readTimeout;
    private boolean m_writeBlock;
    private long m_writeTimeout;
    
    private OnWrite<DataType> m_OnWrite;
    private OnWriteConvert<DataType> m_OnWriteConvert;
    private OnRead<DataType> m_OnRead;
    private OnReadConvert<DataType> m_OnReadConvert;
    private OnOverflow<DataType> m_OnOverflow;
    private OnUnderflow<DataType> m_OnUnderflow;

    private OnConnect m_OnConnect;
    private OnDisconnect m_OnDisconnect;

    private Streamable m_streamable = null;
    private Field m_field = null;
    private Vector<ReturnCode> m_status = new Vector<ReturnCode>();

    private static String m_directNewDataMutex = new String();

    private int m_propValueIndex;

}
