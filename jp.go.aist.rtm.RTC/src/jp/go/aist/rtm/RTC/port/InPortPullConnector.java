package jp.go.aist.rtm.RTC.port;

import jp.go.aist.rtm.RTC.BufferFactory;
import jp.go.aist.rtm.RTC.buffer.BufferBase;
import jp.go.aist.rtm.RTC.util.DataRef;
import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.OutPortConsumerFactory;
import jp.go.aist.rtm.RTC.SerializerFactory;

import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;


/**
 * {@.ja InPortPullConnector クラス}
 * {@.en InPortPullConnector class}
 * <p>
 * {@.ja InPort の pull 型データフローのための Connector クラス。このオブ
 * ジェクトは、接続時に dataflow_type に pull が指定された場合、
 * InPort によって生成・所有され、OutPortPullConnector と対になって、
 * データポートの pull 型のデータフローを実現する。一つの接続に対して、
 * 一つのデータストリームを提供する唯一の Connector が対応する。
 * Connector は 接続時に生成される UUID 形式の ID により区別される。
 *
 * InPortPullConnector は以下の三つのオブジェクトを所有し管理する。
 * <ul>
 * <li>- InPortConsumer
 * <li>- Buffer
 * </ul>
 * OutPort に書き込まれたデータは OutPortPullConnector::write() に渡
 * され Buffer に書き込まれる。InPort::read(),
 * InPortPullConnector::read() は結果として、OutPortConsumer::get()
 * を呼び出し、OutPortPullConnector の持つバッファからデータを読み出
 * し、InPortPullConnector のもつバッファにデータを書き込む。}
 * {@.en Connector class of InPort for pull type dataflow. When "pull" is
 * specified as dataflow_type at the time of establishing
 * connection, this object is generated and owned by the InPort.
 * This connector and InPortPullConnector make a pair and realize
 * pull type dataflow of data ports. One connector corresponds to
 * one connection which provides a data stream. Connector is
 * distinguished by ID of the UUID that is generated at establishing
 * connection.
 *
 * InPortPullConnector owns and manages the following objects.
 * <ul>
 * <li>- InPortConsumer
 * <li>- Buffer
 * </ul>
 * Data written into the OutPort is passed to the
 * OutPortPullConnector::write(), and is written into the buffer.
 * Consequently, InPort::read() and InPortPullConnector::read() call
 * OutPortConsumer::get(), and it reads data from the buffer of
 * OutPortPullConnector.  Finally data would be written into the
 * InPortPullConnector's buffer.}
 *
 *
 */
