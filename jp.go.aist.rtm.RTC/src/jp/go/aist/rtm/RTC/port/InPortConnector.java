package jp.go.aist.rtm.RTC.port;

import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

import jp.go.aist.rtm.RTC.buffer.BufferBase;
import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.util.DataRef;
import jp.go.aist.rtm.RTC.util.ORBUtil;

/**
 * {@.ja InPortConnector 基底クラス}
 * {@.en InPortConnector base class}
 * <p>
 * {@.ja InPort の Push/Pull 各種 Connector を派生させるための基底クラス。}
 * {@.en The base class to derive subclasses for InPort's Push/Pull Connectors}
 */
public abstract class InPortConnector extends ConnectorBase {
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     *
     * @param profile 
     *   {@.ja 接続情報を含む ConnectorInfo オブジェクト}
     *   {@.en ConnectorInfo object which includes connection information}
     * @param buffer 
     *   {@.ja このコネクタのバッファへのポインタ}
     *   {@.en A pointer to the buffer of the connector}
     */
    public InPortConnector(ConnectorBase.ConnectorInfo profile,
                    ConnectorListeners listeners,
                    BufferBase<OutputStream> buffer) {
        rtcout = new Logbuf("InPortConnector");
        m_profile = profile;
        m_buffer = buffer;
        m_isLittleEndian = true;
        m_orb = ORBUtil.getOrb();
        m_listeners = listeners;
        m_directOutPort = null;
    }


    /**
     * {@.ja ConnectorInfo 取得}
     * {@.en Getting ConnectorInfo}
     * <p>
     * {@.ja Connector ConnectorInfo を取得する}
     * {@.en This operation returns ConnectorInfo}
     *
     * @return 
     *   {@.ja このコネクタが保持する ConnectorInfo オブジェクト}
     *   {@.en ConnectorInfo object which is owned by this connector}
     */
    public final ConnectorInfo profile() {
        rtcout.println(Logbuf.TRACE, "profile()");
        return m_profile;
    }
    /**
     * {@.ja Connector ID 取得}
     * {@.en Getting Connector ID}
     * <p>
     * {@.ja Connector ID を取得する}
     * {@.en This operation returns Connector ID}
     *
     * @return 
     *   {@.ja コネクタ ID 文字列へのポインタ}
     *   {@.en A pointer to the connector id string}
     */
    public final String id() {
        rtcout.println(Logbuf.TRACE, "id() = "+profile().id);
        return profile().id;
    }

    /**
     * {@.ja Connector 名取得}
     * {@.en Getting Connector name}
     * <p>
     * {@.ja Connector 名を取得する}
     * {@.en This operation returns Connector name}
     *
     * @return 
     *   {@.ja コネクタ名文字列へのポインタ}
     *   {@.en A pointer to the connector's name string}
     */
    public final String name() {
        rtcout.println(Logbuf.TRACE, "name() = "+profile().name);
        return profile().name;
    }

    /**
     * {@.ja 接続解除関数}
     * {@.en Disconnect connection}
     * <p>
     * {@.ja Connector が保持している接続を解除する}
     * {@.en This operation disconnect this connection}
     *
     * @return 
     *   {@.ja ReturnCode}
     *   {@.en ReturnCode}
     */
    public abstract ReturnCode disconnect();

    /**
     * {@.ja Buffer を取得する}
     * {@.en Getting Buffer}
     * <p>
     * {@.ja Connector が保持している Buffer を返す}
     * {@.en This operation returns this connector's buffer}
     *
     * @return 
     *   {@.ja このコネクタが保持するバッファへのポインタ}
     *   {@.en A pointer to the buffer owned by this connector}
     *
     */
    public BufferBase<OutputStream> getBuffer() {
        return m_buffer;
    }
    /**
     * {@.ja endianタイプ設定}
     * {@.en Setting an endian type}
     * <p>
     * {@.ja  endianタイプを設定する}
     * {@.en This operation set this connector's endian type}
     *
     * @param isLittleEndian
     *   {@.ja true: little, false: big}
     *   {@.en true: little, false: big}
     */
    public void setEndian(boolean isLittleEndian){
        m_isLittleEndian = isLittleEndian;
    }
    /**
     * {@.ja endian 設定を返す}
     * {@.en Returns endian}
     * <p> 
     * {@.en This value is true if the architecture is little-endian; 
     *  false if it is big-endian.}
     *
     * @return 
     *   {@.ja m_littleEndian がlittleの場合true、bigの場合false を返す。}
     *   {@.en Returns the endian setting.}
     * 
     */
    public boolean isLittleEndian(){
        return m_isLittleEndian;
    }
    /**
     * {@.ja リスナを設定する。}
     * {@.en Set the listener.}
     *
     * @param profile 
     *   {@.ja 接続情報}
     *   {@.en Connector information}
     * @param listeners 
     *   {@.ja リスナオブジェクト}
     *   {@.en Listener objects}
     */
    public abstract void setListener(ConnectorInfo profile, 
                            ConnectorListeners listeners);
    

    /**
     *
     * {@.ja read 関数}
     * {@.en Reading data}
     *
     */
    public abstract <DataType> ReturnCode read(DataRef<DataType> data);
    /**
     * {@.ja OutPortのオブジェクトを設定する。}
     * {@.en Sets the object of OutPort}
     * <p>
     * {@.ja ダイレクトポートのためにOutPortrBaseのオブジェクトを設定する。}
     * {@.en Set an object of OutPortBase for a direct port.}
     * @param directOutPort
     *   {@.ja OutPortBase オブジェクト}
     *   {@.en OutPortBase object}
     *
     */
    public boolean setOutPort(OutPortBase directOutPort){
        rtcout.println(Logbuf.TRACE, "setOutPort()");
        if (directOutPort == null) {
            rtcout.println(Logbuf.TRACE, "OutPortBase is null.");
            return false;
        }

        m_directOutPort = directOutPort;
        m_outPortListeners = m_directOutPort.getListeners();

        return true;
    }

    protected Logbuf rtcout;
    protected ConnectorInfo m_profile;
    protected BufferBase<OutputStream> m_buffer;
    protected boolean m_isLittleEndian;
    protected org.omg.CORBA.ORB m_orb;
    /**
     * {@.ja ConnectorListenrs への参照}
     * {@.en A reference to a ConnectorListener}
     */
    ConnectorListeners m_listeners;
    /**
     * {@.ja 同一プロセス上のピアOutPortのポインタ}
     * {@.en OutProt pointer to the peer in the same process}
     */
    protected OutPortBase m_directOutPort;
    /**
     * {@.ja OutPort 側の ConnectorListenrs への参照}
     * {@.en A pointer to a OutPort's ConnectorListener}
     */
    protected ConnectorListeners m_outPortListeners = new ConnectorListeners();

}


