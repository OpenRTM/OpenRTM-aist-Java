package jp.go.aist.rtm.RTC.port;

import jp.go.aist.rtm.RTC.BufferFactory;
import jp.go.aist.rtm.RTC.InPortProviderFactory;
import jp.go.aist.rtm.RTC.SerializerFactory;
import jp.go.aist.rtm.RTC.buffer.BufferBase;
import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.util.DataRef;

import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class InPortPushConnector extends InPortConnector {
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     *
     * <p>
     * {@.ja InPortPushConnector のコンストラクタはオブジェクト生成時に下記を
     * 引数にとる。ConnectorInfo は接続情報を含み、この情報に従いバッファ
     * 等を生成する。InPort インターフェースのプロバイダオブジェクトへ
     * のポインタを取り、所有権を持つので、InPortPushConnector は
     * InPortProvider の解体責任を持つ。各種イベントに対するコールバッ
     * ク機構を提供する ConnectorListeners を持ち、適切なタイミングでコー
     * ルバックを呼び出す。データバッファがもし InPortBase から提供され
     * る場合はそのポインタを取る。}
     * {@.en InPortPushConnector's constructor is given the following
     * arguments.  According to ConnectorInfo which includes
     * connection information, a buffer is created.
     * It is also given a pointer to the provider object for the
     * InPort interface.  The owner-ship of the pointer is owned by
     * this InPortPushConnector, it has responsibility to destruct
     * the InPortProvider.  InPortPushConnector also has
     * ConnectorListeners to provide event callback mechanisms, and
     * they would be called at the proper timing.  If data buffer is
     * given by InPortBase, the pointer to the buffer is also given
     * as arguments.}
     * </p>
     *
     * @param profile 
     *   {@.ja ConnectorInfo}
     *   {@.en ConnectorInfo}
     * @param provider 
     *   {@.ja InPortProvider}
     *   {@.en InPortProvider}
     * @param listeners 
     *   {@.ja ConnectorListeners 型のリスナオブジェクトリスト}
     *   {@.en ConnectorListeners type lsitener object list}
     * @param buffer 
     *   {@.ja CdrBufferBase 型のバッファ}
     *   {@.en CdrBufferBase type buffer}
     */
    public InPortPushConnector(ConnectorInfo profile, InPortProvider provider,
                        ConnectorListeners listeners,
                        BufferBase<OutputStream> buffer) throws Exception {
        super(profile, listeners, buffer);
        m_provider = provider;
        m_listeners = listeners; 
        if (buffer == null ) {
            m_deleteBuffer = true;
        }
        else {
            m_deleteBuffer = false;
        }

        if (m_provider==null) {
            rtcout.println(Logbuf.PARANOID, "    m_provider is null.");
            throw new Exception("bad_alloc()");
        }
        m_provider.init(profile.properties);

        // publisher/buffer creation. This may throw std::bad_alloc;
        if (m_buffer == null) {
            m_buffer = createBuffer(profile);
        }
        if (m_buffer == null) {
            rtcout.println(Logbuf.PARANOID, "    m_buffer is null.");
            throw new Exception("bad_alloc()");
        }

        m_buffer.init(profile.properties.getNode("buffer"));
        m_provider.setBuffer(m_buffer);
        m_provider.setListener(profile, m_listeners);

        onConnect();

        String marshaling_type = profile.properties.getProperty(
            "marshaling_type", "corba");
        marshaling_type = profile.properties.getProperty(
            "in.marshaling_type",marshaling_type);
        m_marshaling_type = marshaling_type.trim();

        final SerializerFactory<CORBA_CdrSerializer,String> factory 
            = SerializerFactory.instance();
        m_serializer = factory.createObject(m_marshaling_type);
    }
    public void setListener(ConnectorInfo profile, 
                            ConnectorListeners listeners){
        m_provider.setListener(profile, m_listeners);
    }

    /**
     * {@.ja データの読み出し}
     * {@.en Reading data}
     *
     * <p>
     * {@.ja バッファからデータを読み出す。正常に読み出せた場合、戻り値は
     * PORT_OK となり、data に読み出されたデータが格納される。それ以外
     * の場合には、エラー値として BUFFER_EMPTY, TIMEOUT,
     * PRECONDITION_NOT_MET, PORT_ERROR が返される。}
     * {@.en This function reads data from the buffer. If data is read
     * properly, this function will return PORT_OK return code. Except
     * normal return, BUFFER_EMPTY, TIMEOUT, PRECONDITION_NOT_MET and
     * PORT_ERROR will be returned as error codes.}
     * </p>
     *
     * @return 
     *   {@.ja PORT_OK              正常終了
     *         BUFFER_EMPTY         バッファは空である
     *         TIMEOUT              タイムアウトした
     *         PRECONDITION_NOT_MET 事前条件を満たさない
     *         PORT_ERROR           その他のエラー}
     *
     *   {@.en PORT_OK              Normal return
     *         BUFFER_EMPTY         Buffer empty
     *         TIMEOUT              Timeout
     *         PRECONDITION_NOT_MET Preconditin not met
     *         PORT_ERROR           Other error}
     *
     */
    public <DataType> ReturnCode read(DataRef<DataType> data) {
        rtcout.println(Logbuf.TRACE, "read()");
        if (m_directOutPort != null) {
            OutPort outport = (OutPort)m_directOutPort;
            //DataRef<DataType> dataref 
            //        = new DataRef<DataType>(data);
            outport.read(data); 
            // ON_RECEIVED(In,Out) callback
            return ReturnCode.PORT_OK;

        }
        /*
         * buffer returns
         *   BUFFER_OK
         *   BUFFER_EMPTY
         *   TIMEOUT
         *   PRECONDITION_NOT_MET
         */
        if (m_buffer == null) {
            return ReturnCode.PRECONDITION_NOT_MET;
        }
        
        org.omg.CORBA.Any any = m_orb.create_any(); 
        OutputStream cdr = any.create_output_stream();
        DataRef<OutputStream> dataref = new DataRef<OutputStream>(cdr);
        jp.go.aist.rtm.RTC.buffer.ReturnCode ret 
                                        = m_buffer.read(dataref, -1, -1);
        //data.v = dataref.v.create_input_stream();
        //return convertReturn(ret,dataref.v);
/*
        ReturnCode code = convertReturn(ret,dataref);
        data.v = dataref.v.create_input_stream();
        return code;
*/
        if(ret != jp.go.aist.rtm.RTC.buffer.ReturnCode.BUFFER_OK){
            ReturnCode code = convertReturn(ret,dataref);
            //data.v = dataref.v.create_input_stream();
            return code;
        }
        else {
            onBufferRead(dataref);
            if(m_serializer == null){
                rtcout.println(Logbuf.ERROR, "serializer creation failure.");
                return ReturnCode.UNKNOWN_ERROR;
            }
            m_serializer.isLittleEndian(m_isLittleEndian);
            //InputStream in_cdr = cdr.create_input_stream();
            //SerializeReturnCode ser_ret = m_serializer.deserialize(dataref,in_cdr);

            SerializeReturnCode ser_ret = m_serializer.deserialize(data,dataref.v);

            if(ser_ret.equals(SerializeReturnCode.SERIALIZE_OK)){
                //data = _data;
                return ReturnCode.PORT_OK;
            }
	    else if(ser_ret.equals(SerializeReturnCode.SERIALIZE_NOT_SUPPORT_ENDIAN)){
                rtcout.println(Logbuf.ERROR, "unknown endian from connector");
                return ReturnCode.UNKNOWN_ERROR;
            }
	    else if(ser_ret.equals(SerializeReturnCode.SERIALIZE_ERROR)){
                rtcout.println(Logbuf.ERROR, "unknown error");
                return ReturnCode.UNKNOWN_ERROR;
            }
	    else if(ser_ret.equals(SerializeReturnCode.SERIALIZE_NOTFOUND)){
                rtcout.println(Logbuf.ERROR, "unknown serializer from connector");
                return ReturnCode.UNKNOWN_ERROR;
            }
        }
        return ReturnCode.PORT_ERROR;
    }

    /**
     * {@.ja 接続解除}
     * {@.en disconnect}
     *
     * <p>
     * {@.ja consumer, publisher, buffer が解体・削除される。}
     * {@.en This operation destruct and delete the consumer, the publisher
     * and the buffer.}
     */
    public ReturnCode disconnect() {
        rtcout.println(Logbuf.TRACE, "disconnect()");
        onDisconnect();
        // delete provider 
        if (m_provider != null) {
            InPortProviderFactory<InPortProvider,String> cfactory 
                = InPortProviderFactory.instance();
            cfactory.deleteObject(m_provider);
        }
        m_provider = null;

        // delete buffer
        if (m_buffer != null && m_deleteBuffer == true) {
            BufferFactory<BufferBase<OutputStream>,String> bfactory 
                = BufferFactory.instance();
            bfactory.deleteObject(m_buffer);
        }
        m_buffer = null;

        m_serializer = null;

        return ReturnCode.PORT_OK;
    }

    /**
     * {@.ja アクティブ化}
     * {@.en Connector activation}
     * <p>
     * {@.ja このコネクタをアクティブ化する}
     * {@.en This operation activates this connector}
     */
    public  void activate(){}; // do nothing

    /**
     * {@.ja 非アクティブ化}
     * {@.en Connector deactivation}
     * <p>
     * {@.ja このコネクタを非アクティブ化する}
     * {@.en This operation deactivates this connector}
     */
    public void deactivate(){}; // do nothing

    /**
     * {@.ja Bufferの生成}
     * {@.en create buffer}
     * <p>
     * {@.ja 与えられた接続情報に基づきバッファを生成する。}
     * {@.en This function creates a buffer based on given information.}
     *
     * @param profile 
     *   {@.ja 接続情報}
     *   {@.en Connector information}
     * @return 
     *   {@.ja バッファへのポインタ}
     *   {@.en The poitner to the buffer}
     */
    protected BufferBase<OutputStream> createBuffer(ConnectorInfo profile) {
        String buf_type;
        buf_type = profile.properties.getProperty("buffer_type",
                                              "ring_buffer");
        BufferFactory<BufferBase<OutputStream>,String> factory 
                = BufferFactory.instance();
        return factory.createObject(buf_type);
    }

    /**
     * {@.ja 接続確立時にコールバックを呼ぶ}
     * {@.en Invoke callback when connection is established}
     */
    protected void onConnect() {
        m_listeners.connector_[ConnectorListenerType.ON_CONNECT].notify(m_profile);
    }

    /**
     * {@.ja 接続切断時にコールバックを呼ぶ}
     * {@.en Invoke callback when connection is destroied}
     */
    protected void onDisconnect() {
        m_listeners.connector_[ConnectorListenerType.ON_DISCONNECT].notify(
                                                                    m_profile);
    }

    protected void onBufferRead(DataRef<OutputStream> data) {
        m_listeners.connectorData_[ConnectorDataListenerType.ON_BUFFER_READ].notify(m_profile, data);
    }

    protected void onBufferEmpty() {
      m_listeners.connector_[ConnectorListenerType.ON_BUFFER_EMPTY].notify(m_profile);
    }

    protected void onBufferReadTimeout(){
      m_listeners.connector_[ConnectorListenerType.ON_BUFFER_READ_TIMEOUT].notify(m_profile);
    }

    /**
     * {@.ja buffer.ReturnCodeをport.ReturnCodeに変換する。}
     * {@.en Converts buffer.ReturnCode into port.ReturnCode.}
     * 
     * @param status
     *   {@.ja jp.go.aist.rtm.RTC.buffer.ReturnCode}
     *   {@.en jp.go.aist.rtm.RTC.buffer.ReturnCode}
     * @return
     *   {@.ja jp.go.aist.rtm.RTC.port.ReturnCode}
     *   {@.en jp.go.aist.rtm.RTC.port.ReturnCode}
     */
    protected ReturnCode convertReturn(jp.go.aist.rtm.RTC.buffer.ReturnCode status, DataRef<OutputStream> data) {
        switch (status) {
            case BUFFER_OK:
                onBufferRead(data);
                return ReturnCode.PORT_OK;
            case BUFFER_EMPTY:
                onBufferEmpty();
                return ReturnCode.BUFFER_EMPTY;
            case TIMEOUT:
                onBufferReadTimeout();
                return ReturnCode.BUFFER_TIMEOUT;
            case PRECONDITION_NOT_MET:
                return ReturnCode.PRECONDITION_NOT_MET;
            default:
                return ReturnCode.PORT_ERROR;
        }
    }
    /**
     * <p> the pointer to the InPortConsumer </p>
     */
    private InPortProvider m_provider;

    private boolean m_deleteBuffer;

    /**
     * <p> A reference to a ConnectorListener </p>
     */
    private ConnectorListeners m_listeners;

    private String m_marshaling_type;
    private CORBA_CdrSerializer m_serializer;
}