public class InPortPullConnector extends InPortConnector {
    /**
     * {@.ja コンストラクタ.}
     * {@.en Constructor.}
     *
     * <p>
     * {@.ja InPortPullConnector のコンストラクタはオブジェクト生成時に下記
     * を引数にとる。ConnectorInfo は接続情報を含み、この情報に従いバッ
     * ファ等を生成する。OutPort インターフェースのプロバイダオブジェク
     * トへのポインタを取り、所有権を持つので、InPortPullConnector は
     * OutPortConsumer の解体責任を持つ。各種イベントに対するコールバッ
     * ク機構を提供する ConnectorListeners を持ち、適切なタイミングでコー
     * ルバックを呼び出す。データバッファがもし InPortBase から提供さ
     * れる場合はそのポインタを取る。}
     * {@.en InPortPullConnector's constructor is given the following
     * arguments.  According to ConnectorInfo which includes
     * connection information, a buffer is created.  It is also given
     * a pointer to the consumer object for the OutPort interface.
     * The owner-ship of the pointer is owned by this
     * OutPortPullConnector, it has responsibility to destruct the
     * OutPortConsumer.  OutPortPullConnector also has
     * ConnectorListeners to provide event callback mechanisms, and
     * they would be called at the proper timing.  If data buffer is
     * given by OutPortBase, the pointer to the buffer is also given
     * as arguments.}
     * </p>
     *
     * @param profile 
     *   {@.ja ConnectorInfo}
     *   {@.en ConnectorInfo}
     * @param consumer 
     *   {@.ja OutPortConsumer}
     *   {@.en consumer OutPortConsumer}
     * @param listeners 
     *   {@.ja ConnectorListeners 型のリスナオブジェクトリスト}
     *   {@.en ConnectorListeners type lsitener object list}
     * @param buffer 
     *   {@.ja CdrBufferBase 型のバッファ}
     *   {@.en CdrBufferBase type buffer}
     */
    public InPortPullConnector(ConnectorInfo profile,
                        OutPortConsumer consumer,
                        ConnectorListeners listeners,
                        BufferBase<OutputStream> buffer) throws Exception {
        super(profile, listeners, buffer);
        m_consumer = consumer;
        m_listeners = listeners; 
        rtcout = new Logbuf("InPortPullConnector");

        if (m_consumer == null) {
            throw new Exception("bad_alloc()");
        }
        m_consumer.init(profile.properties);
        
        if (buffer == null) {
            m_buffer = createBuffer(m_profile);
        }
        if (m_buffer == null) {
            throw new Exception("bad_alloc()");
        }
        m_buffer.init(profile.properties.getNode("buffer"));
        m_consumer.setBuffer(m_buffer);
        m_consumer.setListener(profile, m_listeners);

        onConnect();

        String marshaling_type = profile.properties.getProperty(
            "marshaling_type", "corba");
        marshaling_type = profile.properties.getProperty(
            "in.marshaling_type", marshaling_type);
        m_marshaling_type = marshaling_type.trim();

        //m_serializer = SerializerFactory.instance().createObject(m_marshaling_type);
        final SerializerFactory<CORBA_CdrSerializer,String> factory 
            = SerializerFactory.instance();
        m_serializer = factory.createObject(m_marshaling_type);

    }
    /**
     * {@.ja read 関数}
     * {@.en Reading data}
     * <p>
     * {@.ja OutPortConsumer からデータを取得する。正常に読み出せた場合、戻り
     * 値は PORT_OK となり、data に読み出されたデータが格納される。それ
     * 以外の場合には、エラー値として BUFFER_EMPTY, TIMEOUT,
     * PRECONDITION_NOT_MET, PORT_ERROR が返される。}
     * {@.en This function get data from OutPortConsumer.  If data is read
     * properly, this function will return PORT_OK return code. Except
     * normal return, BUFFER_EMPTY, TIMEOUT, PRECONDITION_NOT_MET and
     * PORT_ERROR will be returned as error codes.}
     *
     * @return 
     *   {@.ja PORT_OK              正常終了
     *         BUFFER_EMPTY         バッファは空である
     *         TIMEOUT              タイムアウトした
     *         PRECONDITION_NOT_MET 事前条件を満たさない
     *         PORT_ERROR           その他のエラー}
     *   {@.en PORT_OK              Normal return
     *         BUFFER_EMPTY         Buffer empty
     *         TIMEOUT              Timeout
     *         PRECONDITION_NOT_MET Preconditin not met
     *         PORT_ERROR           Other error}
     */
    public <DataType> ReturnCode read(DataRef<DataType> data){
        rtcout.println(Logbuf.TRACE, "InPortPullConnector.read()");
        if (m_directOutPort != null) {
            return ReturnCode.BUFFER_EMPTY;
            //OutPort outport = (OutPort)m_directOutPort;
            //outport.read(data); 
            //return ReturnCode.PORT_OK;

        }
        if (m_consumer == null) {
            return ReturnCode.PORT_ERROR;
        }
        EncapsOutputStreamExt cdr = new EncapsOutputStreamExt(m_orb, 
                                                    m_isLittleEndian);
        ReturnCode ret = m_consumer.get(cdr);
        if(ret==ReturnCode.PORT_OK){
            if(m_serializer==null){
                rtcout.println(Logbuf.TRACE, "serializer creation failure.");
                return ReturnCode.UNKNOWN_ERROR;
            }
            m_serializer.isLittleEndian(m_isLittleEndian);
            //InputStream in_cdr = cdr.create_input_stream();
            //SerializeReturnCode ser_ret = m_serializer.deserialize(data,in_cdr);
            SerializeReturnCode ser_ret = m_serializer.deserialize(data,cdr);

            //if(ser_ret == SERIALIZE_NOT_SUPPORT_ENDIAN){
            if(ser_ret.equals(SerializeReturnCode.SERIALIZE_NOT_SUPPORT_ENDIAN)){
                rtcout.println(Logbuf.TRACE, "unknown endian from connector");
                return ReturnCode.UNKNOWN_ERROR;
            }
	    else if(ser_ret.equals(SerializeReturnCode.SERIALIZE_ERROR)){
                rtcout.println(Logbuf.TRACE, "unknown error");
                return ReturnCode.UNKNOWN_ERROR;
            }
	    else if(ser_ret.equals(SerializeReturnCode.SERIALIZE_NOTFOUND)){
                rtcout.println(Logbuf.TRACE, "unknown serializer from connector");
                return ReturnCode.UNKNOWN_ERROR;
            }
        }
        //DataRef<OutputStream> dataref = new DataRef<OutputStream>(cdr);
        //data.v = dataref.v.create_input_stream();
        return ret;
    }
    /**
     * {@.ja 接続解除関数}
     * {@.en Disconnect connection}
     *
     * <p>
     * {@.ja Connector が保持している接続を解除する}
     * {@.en This operation disconnect this connection}
     * </p>
     */
    public ReturnCode disconnect() {
        rtcout.println(Logbuf.TRACE, "disconnect()");
        onDisconnect();
        // delete consumer
        if (m_consumer != null) {
            rtcout.println(Logbuf.DEBUG, "delete consumer");
            OutPortConsumerFactory<OutPortConsumer,String> cfactory 
                = OutPortConsumerFactory.instance();
            cfactory.deleteObject(m_consumer);
        }
        m_consumer = null;
        m_serializer = null;

        return ReturnCode.PORT_OK;
    }

    /**
     * {@.ja アクティブ化}
     * {@.en Connector activation}
     * <p>
     * {@.ja このコネクタをアクティブ化する}
     * {@.en This operation activates this connector}
     *
     */
    public void activate(){}; // do nothing

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

    /**
     * {@.ja リスナを設定する。}
     * {@.en Set the listener.}
     * 
     * <p>
     * {@.ja InPort はデータ送信処理における各種イベントに対して特定のリスナ
     * オブジェクトをコールするコールバック機構を提供する。詳細は
     * ConnectorListener.h の ConnectorDataListener, ConnectorListener
     * 等を参照のこと。InPortCorbaCdrProvider では、以下のコールバック
     * が提供される。}
     * {@.en InPort provides callback functionality that calls specific
     * listener objects according to the events in the data publishing
     * process. For details, see documentation of
     * ConnectorDataListener class and ConnectorListener class in
     * ConnectorListener.h. In this InPortCorbaCdrProvider provides
     * the following callbacks.}
     * 
     *
     * @param profile 
     *   {@.ja 接続情報}
     *   {@.en Connector information}
     * @param listeners 
     *   {@.ja リスナオブジェクト}
     *   {@.en Listener objects}
     */
    public void setListener(ConnectorInfo profile, 
                            ConnectorListeners listeners){
    }
    private OutPortConsumer m_consumer;

    private Logbuf rtcout;
    /**
     * A reference to a ConnectorListener
     */
    private ConnectorListeners m_listeners;

    private String m_marshaling_type;
    private CORBA_CdrSerializer m_serializer;
}
