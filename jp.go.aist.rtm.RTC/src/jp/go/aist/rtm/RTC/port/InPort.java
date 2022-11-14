package jp.go.aist.rtm.RTC.port;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import jp.go.aist.rtm.RTC.buffer.BufferBase;
import jp.go.aist.rtm.RTC.buffer.RingBuffer;
import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.util.DataRef;
import jp.go.aist.rtm.RTC.util.ORBUtil;

import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.Streamable;


/**
 * <p>入力ポートのためのベース実装クラスです。
 * 外部から送信されてきたデータは順次、コンストラクタで指定されたバッファに格納されます。
 * バッファ内のデータはフラグによって未読／既読状態が管理され、isNew(), isEmpty()などの
 * メソッドによってハンドリングできます。</p>
 * 
 * @param <DataType> データ型を指定します。
 */
public class InPort<DataType> extends InPortBase {

    private static final long TIMEOUT_TICK_USEC = 10;
    private static final long TIMEOUT_TICK_MSEC_PART = TIMEOUT_TICK_USEC / 1000;
    private static final int TIMEOUT_TICK_NSEC_PART = ((int) (TIMEOUT_TICK_USEC % 1000)) * 1000;

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
     * <p> read_stream </p>
     * <p> This function reads data from InputStream.  </p>
     *
     * @param data  The read data is stored.  
     * @param cdr   InPutStream
     * @return Read data
     */
    private DataType read_stream(DataRef<DataType> data,InputStream cdr) {

        try {
            m_streamable._read(cdr);
            data.v = (DataType)m_field.get(m_streamable);
        }
        catch(IllegalAccessException e){
            //set throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(IllegalArgumentException e){
            //invoke throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }

        return data.v;
    }
    /**
     * <p>コンストラクタです。</p>
     *
     * @param name ポート名称
     * @param value このポートにバインドされるDataType型の変数
     * @param read_block データ読み込み時に未読データがない場合に、データ受信までブロックする場合はtrue、さもなくばfalse
     * @param write_block データ書き込み時にバッファがフルであった場合に、バッファに空きができるまでブロック場合はtrue、さもなくばfalse
     * @param read_timeout 非ブロック指定の場合の、データ読み取りのタイムアウト時間 (ミリ秒)
     * @param write_timeout 非ブロック指定の場合の、データ書き込みのタイムアウト時間 (ミリ秒)
     */
    public InPort(BufferBase<DataType> superClass,
            final String name, DataRef<DataType> value,
            boolean read_block, boolean write_block,
            long read_timeout, long write_timeout) {
        
        super(name,toRepositoryId(value));
        //super(name, toTypeCode(value));

        this.m_name = name;
        this.m_value = value;
        this.m_readBlock = read_block;
        this.m_writeBlock = write_block;
        this.m_readTimeout = read_timeout;
        this.m_writeTimeout = write_timeout;
        
        this.m_OnWrite = null;
        this.m_OnWriteConvert = null;
        this.m_OnRead = null;
        this.m_OnReadConvert = null;
        this.m_OnOverflow = null;
        this.m_OnUnderflow = null;

        this.m_directNewData = false;

        m_orb = ORBUtil.getOrb();

        Class cl = value.v.getClass();
        String str = cl.getName();
        try {
            Class holder = Class.forName(str+"Holder",
                                         true,
                                         this.getClass().getClassLoader());
            m_streamable = (Streamable)holder.newInstance();
            m_field = m_streamable.getClass().getField("value");
        }
        catch(NoSuchFieldException e){
            //getField throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(java.lang.InstantiationException e){
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(ClassNotFoundException e){
            //forName throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(IllegalAccessException e){
            //set throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        catch(IllegalArgumentException e){
            //invoke throws
            rtcout.println(Logbuf.WARN, 
                   "Exception caught."+e.toString());
        }
        this.addConnectorDataListener(ConnectorDataListenerType.ON_RECEIVED,
                                     new Timestamp<DataType>("on_received",cl));
        this.addConnectorDataListener(ConnectorDataListenerType.ON_BUFFER_READ,
                                     new Timestamp<DataType>("on_read",cl));

    }
    
    /**
     * <p>コンストラクタです。
     * 読み取り・書き込みともに非ブロックモードとなり、タイムアウト時間は0で設定されます。</p>
     *
     * @param name ポート名称
     * @param value このポートにバインドされるDataType型の変数
     */
    public InPort(final String name, DataRef<DataType> value) {
        this( new RingBuffer<DataType>(8), name, value, false, false, 0, 0);
    }
    
    /**
     * <p>ポート名称を取得します。<p>
     * 
     * @return ポート名称
     */
    public String name() {
        return this.m_name;
    }
    /**
     * <p> Check whether the data is newest </p>
     * 
     * <p> Check whether the data stored at a current buffer position is newest.</p>
     *
     * @return Newest data check result
     *         ( true:Newest data. Data has not been readout yet.
     *          false:Past data Data has already been readout.)
     * 
     */
    /**
     * {@.ja 最新データが存在するか確認する}
     * {@.en Check whether the data is newest}
     * 
     * <p>
     * {@.ja InPortに未読の最新データが到着しているかをbool値で返す。
     * InPortが未接続の場合、および接続コネクタのバッファがEmpty
     * の場合にはfalseを返す。}
     * {@.en Check whether the data stored at a current buffer position is i
     * newest.}
     *
     * @return 
     *   {@.ja true 未読の最新データが存在する
     *         false 未接続またはバッファにデータが存在しない。}
     *   {@.en Newest data check result
     *         ( true:Newest data. Data has not been readout yet.
     *          false:Past data．Data has already been readout.)}
     * 
     */
    public boolean isNew() {

        rtcout.println(Logbuf.TRACE, "isNew()");
        int r = 0;

        synchronized (m_directNewDataMutex){
            if (m_directNewData == true) {
                rtcout.println(Logbuf.DEBUG, 
                              "isNew() returns true because of direct write.");
                return true;
            }
        }
        int index = 0;;
        synchronized (m_connectorsMutex){
            synchronized (m_connectors){
                if (m_connectors.size() == 0) {
                    rtcout.println(Logbuf.DEBUG, "no connectors");
                    return false;
                }
                //r = m_connectors.elementAt(0).getBuffer().readable();
                for(int ic=0;ic<m_connectors.size();++ic){
                    r = m_connectors.elementAt(ic).getBuffer().readable();
                    if (r > 0) {
                        index =ic;
                        break;
                    }
                }
            }
        }
        if (r > 0) {
            String name = m_connectors.elementAt(index).name();
            rtcout.println(Logbuf.DEBUG, 
                              "isNew() = true, connector name="+ name +
                              ", readable data: " + r);
            return true;
        }
  
        rtcout.println(Logbuf.DEBUG, "isNew() = false, no readable data");
        return false;
    }
    /**
     *
     */
    public boolean isNew(String conenctor) {
        rtcout.println(Logbuf.TRACE, "isNew()");
        synchronized (m_directNewDataMutex){
            if (m_directNewData == true) {
                rtcout.println(Logbuf.DEBUG, 
                              "isNew() returns true because of direct write.");
                return true;
            }
        }
        synchronized (m_connectorsMutex){
            synchronized (m_connectors){
                if (m_connectors.size() == 0) {
                    rtcout.println(Logbuf.DEBUG, "no connectors");
                    return false;
                }
                int r = 0;
                for(int ic=0;ic<m_connectors.size();++ic){
                    if (m_connectors.elementAt(ic).name().equals(conenctor)) {
                        r = m_connectors.elementAt(ic).getBuffer().readable();
                        if (r > 0) {
                            String name = m_connectors.elementAt(ic).name();
                            rtcout.println(Logbuf.DEBUG, 
                              "isNew() = true, connector name="+ name +
                              ", readable data: " + r);
                            return true;
                        }
                        return false;
                    }
                }
                return false;
            }
        }
    }
    /**
     *
     */
    public boolean isNew(ArrayList<String> names) {

        rtcout.println(Logbuf.TRACE, "isNew()");
        int r = 0;

        synchronized (m_directNewDataMutex){
            if (m_directNewData == true) {
                rtcout.println(Logbuf.DEBUG, 
                              "isNew() returns true because of direct write.");
                return true;
            }
        }
        boolean ret = false;
        synchronized (m_connectorsMutex){
            synchronized (m_connectors){
                if (m_connectors.size() == 0) {
                    rtcout.println(Logbuf.DEBUG, "no connectors");
                    return false;
                }
                for(int ic=0;ic<m_connectors.size();++ic){
                    r = m_connectors.elementAt(ic).getBuffer().readable();
                    if (r > 0) {
                        names.add(m_connectors.elementAt(ic).name());
                        ret = true;
                    }
                }
            }
        }
  
        return ret;
    }
    
    
    /**
     * {@.ja DataPort から値を読み出す}
     * {@.en Readout the value from DataPort}
     * <p>
     * {@.ja InPortに書き込まれたデータを読みだす。接続数が0、またはバッファに
     * データが書き込まれていない状態で読みだした場合の戻り値は不定である。
     * バッファが空の状態のとき、
     * 事前に設定されたモード (readback, do_nothing, block) に応じて、
     * 以下のような動作をする。
     *
     * - readback: 最後の値を読みなおす。
     *
     * - do_nothing: 何もしない
     *
     * - block: ブロックする。タイムアウトが設定されている場合は、
     *       タイムアウトするまで待つ。
     *
     * バッファが空の状態では、InPortにバインドされた変数の値が返される。
     * したがって、初回読み出し時には不定値を返す可能性がある。
     * この関数を利用する際には、
     *
     * - isNew(), isEmpty() と併用し、事前にバッファ状態をチェックする。
     * 
     * - 初回読み出し時に不定値を返さないようにバインド変数を事前に初期化する
     * 
     * - ReturnCode read(DataType& data) 関数の利用を検討する。
     *
     * ことが望ましい。
     *
     * 各コールバック関数は以下のように呼び出される。
     * - OnRead: read() 関数が呼ばれる際に必ず呼ばれる。
     * 
     * - OnReadConvert: データの読み出しが成功した場合、読みだしたデータを
     *       引数としてOnReadConvertが呼び出され、戻り値をread()が戻り値
     *       として返す。
     *
     * - OnEmpty: バッファが空のためデータの読み出しに失敗した場合呼び出される。
     *        OnEmpty の戻り値を read() の戻り値として返す。
     *
     * - OnBufferTimeout: データフロー型がPush型の場合に、読み出し
     *        タイムアウトのためにデータの読み出しに失敗した場合に呼ばれる。
     *
     * - OnRecvTimeout: データフロー型がPull型の場合に、読み出しタイムアウト
     *        のためにデータ読み出しに失敗した場合に呼ばれる。
     *
     * - OnReadError: 上記以外の理由で読みだしに失敗した場合に呼ばれる。
     *        理由としては、バッファ設定の不整合、例外の発生などが考えられる
     *        が通常は起こりえないためバグの可能性がある。}
     * {@.en Readout the value from DataPort
     *
     * - When Callback functor OnRead is already set, OnRead will be invoked
     *   before reading from the buffer held by DataPort.
     * - When the buffer held by DataPort can detect the underflow,
     *   and when it detected the underflow at reading, callback functor
     *   OnUnderflow will be invoked.
     * - When callback functor OnReadConvert is already set, the return value of
     *   operator() of OnReadConvert will be the return value of read().
     * - When timeout of reading is already set by setReadTimeout(),
     *   it waits for only timeout time until the state of the buffer underflow
     *   is reset, and if OnUnderflow is already set, this will be invoked to 
     *   return.}
     * </p>
     * @return 
     *   {@.ja 読み出し結果(読み出し成功:true, 読み出し失敗:false)}
     *   {@.en Readout result (Successful:true, Failed:false)}
     *
     */
    public boolean read() {
        rtcout.println(Logbuf.TRACE, "DataType read()");

        if (m_OnRead != null) {
            m_OnRead.run();
            rtcout.println(Logbuf.TRACE, "OnRead called");
        }

        // 1) direct connection
        synchronized (m_directNewDataMutex){
            if (m_directNewData == true) {
                rtcout.println(Logbuf.DEBUG, "Direct data transfer");
                m_directNewData = false;
                if (m_OnReadConvert != null) {
                    m_value.v = m_OnReadConvert.run(m_value.v);
                    rtcout.println(Logbuf.DEBUG, 
                         "OnReadConvert for direct data called");
                    return true;
                }
                return true;
            }
        }
        // 2) network connection
        synchronized (m_connectorsMutex){

//            if (m_OnRead != null) {
//                m_OnRead.run();
//                rtcout.println(Logbuf.TRACE, "OnRead called");
//            }

            ReturnCode ret = ReturnCode.PORT_OK;
            EncapsOutputStreamExt cdr = new EncapsOutputStreamExt(m_orb, 
                                                        isLittleEndian());
            //DataRef<InputStream> dataref 
            //        = new DataRef<InputStream>(cdr.create_input_stream());
            synchronized (m_connectors){

                if (m_connectors.size() == 0) {
                    rtcout.println(Logbuf.DEBUG, "no connectors");
                    return false;
                }

                //ret = m_connectors.elementAt(0).read(dataref);
                for(int ic=0;ic<m_connectors.size();++ic){
                    ret = m_connectors.elementAt(ic).read(m_value);
                    if (ret.equals(ReturnCode.PORT_OK)) {
                        break;
                    }
                }
            }

            if (ret.equals(ReturnCode.PORT_OK)) {
                rtcout.println(Logbuf.DEBUG, "data read succeeded");

                //m_value.v = read_stream(m_value,dataref.v);
                if (m_OnReadConvert != null) {
                    m_value.v = m_OnReadConvert.run(m_value.v);
                    rtcout.println(Logbuf.DEBUG, "OnReadConvert called");
                    return true;
                }
                return true;
            }
            else if (ret.equals(ReturnCode.BUFFER_EMPTY)) {
                rtcout.println(Logbuf.WARN, "buffer empty");
                return false;
            }
            else if (ret.equals(ReturnCode.BUFFER_TIMEOUT)) {
                rtcout.println(Logbuf.WARN, "buffer read timeout");
                return false;
            }
            rtcout.println(Logbuf.ERROR, 
                           "unknown retern value from buffer.read()");
            return false;

        }
    }
    /**
     *
     */
    public boolean read(String name) {
        rtcout.println(Logbuf.TRACE, "DataType read()");

        if (m_OnRead != null) {
            m_OnRead.run();
            rtcout.println(Logbuf.TRACE, "OnRead called");
        }

        // 1) direct connection
        synchronized (m_directNewDataMutex){
            if (m_directNewData == true) {
                rtcout.println(Logbuf.DEBUG, "Direct data transfer");
                m_directNewData = false;
                if (m_OnReadConvert != null) {
                    m_value.v = m_OnReadConvert.run(m_value.v);
                    rtcout.println(Logbuf.DEBUG, 
                         "OnReadConvert for direct data called");
                    return true;
                }
                return true;
            }
        }
        // 2) network connection
        synchronized (m_connectorsMutex){
            ReturnCode ret = ReturnCode.PORT_OK;
            EncapsOutputStreamExt cdr = new EncapsOutputStreamExt(m_orb, 
                                                        isLittleEndian());
            DataRef<InputStream> dataref 
                    = new DataRef<InputStream>(cdr.create_input_stream());
            synchronized (m_connectors){

                if (m_connectors.size() == 0) {
                    rtcout.println(Logbuf.DEBUG, "no connectors");
                    return false;
                }

                for(int ic=0;ic<m_connectors.size();++ic){
                    if (m_connectors.elementAt(ic).name().equals(name)) {
                        ret = m_connectors.elementAt(ic).read(dataref);
                    }
                }
            }

            if (ret.equals(ReturnCode.PORT_OK)) {
                rtcout.println(Logbuf.DEBUG, "data read succeeded");

                m_value.v = read_stream(m_value,dataref.v);
                if (m_OnReadConvert != null) {
                    m_value.v = m_OnReadConvert.run(m_value.v);
                    rtcout.println(Logbuf.DEBUG, "OnReadConvert called");
                    return true;
                }
                return true;
            }
            else if (ret.equals(ReturnCode.BUFFER_EMPTY)) {
                rtcout.println(Logbuf.WARN, "buffer empty");
                return false;
            }
            else if (ret.equals(ReturnCode.BUFFER_TIMEOUT)) {
                rtcout.println(Logbuf.WARN, "buffer read timeout");
                return false;
            }
            rtcout.println(Logbuf.ERROR, 
                           "unknown retern value from buffer.read()");
            return false;

        }
    }
    
    /**
     * <p>バインドされたDataType型のデータに、ポート内バッファの最新値を読み込みます。<br />
     * ただし、あらかじめコンストラクタで、DataType型の変数がバインドされていなければなりません。<br />
     */
    public void update() {
        this.read();
    }

    /**
     * {@.ja T 型のデータへ InPort の最新値データを読み込む}
     * {@.en Read the newly value data in InPort to type-T variable}
     * <p>
     * {@.ja InPort に設定されている最新データを読み込み、
     *       指定されたデータ変数に設定する。}
     * {@.en Read the newly data set in InPort and set to specified data 
     *       variable.}
     * </p>
     * @return
     *   {@.ja InPort バッファから値を読み込む T 型変数}
     *   {@.en The type-T variable to read from InPort's buffer}
     */
    public DataType extract() {
        this.read();
        return m_value.v;
    }
    
    /**
     * <p>ポートが持つバッファにデータが書き込まれる直前に呼び出される
     * コールバックインタフェースを設定します。</p>
     * 
     * @param onWrite OnWrite&lt;DataType&gt;インタフェースを持つオブジェクト
     */
    public void setOnWrite(OnWrite<DataType> onWrite) {
        this.m_OnWrite = onWrite;
    }
    
    /**
     * <p>ポートが持つバッファにデータが書き込まれる際に呼び出される
     * コールバックインタフェースを設定します。<br />
     * コールバックインタフェースの戻り値がバッファに書き込まれます。</p>
     * 
     * @param onWriteConvert OnWriteConvert&lt;DataType&gt;インタフェースを持つオブジェクト
     */
    public void setOnWriteConvert(OnWriteConvert<DataType> onWriteConvert) {
        this.m_OnWriteConvert = onWriteConvert;
    }
    
    /**
     * <p>ポートが持つバッファからデータが読み込まれる直前に呼び出される
     * コールバックインタフェースを設定します。</p>
     * 
     * @param onRead OnRead&lt;DataType&gt;インタフェースを持つオブジェクト
     */
    public void setOnRead(OnRead<DataType> onRead) {
        this.m_OnRead = onRead;
    }
    
    /**
     * <p>ポートが持つバッファからデータが読み出される際に呼び出される
     * コールバックインタフェースを設定します。<br />
     * コールバックインタフェースの戻り値がread()メソッドの読み出し結果として取得されます。</p>
     * 
     * @param onReadConvert OnReadConvert&lt;DataType&gt;インタフェースを持つオブジェクト
     */
    public void setOnReadConvert(OnReadConvert<DataType> onReadConvert) {
        this.m_OnReadConvert = onReadConvert;
    }
    
    /**
     * <p>データ書き込み時に、バッファオーバーフロー検出の際に呼び出される
     * コールバックインタフェースを設定します。</p>
     * 
     * @param onOverflow OnOverflow&lt;DataType&gt;インタフェースを持つオブジェクト
     */
    public void setOnOverflow(OnOverflow<DataType> onOverflow) {
        this.m_OnOverflow = onOverflow;
    }
    
    /**
     * <p>データ読み込みときに、バッファアンダーフロー検出の際に呼び出される
     * コールバックインタフェースを設定します。</p>
     * 
     * @param onUnderflow OnUnderflow&lt;DataType&gt;インタフェースを持つオブジェクト
     */
    public void setOnUnderflow(OnUnderflow<DataType> onUnderflow) {
        this.m_OnUnderflow = onUnderflow;
    }
    

    /**
     * {@.ja バッファが空かどうか確認する}
     * {@.en Check whether the data is newest}
     *
     * <p> 
     * {@.ja InPortのバッファが空かどうかを bool 値で返す。
     * 空の場合は true, 未読データがある場合は false を返す。}
     * {@.en Check whether the data stored at a current buffer position is 
     * newest.}
     *
     * @return 
     *   {@.ja true  バッファは空
     *         false バッファに未読データがある}
     *   {@.en Newest data check result
     *         ( true:Newest data. Data has not been readout yet.
     *          false:Past data．Data has already been readout.)}
     * 
     *
     */
    public boolean isEmpty() {
        rtcout.println(Logbuf.TRACE, "isEmpty()");

        synchronized (m_directNewDataMutex){
            if (m_directNewData == true) { 
                return false; 
            }
        }

        int r = 0;
        synchronized (m_connectorsMutex){
            synchronized (m_connectors){
                if (m_connectors.size() == 0) {
                    rtcout.println(Logbuf.DEBUG, "no connectors");
                    return true;
                }
                r = m_connectors.elementAt(0).getBuffer().readable();
            }
        }
        if (r == 0) {
            rtcout.println(Logbuf.DEBUG, 
                           "isEmpty() = true, buffer is empty");
            return true;
        }
          
        rtcout.println(Logbuf.DEBUG, 
                   "isEmpty() = false, data exists in the buffer");
        return false;
    }
    /**
     * {@.ja データ書き込み}
     * {@.en Write data}
     * @param data 
     *   {@.ja 書き込み対象データ}
     *   {@.en The target data for writing}
     *
     */
    public void write(final DataRef<DataType> data)
    {
        rtcout.println(Logbuf.TRACE, "write()");
        synchronized (m_directNewDataMutex){
            m_value.v = data.v;
            m_directNewData = true;
        }
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

    private BufferBase<DataType> m_superClass;
    private String m_name;
    private DataRef<DataType> m_value;
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

    private Streamable m_streamable = null;
    private Field m_field = null;
    
    private ORB m_orb;

    private boolean m_directNewData;
    private static String m_directNewDataMutex = new String();

}
