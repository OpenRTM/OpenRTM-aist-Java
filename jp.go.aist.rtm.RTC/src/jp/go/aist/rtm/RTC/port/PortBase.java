package jp.go.aist.rtm.RTC.port;

import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.util.CORBA_SeqUtil;
import jp.go.aist.rtm.RTC.util.CORBA_RTCUtil;
import jp.go.aist.rtm.RTC.util.ConnectorProfileFactory;
import jp.go.aist.rtm.RTC.util.NVUtil;
import jp.go.aist.rtm.RTC.util.POAUtil;
import jp.go.aist.rtm.RTC.util.PortProfileFactory;
import jp.go.aist.rtm.RTC.util.Properties;
import jp.go.aist.rtm.RTC.util.equalFunctor;
import jp.go.aist.rtm.RTC.util.operatorFunc;
import jp.go.aist.rtm.RTC.util.StringUtil;

import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;

import RTC.ConnectorProfile;
import RTC.ConnectorProfileHolder;
import RTC.ConnectorProfileListHolder;
import RTC.PortInterfacePolarity;
import RTC.PortInterfaceProfile;
import RTC.PortInterfaceProfileListHolder;
import RTC.PortProfile;
import RTC.PortService;
import RTC.PortServiceHelper;
import RTC.PortServiceListHolder;
import RTC.PortServicePOA;
import RTC.RTObject;
import RTC.ReturnCode_t;
import _SDOPackage.NVListHolder;
import _SDOPackage.NameValue;


/**
 * {@.ja Port の基底クラス}
 * {@.en Port base class}
 *
 *<p>
 *{@.ja RTC::Port の基底となるクラス。
 * RTC::Port はほぼ UML Port の概念を継承しており、ほぼ同等のものとみなす
 * ことができる。RT コンポーネントのコンセプトにおいては、
 * Port はコンポーネントに付属し、コンポーネントが他のコンポーネントと相互作用
 * を行う接点であり、通常幾つかのインターフェースと関連付けられる。
 * コンポーネントは Port を通して外部に対しインターフェースを提供または要求
 * することができ、Portはその接続を管理する役割を担う。
 * Port の具象クラスは、通常 RT コンポーネントインスタンス生成時に同時に
 * 生成され、提供・要求インターフェースを登録した後、RT コンポーネントに
 * 登録され、外部からアクセス可能な Port として機能することを想定している。
 * RTC::Port は CORBA インターフェースとして以下のオペレーションを提供する。
 *
 * <ul>
 * <li> get_port_profile()
 * <li> get_connector_profiles()
 * <li> get_connector_profile()
 * <li> connect()
 * <li> notify_connect()
 * <li> disconnect()
 * <li> notify_disconnect()
 * <li> disconnect_all()
 * </ul>
 *
 * このクラスでは、これらのオペレーションの実装を提供する。
 *
 * これらのオペレーションのうち、get_port_profile(), get_connector_profiles(),
 * get_connector_profile(), connect(), disconnect(), disconnect_all() は、
 * サブクラスにおいて特に振る舞いを変更する必要がないため、オーバーライド
 * することは推奨されない。
 *
 * notify_connect(), notify_disconnect() については、サブクラスが提供・要求
 * するインターフェースの種類に応じて、振る舞いを変更する必要が生ずる
 * 可能性があるが、これらを直接オーバーライドすることは推奨されず、
 * 後述の notify_connect(), notify_disconnect() の項においても述べられる通り
 * これらの関数に関連した protected 関数をオーバーライドすることにより
 * 振る舞いを変更することが推奨される。}
 * {@.en This class is a base class of RTC::Port.
 * RTC::Port inherits a concept of RT-Component, and can be regarded as almost
 * the same as it. In the concept of RT-Component, Port is attached to the
 * component, can mediate interaction between other components and usually is
 * associated with some interfaces.
 * Component can provide or require interface for outside via Port, and the
 * Port plays a role to manage the connection.
 *
 * Concrete class of Port assumes to be usually created at the same time that
 * RT-Component's instance is created, be registerd to RT-Component after
 * provided and required interfaces are registerd, and function as accessible
 * Port from outside.
 *
 * RTC::Port provides the following operations as CORBA interface:
 *
 * <ul>
 * <li> get_port_profile()
 * <li> get_connector_profiles()
 * <li> get_connector_profile()
 * <li> connect()
 * <li> notify_connect()
 * <li> disconnect()
 * <li> notify_disconnect()
 * <li> disconnect_all()
 * </ul>
 *
 * This class provides implementations of these operations.
 *
 * In these operations, as for get_port_profile(), get_connector_profiles(),
 * get_connector_profile(), connect(), disconnect() and disconnect_all(),
 * since their behaviors especially need not to be change in subclass, 
 * overriding is not recommended.
 *
 * As for notify_connect() and notify_disconnect(), you may have to modify
 * behavior according to the kind of interfaces that subclass provides and
 * requires, however it is not recommended these are overriden directly.
 * In the section of notify_connect() and notify_disconnect() as described
 * below, it is recommended that you modify behavior by overriding the
 * protected function related to these functions.}
 *
 */
public abstract class PortBase extends PortServicePOA {
    
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     *
     * <p>
     * {@.ja PortBase のコンストラクタは Port 名 name を引数に取り初期化を行う
     * と同時に、自分自身を CORBA Object として活性化し、自身の PortProfile
     * の port_ref に自身のオブジェクトリファレンスを格納する。}
     * {@.en The constructor of the ProtBase class is given the name of 
     * this Portand initialized. At the same time, 
     * the PortBase activates itself as CORBA object 
     * and stores its object reference to the PortProfile's * port_ref member.}
     * </p>
     *
     * @param name 
     *   {@.ja Port の名前(デフォルト値:"")}
     *   {@.en The name of Port (The default value:"")}
     *
     */
    public PortBase(final String name) {
        m_ownerInstanceName = "unknown";
        String portname = m_ownerInstanceName;
        portname += ".";
        portname += name;
        this.m_profile.name = portname;
        this.m_profile.owner = null;
        this.m_profile.interfaces = new PortInterfaceProfile[0];
        this.m_profile.connector_profiles = new ConnectorProfile[0];
        this.m_profile.properties = new NameValue[0];
        this.m_objref = PortServiceHelper.narrow(this._this()._duplicate());
        this.m_profile.port_ref = this.m_objref;
        rtcout = new Logbuf(name);
        m_onPublishInterfaces = null;
        m_onSubscribeInterfaces = null;
        m_onConnected = null;
        m_onUnsubscribeInterfaces = null;
        m_onDisconnected = null;
        m_onConnectionLost = null;
        m_connectionLimit = 0;
    }

    /**
     * {@.ja 当該PortのCORBAオブジェクト参照を取得する。}
     * {@.en Gets CORBA object referense of this PortService}
     * 
     * @return 
     *   {@.ja 当該PortのCORBAオブジェクト参照}
     *   {@.en CORBA object referense of this OpenRTM.OutPortCdr}
     */
    public PortService _this() {
        if (this.m_objref == null) {
            try {
                this.m_objref = PortServiceHelper.narrow(POAUtil.getRef(this));
                
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        
        return this.m_objref;
    }

    /**
     * {@.ja デフォルトコンストラクタ}
     * {@.en Default constructor}
     * <p>
     * {@.ja ポート名には空文字列が割り当てらる。}
     * {@.en Empty string is assigned port name.}
     */
    public PortBase() {
        this("");
    }

    /**
     * {@.ja [CORBA interface] PortProfileを取得する}
     * {@.en [CORBA interface] Get the PortProfile of the Port}
     * <p>
     * {@.ja Portが保持するPortProfileを返す。この関数は CORBA オペレーション
     * であり、CORBA のメモリ管理規則に従い、呼び出し側は返される
     * PortProfile オブジェクトを解体する責任がある。PortProfile 構造体
     * は以下のメンバーを持つ。
     * <ul>
     * <li>- name          [string 型] Port の名前。
     * <li>- interfaces    [PortInterfaceProfileList 型] Port が保持する
     *                     PortInterfaceProfile のシーケンス
     * <li>- port_ref      [Port Object 型] Port 自身のオブジェクトリファレンス
     * <li>- connector_profile [ConnectorProfileList 型] Port が現在保持する
     *                     ConnectorProfile のシーケンス
     * <li>- owner         [RTObject Object 型] この Port を所有する
     *                     RTObjectのリファレンス
     * <li>- properties    [NVList 型] その他のプロパティ。
     * </ul>
     * 事後条件:この関数を呼び出すことにより内部状態が変更されることはない。}
     * {@.en This operation returns the PortProfile of the Port. Since this
     * function is CORBA operation, callers have responsibility to
     * destruction of the returned PortProfile object according to the
     * CORBA memory management rules.
     *
     * PortProfile struct has the following members:
     * <ul>
     * <li>- name          [string type] The name of the Port.
     * <li>- interfaces    [PortInterfaceProfileList type] The sequence of 
     *                     PortInterfaceProfile owned by the Port
     * <li>- port_ref      [Port Object type] The object reference of the Port.
     * <li>- connector_profile [ConnectorProfileList type] The sequence of 
     *                     ConnectorProfile owned by the Port.
     * <li>- owner         [RTObject Object type] The object reference of 
     *                     RTObject that is owner of the Port.
     * <li>- properties    [NVList type] The other properties.
     * </ul>
     * post:This function never changes the state of the object.}
     *
     *
     * @return 
     *   {@.ja PortProfile}
     *   {@.en PortProfile of the Port}
     *
     */
    public PortProfile get_port_profile() {
        rtcout.println(Logbuf.TRACE, "get_port_profile()");

        updateConnectors();
        synchronized (this.m_profile) {
            return PortProfileFactory.clone(this.m_profile);
        }
    }
    
    /**
     * {@.ja PortProfile を取得する。}
     * {@.en Get the PortProfile of the Port}
     * <p>
     * {@.ja この関数は、オブジェクト内部に保持されている PortProfile の
     * const 参照を返す const 関数である。
     *
     * 事後条件:この関数を呼び出すことにより内部状態が変更されることはない。}
     * {@.en This function is a const function that returns a const
     * reference of the PortProfile stored in this Port.
     *
     * post:This function never changes the state of the object.}
     *
     * @return 
     *   {@.ja PortProfile}
     *   {@.en PortProfile}
     */
    public final PortProfile getPortProfile() {
        return m_profile;
    }

    /**
     * {@.ja [CORBA interface] ConnectorProfileListを取得する}
     * {@.en [CORBA interface] Get the ConnectorProfileList of the Port}
     * <p>
     * {@.ja Portが保持する ConnectorProfile の sequence を返す。この関数は
     * CORBA オペレーションであり、CORBA のメモリ管理規則に従い、呼び出
     * し側は返される ConnectorProfileList オブジェクトを解体する責任が
     * ある。
     *
     * ConnectorProfile は Port 間の接続プロファイル情報を保持する構造体であり、
     * 接続時にPort間で情報交換を行い、関連するすべての Port で同一の値が
     * 保持される。
     * ConnectorProfile は以下のメンバーを保持している。
     * <ul>
     * <li>- name         [string 型] このコネクタの名前。
     * <li>- connector_id [string 型] ユニークなID
     * <li>- ports    [Port sequnce] このコネクタに関連する Port のオブジェクト
     *                リファレンスのシーケンス。
     * <li>- properties   [NVList 型] その他のプロパティ。
     * </ul>
     * 事後条件:この関数を呼び出すことにより内部状態が変更されることはない。}
     * {@.en This operation returns a list of the ConnectorProfiles of the
     * Port.  Since this function is CORBA operation, callers have
     * responsibility to destruction of the returned ConnectorProfileList
     * object according to the CORBA memory management rules.
     *
     * ConnectorProfile includes the connection information that
     * describes relation between (among) Ports, and Ports exchange
     * the ConnectionProfile on connection process and hold the same
     * information in every Port.  ConnectionProfile has the following
     * members:
     * <ul>
     * <li>- name         [string type] The name of the connection.
     * <li>- connector_id [string type] Unique identifier.
     * <li>- ports        [Port sequnce] The sequence of Port's object reference
     *                that are related the connection.
     * <li>- properties   [NVList type] The other properties.
     * </ul>
     * post:This function never changes the state of the object.}
     *
     * @return 
     *   {@.ja この Port が保持する ConnectorProfile}
     *   {@.en ConnectorProfileList of the Port}
     */
    public RTC.ConnectorProfile[] get_connector_profiles() {
        rtcout.println(Logbuf.TRACE, "get_connector_profiles()");

        updateConnectors();
        synchronized (this.m_profile) {

            int length = this.m_profile.connector_profiles.length;

            RTC.ConnectorProfile[] conn_prof = new RTC.ConnectorProfile[length];
            for (int i = 0; i < length; i++) {
                conn_prof[i] = this.m_profile.connector_profiles[i];
            }

            return conn_prof;
        }
    }

    /**
     * {@.ja [CORBA interface] ConnectorProfile を取得する}
     * {@.en [CORBA interface] Get the ConnectorProfile}
     * <p>
     * {@.ja connector_id で指定された ConnectorProfile を返す。この関数は
     * CORBA オペレーションであり、CORBA のメモリ管理規則に従い、呼び出
     * し側は返される ConnectorProfile オブジェクトを解体する責任がある。
     *
     * 事前条件:引数に与える connector_id は有効な文字列でなければならない。
     * 空文字を指定した場合、または指定した connector_id を持つ
     * ConnectorProfile が見つからない場合は、空の ConnectorProfile を
     * 返す。
     *
     * 事後条件:この関数を呼び出すことにより内部状態が変更されることはない。}
     * {@.en This operation returns the ConnectorProfiles specified
     * connector_id.  Since this function is CORBA operation, callers
     * have responsibility to destruction of the returned
     * ConnectorProfile object according to the CORBA memory
     * management rules.
     *
     * If ConnectorProfile with specified connector_id is not included,
     * empty ConnectorProfile is returned.
     *
     * post:This function never changes the state of the object.}
     *
     *
     * @param connector_id 
     *   {@.ja ConnectorProfile の ID}
     *   {@.en ID of the ConnectorProfile}
     * @return 
     *   {@.ja connector_id で指定された ConnectorProfile}
     *   {@.en the ConnectorProfile identified by the connector_id}
     */
    public ConnectorProfile get_connector_profile(final String connector_id) {

        rtcout.println(Logbuf.TRACE, "get_connector_profile("+connector_id+")");

        updateConnectors();
        synchronized (m_profile_mutex) {

            ConnectorProfileListHolder holder =
                new ConnectorProfileListHolder(this.m_profile.connector_profiles);
            int index = 
                    CORBA_SeqUtil.find(holder, new find_conn_id(connector_id));
            
            if (index < 0) {
                ConnectorProfile conn_prof = ConnectorProfileFactory.create();
                return conn_prof;
            }
            ConnectorProfile org_conn_prof = this.m_profile.connector_profiles[index];
            ConnectorProfile conn_prof = new ConnectorProfile(
                    org_conn_prof.name,
                    org_conn_prof.connector_id,
                    org_conn_prof.ports,
                    org_conn_prof.properties);
            
            return conn_prof;
        }
    }

    /**
     * <p>ポート間の接続を行います。指定された接続プロファイルにしたがってポート間の接続を確立します。</p>
     * 
     * <p>アプリケーションプログラム側は、幾つかのコンポーネントが持つ複数のポート間を接続したい場合、
     * 適切な値を設定した接続プロファイルを引数として渡すことにより、関連するポート間の接続を確立できます。</p>
     * 
     * <p>本メソッドの呼び出しにあたっては、ConnectorProfileオブジェクトのメンバのうち、
     * 次のものについて値を設定しておかねばなりません。
     * <ul>
     * <li>name</li>
     * <li>ports</li>
     * <li>properties</li>
     * </ul>
     * </p>
     * 
     * @param connector_profile 接続プロファイル
     * @return ReturnCode_t型による戻り値
     */
    /**
     * {@.ja [CORBA interface] Port の接続を行う}
     * {@.en [CORBA interface] Connect the Port}
     * <p>
     * {@.ja 与えられた ConnectoionProfile の情報に基づき、Port間の接続を確立
     * する。この関数は主にアプリケーションプログラムやツールから呼び出
     * すことを前提としている。
     * <b>事前条件:</b>
     * <ul>
     * <li> アプリケーションプログラムは、コンポーネント間の複数の
     * Port を接続するために、適切な値をセットした ConnectorProfile を
     * connect() の引数として与えて呼び出さなければならない。</li>
     *
     * <li> connect() に与える ConnectorProfile のメンバーのうち、
     * name, ports, properties メンバーに対してデータをセットしなければ
     * ならない。connector_id には通常空文字を設定するか、適当なUUIDを
     * 文字列で設定する必要がある。</li>
     *
     * <li> ConnectorProfile::name は接続につける名前で CORBA::string
     * 型に格納できる任意の文字列である必要がある。</li>
     * 
     * <li> ConnectorProfile::connector_id はすべての接続に対して一意な
     * ID (通常はUUID) が格納される。UUIDの設定は connect() 関数内で行
     * われるので、呼び出し側は空文字を設定する。既存の接続と同じUUIDを
     * 設定し connect() を呼び出した場合には PRECONDITION_NOT_MET エラー
     * を返す。ただし、将来の拡張で既存の接続プロファイルを更新するため
     * に既存の UUID を設定して呼び出す使用法が用いられる可能性がある。</li>
     *
     * <li> ConnectorProfile::ports は RTC::PortService のシーケンスで、
     * 接続を構成する通常2つ以上のポートのオブジェクト参照を代入する必
     * 要がある。例外として、ポートのオブジェクト参照を1つだけ格納して
     * connect()を呼び出すことで、ポートのインターフェース情報を取得し
     * たり、特殊なポート(CORBAのRTC::PortService以外の相手)に対して接
     * 続を行う場合もある。</li>
     *
     * <li> ConnectorProfile::properties はポートに関連付けられたインター
     * フェースに対するプロパティを与えるために使用する。プロパティは、
     * string 型をキー、Any 型を値としてもつペアのシーケンスであり、値
     * には任意のCORBAデータ型を格納できるが、可能な限り string 型とし
     * て格納されることが推奨される。</li>
     *
     * <li> 以上 connect() 呼び出し時に設定する ConnectorProfile のメン
     * バをまとめると以下のようになる。
     * <ul>
     * <li>- ConnectorProfile::name: 任意の接続名
     * <li>- ConnectorProfile::connector_id: 空文字
     * <li>- ConnectorProfile::ports: 1つ以上のポート
     * <li>- ConnectorProfile::properties: インターフェースに対するプロパティ
     * </ul>
     * </li>
     * </ul>
     * <b>事後条件:</b>
     * <ul>
     * <li> connect() 関数は、ConnectorProfile::portsに格納されたポー
     * トシーケンスの先頭のポートに対して notify_connect() を呼ぶ。</li>
     *
     * <li> notify_connect() は ConnectorProfile::ports に格納されたポー
     * ト順に notify_connect() をカスケード呼び出しする。このカスケード
     * 呼び出しは、途中のnotify_connect() でエラーが出てもポートのオブ
     * ジェクト参照が有効である限り、必ずすべてのポートに対して行われる
     * ことが保証される。有効でないオブジェクト参照がシーケンス中に存在
     * する場合、そのポートをスキップして、次のポートに対して
     * notify_connect() を呼び出す。</li>
     *
     * <li> connect() 関数は、notify_connect()の戻り値がRTC_OKであれば、
     * RTC_OK を返す。この時点で接続は完了する。RTC_OK以外
     * の場合は、この接続IDに対してdisconnect()を呼び出し接続を解除し、
     * notify_connect() が返したエラーリターンコードをそのまま返す。</li>
     * 
     * <li> connect() の引数として渡した ConnectorProfile には、
     * ConnectorProfile::connector_id および、途中のポートが
     * publishInterfaces() で公開したポートインターフェースの各種情報が
     * 格納されている。connect() および途中の notify_connect() が
     * ConnectorProfile::{name, ports} を変更することはない。</li></ul>}
     *
     * {@.en This operation establishes connection according to the given
     * ConnectionProfile inforamtion. This function is premised on
     * calling from mainly application program or tools.
     *
     * <b>Preconditioin:</b>
     * <ul>
     * <li>To establish the connection among Ports of RT-Components,
     * application programs must call this operation giving
     * ConnectorProfile with valid values as an argument.</li>
     *
     * <li>Out of ConnectorProfile member variables, "name", "ports"
     * and "properties" members shall be set valid
     * data. "connector_id" shall be set as empty string value or
     * valid string UUID value.</li>
     *
     * <li>ConnectorProfile::name that is connection identifier shall
     * be any valid CORBA::string.</li>
     * 
     *
     * <li>ConnectorProfile::connector_id shall be set unique
     * identifier (usually UUID is used) for all connections. Since
     * UUID string value is usually set in the connect() function,
     * caller should just set empty string. If the connect() is called
     * with the same UUID as existing connection, this function
     * returns PRECONDITION_NOT_MET error. However, in order to update
     * the existing connection profile, the "connect()" operation with
     * existing connector ID might be used as valid method by future
     * extension</li>
     *
     * <li>ConnectorProfile::ports, which is sequence of
     * RTC::PortService references, shall store usually two or more
     * ports' references. As exceptions, the "connect()" operation
     * might be called with only one reference in ConnectorProfile, in
     * case of just getting interfaces information from the port, or
     * connecting a special port (i.e. the peer port except
     * RTC::PortService on CORBA).</li>
     *
     * <li>ConnectorProfile::properties might be used to give certain
     * properties to the service interfaces associated with the port.
     * The properties is a sequence variable with a pair of key string
     * and Any type value. Although the A variable can store any type
     * of values, it is not recommended except string.</li>
     *
     * <li>The following is the summary of the ConnectorProfile
     * member to be set when this operation is called.
     * <ul>
     * <li>- ConnectorProfile::name: The any name of connection</li>
     * <li>- ConnectorProfile::connector_id: Empty string</li>
     * <li>- ConnectorProfile::ports: One or more port references</li>
     * <li>- ConnectorProfile::properties: Properties for the interfaces</li>
     * </li>
     * </ul>
     * <b>Postcondition:</b>
     * <ul>
     * <li> connect() operation will call the first port in the
     * sequence of the ConnectorProfile.</li>
     *
     * <li> "noify_connect()"s perform cascaded call to the ports
     * stored in the ConnectorProfile::ports by order. Even if errors
     * are raised by intermediate notify_connect() operation, as long
     * as ports' object references are valid, it is guaranteed that
     * this cascaded call is completed in all the ports.  If invalid
     * or dead ports exist in the port's sequence, the ports are
     * skipped and notify_connect() is called for the next valid port.</li>
     *
     * <li> connect() function returns RTC_OK if all the
     * notify_connect() return RTC_OK. At this time the connection is
     * completed.  If notify_connect()s return except RTC_OK,
     * connect() calls disconnect() operation with the connector_id to
     * destruct the connection, and then it returns error code from
     * notify_connect().</li>
     *
     * <li> The ConnectorProfile argument of the connect() operation
     * returns ConnectorProfile::connector_id and various information
     * about service interfaces that is published by
     * publishInterfaces() in the halfway ports. The connect() and
     * halfway notify_connect() functions never change
     * ConnectorProfile::{name, ports}. </li></ul>}
     *
     * @param connector_profile 
     *   {@.ja ConnectorProfile}
     *   {@.en The ConnectorProfile.}
     * @return 
     *   {@.ja ReturnCode_t 型のリターンコード}
     *   {@.en ReturnCode_t The return code of ReturnCode_t type.}
     *
     */
    public ReturnCode_t connect(ConnectorProfileHolder connector_profile) {

        rtcout.println(Logbuf.TRACE, "connect()");
        if (isEmptyId(connector_profile.value)) {
            
            synchronized (m_profile_mutex) {
                // "connector_id" stores UUID which is generated at the initial Port
                // in connection process.
                setUUID(connector_profile);
                assert (! isExistingConnId(connector_profile.value.connector_id));
            }
        }
        else {
            synchronized (m_profile_mutex) {
   	        if (isExistingConnId(connector_profile.value.connector_id)) {
                    rtcout.println(Logbuf.ERROR, "Connection already exists.");
                    return ReturnCode_t.PRECONDITION_NOT_MET;
	        }
            }
        }
        
        try {
            ReturnCode_t ret 
           = connector_profile.value.ports[0].notify_connect(connector_profile);
            if (!ret.equals(ReturnCode_t.RTC_OK)) {
                rtcout.println(Logbuf.ERROR, "Connection failed. cleanup.");
                disconnect(connector_profile.value.connector_id);
            }
            return ret;
        } catch(Exception ex) {
            return ReturnCode_t.BAD_PARAMETER;
        }
    }

    /**
     * {@.ja [CORBA interface] Port の接続通知を行う}
     * {@.en [CORBA interface] Notify the Ports connection}
     *
     * <p>
     * {@.ja このオペレーションは、Port間の接続が行われる際に、Port間で内部的
     * に呼ばれるオペレーションであって、通常アプリケーションプログラム
     * や、Port以外のRTC関連オブジェクト等から呼び出されることは想定さ
     * れていない。
     *
     * notify_connect() 自体はテンプレートメソッドパターンとして、サブ
     * クラスで実装されることが前提の publishInterfaces(),
     * subscribeInterfaces() の2つの関数を内部で呼び出す。処理の手順は
     * 以下の通りである。
     *
     * - publishInterfaces(): インターフェース情報の公開
     * - connectNext(): 次の Port の notify_connect() の呼び出し
     * - subscribeInterfaces(): インターフェース情報の取得
     * - 接続情報の保存
     *
     * notify_connect() は ConnectorProfile::ports に格納されている
     * Port の順序に従って、カスケード状に呼び出しを行うことにより、イ
     * ンターフェース情報の公開と取得を関連すすべてのポートに対して行う。
     * このカスケード呼び出しは途中で中断されることはなく、必ず
     * ConnectorProfile::ports に格納されている全ポートに対して行われる。
     * </p>
     * <p>
     * 事前条件:<br>
     * notify_connect() は ConnectorProfile::ports 内に格納されて
     * いる Port 参照リストのうち、当該 Port 自身の参照の次に格納されて
     * いる Port に対して notify_connect() を呼び出す。したがって
     * ConnectorProfile::ports には当該 Port の参照が格納されている必要
     * がある。もし、自身の参照が格納されていない場合、その他の処理によ
     * りエラーが上書きされなければ、BAD_PARAMETER エラーが返される。
     *
     * 呼び出し時に ConnectorProfile::connector_id には一意なIDと
     * して UUID が保持されている必要がある。通常 connector_id は
     * connect() 関数により与えられ、空文字の場合は動作は未定義である。
     * </p>
     * <p>
     * 事後条件:<br>
     * ConnectorProfile::name, ConnectorProfile::connector_id,
     * ConnectorProfile::ports は notify_connect() の呼び出しにより
     * 書き換えられることはなく不変である。
     *
     * ConnectorProfile::properties は notify_connect() の内部で、
     * 当該 Port が持つサービスインターフェースに関する情報を他の Port
     * に伝えるために、プロパティ情報が書き込まれる。
     *
     * なお、ConnectorProfile::ports のリストの最初 Port の
     * notify_connet() が終了した時点では、すべての関連する Port の
     * notify_connect() の呼び出しが完了する。publishInterfaces(),
     * connectNext(), subscribeInterfaces() および接続情報の保存のいず
     * れかの段階でエラーが発生した場合でも、エラーコードは内部的に保持
     * されており、最初に生じたエラーのエラーコードが返される。}
     * {@.en This operation is usually called from other ports' connect() or
     * notify_connect() operations when connection between ports is
     * established.  This function is not premised on calling from
     * other functions or application programs.
     *
     * According to the template method pattern, the notify_connect()
     * calls "publishInterfaces()" and "subsctiveInterfaces()"
     * functions, which are premised on implementing in the
     * subclasses. The processing sequence is as follows.
     *
     * - publishInterfaces(): Publishing interface information
     * - connectNext(): Calling notify_connect() of the next port
     * - subscribeInterfaces(): Subscribing interface information
     * - Storing connection profile
     *
     * According to the order of port's references stored in the
     * ConnectorProfile::ports, publishing interface information to
     * all the ports and subscription interface information from all
     * the ports is performed by "notify_connect()"s.  This cascaded
     * call never aborts in the halfway operations, and calling
     * sequence shall be completed for all the ports.
     * </p>
     * <p>
     * Precondition:<br>
     * notify_connect() calls notify_connect() for the port's
     * reference that is stored in next of this port's reference in
     * the sequence of the ConnectorProfile::ports. Therefore the
     * reference of this port shall be stored in the
     * ConnectorProfile::ports. If this port's reference is not stored
     * in the sequence, BAD_PARAMETER error will be returned, except
     * the return code is overwritten by other operations.
     *
     * UUID shall be set to ConnectorProfile::connector_id as a
     * unique identifier when this operation is called.  Usually,
     * connector_id is given by a connect() function and, the behavior
     * is undefined in the case of a null character.
     * </p>
     * <p>
     * Postcondition:<br>
     * ConnectorProfile::name, ConnectorProfile::connector_id,
     * ConnectorProfile::ports are invariant, and they are never
     * rewritten by notify_connect() operations.
     *
     * In order to transfer interface information to other
     * ports, interface property information is stored into the
     * ConnectorProfile::properties.
     *
     * At the end of notify_connect() operation for the first
     * port stored in the ConnectorProfile::ports sequence, the
     * related ports' notify_connect() invocations complete. Even if
     * errors are raised at the halfway of publishInterfaces(),
     * connectNext(), subscribeInterfaces() and storing process of
     * ConnectorProfile, error codes are saved and the first error is
     * returned.}
     *
     * @param connector_profile 
     *   {@.ja ConnectorProfileHolder}
     *   {@.en ConnectorProfileHolder}
     * @return 
     *   {@.ja ReturnCode_t 型のリターンコード}
     *   {@.en ReturnCode_t The return code of ReturnCode_t type.}
     *
     */
    public ReturnCode_t 
    notify_connect(ConnectorProfileHolder connector_profile) {

        rtcout.println(Logbuf.TRACE, "notify_connect()");


        Properties prop = new Properties();
        NVListHolder nvholder = 
                new NVListHolder(this.m_profile.properties);
        NVUtil.copyToProperties(prop, nvholder);
        boolean default_value = StringUtil.toBool(
                   prop.getProperty("dataport.allow_dup_connection"),
                                     "YES", "NO", false); 

        prop = new Properties();
        nvholder = 
                new NVListHolder(connector_profile.value.properties);
        NVUtil.copyToProperties(prop, nvholder);
        if (!StringUtil.toBool(
            prop.getProperty("dataport.allow_dup_connection"), 
            "YES","NO",default_value)){
            for(int ic=0;ic<connector_profile.value.ports.length;++ic){
                RTC.PortService port = connector_profile.value.ports[ic];
                if(!port._is_equivalent(m_objref)){
                    boolean ret = CORBA_RTCUtil.already_connected(port, 
                                                                  m_objref);
                    if(ret){
                        return ReturnCode_t.PRECONDITION_NOT_MET;
                    }
                }
            }
        }

        synchronized (m_connectorsMutex){
            ReturnCode_t[] retval = {ReturnCode_t.RTC_OK, ReturnCode_t.RTC_OK, 
                                 ReturnCode_t.RTC_OK}; 

            onNotifyConnect(getName(), connector_profile.value);

            //
            // publish owned interface information to the ConnectorProfile
            retval[0] = publishInterfaces(connector_profile);
            if (! ReturnCode_t.RTC_OK.equals(retval[0])) {
                rtcout.println(Logbuf.ERROR, 
                           "publishInterfaces() in notify_connect() failed.");
            }
            onPublishInterfaces(getName(), connector_profile.value, retval[0]);

            synchronized (m_onPublishInterfaces_mutex){
                if (m_onPublishInterfaces != null) {
                    m_onPublishInterfaces.run(connector_profile);
                }
            }
    

            // call notify_connect() of the next Port
            retval[1] = connectNext(connector_profile);
            if (! ReturnCode_t.RTC_OK.equals(retval[1])) {
                rtcout.println(Logbuf.ERROR, 
                               "connectNext() in notify_connect() failed.");
            }
            onConnectNextport(getName(), connector_profile.value, retval[1]);

            // subscribe interface from the ConnectorProfile's information
            synchronized (m_onSubscribeInterfaces_mutex){
                if (m_onSubscribeInterfaces != null) {
                    m_onSubscribeInterfaces.run(connector_profile);
                }
            }
            retval[2] = subscribeInterfaces(connector_profile);
            if (! ReturnCode_t.RTC_OK.equals(retval[2])) {
                // cleanup this connection for downstream ports
                rtcout.println(Logbuf.ERROR, 
                           "subscribeInterfaces() in notify_connect() failed.");
            }
            onSubscribeInterfaces(getName(), connector_profile.value, retval[2]);

            rtcout.println(Logbuf.PARANOID, 
                m_profile.connector_profiles.length
                +" connectors are existing.");

            synchronized (m_profile_mutex) {
                // update ConnectorProfile
                int index = 
                     findConnProfileIndex(connector_profile.value.connector_id);
                if (index < 0) {
                    ConnectorProfileListHolder holder =
                      new ConnectorProfileListHolder(
                                             this.m_profile.connector_profiles);
                    CORBA_SeqUtil.push_back(holder, connector_profile.value);
                    this.m_profile.connector_profiles = holder.value;
                    rtcout.println(Logbuf.PARANOID,
                               "New connector_id. Push backed.");

                } else {
                    this.m_profile.connector_profiles[index] = 
                                                       connector_profile.value;
                    rtcout.println(Logbuf.PARANOID,
                                   "Existing connector_id. Updated.");
                } 
            }

            for (int i=0, len=retval.length; i < len; ++i) {
                if (! ReturnCode_t.RTC_OK.equals(retval[i])) {
                    onConnected(getName(), connector_profile.value, retval[i]);
                    return retval[i];
                }
            }

            // connection established without errors
            synchronized (m_onConnected_mutex){
                if (m_onConnected != null) {
                    m_onConnected.run(connector_profile);
                }
            }
            onConnected(getName(), connector_profile.value, ReturnCode_t.RTC_OK);
            return ReturnCode_t.RTC_OK;
        }
    }

    /**
     * <p>ポートの接続を解除します。このメソッドは、接続確立時に接続に対して与えられるコネクタIDに
     * 対応するピア・ポートとの接続を解除します。</p>
     * 
     * @param connector_id コネクタID
     * @return ReturnCode_t型の戻り値
     */
    /**
     * {@.ja [CORBA interface] Port の接続を解除する}
     * {@.en [CORBA interface] Disconnect the Port}
     * <p>
     * {@.ja このオペレーションは与えられた connector_id に対応する接続を解除
     * する。connector_id は通常、システム全体において一意な UUID の文
     * 字列であり、事前に connect()/notify_connect() の呼び出しにより確
     * 立された接続プロファイル ConnectorProfile::connector_id に対応す
     * る。
     * <b>事前条件:</b>
     * <ul>
     * <li> connector_id は Port が保持する ConnectorProfile の少なくと
     * も一つの ID に一致する文字列でなければならない。当該 Port が持つ
     * ConnectorProfile のリスト内に connector_id と同一の ID を持つ
     * ConnectorProfile が存在しなければこの関数は BAD_PARAMETER エラー
     * を返す。</li>
     *
     * <li> connector_id と同じ ID を持つ ConnectorProfile::ports には
     * 有効な Port の参照が含まれていなければならない。</li>
     *
     * <li> disconnect() 関数は、ConnectorProfile::ports の Port の参
     * 照リストの先頭に対して、notify_disconnect() を呼び出す。参照が無
     * 効であるなど、notify_disconnect() の呼び出しに失敗した場合には、
     * 参照リストの先頭から順番に成功するまで notify_disconnect() の呼
     * び出しを試す。notify_disconnect() の呼び出しに一つでも成功すれば、
     * notify_disconnect() の返却値をそのまま返し、一つも成功しなかった
     * 場合には RTC_ERROR エラーを返す。</li></ul>}
     *
     * {@.en This operation destroys connection between this port and the
     * peer port according to given connector_id. Usually connector_id
     * should be a UUID string that is unique in the system.  And the
     * connection, which is established by connect()/notify_connect()
     * functions, is identified by the ConnectorProfile::connector_id.
     * <b>Precondition</b>
     * <ul>
     * <li> connector_id shall be a character string which is same
     * with ID of at least one of the ConnectorProfiles stored in this
     * port. If ConnectorProfile that has same ID with the given
     * connector_id does not exist in the list of ConnectorProfile,
     * this operation returns BAD_PARAMTER error.</li>
     *
     * <li> ConnectorProfile::ports that is same ID with given
     * connector_id shall store the valid ports' references.</li>
     *
     * <li> disconnect() function invokes the notify_disconnect() for
     * the port that is stored in the first of the
     * ConnectorProfile::ports. If notify_disconnect() call fails for
     * the first port, It tries on calling "notify_disconnect()" in
     * order for ports stored in ConnectorProfile::ports until the
     * operation call is succeeded. If notify_disconnect() succeeded
     * for at least one port, it returns return code from
     * notify_disconnect(). If none of notify_connect() call
     * succeeded, it returns RTC_ERROR error.</li></ul>}
     *
     * @param connector_id 
     *   {@.ja ConnectorProfile の ID}
     *   {@.en The ID of the ConnectorProfile.}
     * @return 
     *   {@.ja ReturnCode_t 型のリターンコード}
     *   {@.en ReturnCode_t The return code of ReturnCode_t type.}
     *
     */
    public ReturnCode_t disconnect(final String connector_id) {

        rtcout.println(Logbuf.TRACE, "disconnect("+connector_id+")");
        // find connector_profile
        int index = findConnProfileIndex(connector_id);
        if (index < 0) {
            rtcout.println(Logbuf.ERROR, "Invalid connector id: "+connector_id);
       	    return ReturnCode_t.BAD_PARAMETER;
        }

        ConnectorProfile prof;
        synchronized (m_profile_mutex) {
            // lock and copy profile
            prof = m_profile.connector_profiles[index];
        }

        if (prof.ports.length < 1) {
            rtcout.println(Logbuf.FATAL, "ConnectorProfile has empty port list.");
            return ReturnCode_t.PRECONDITION_NOT_MET;
        }

        for (int i=0, len=prof.ports.length; i < len; ++i) {
            RTC.PortService p = prof.ports[i];
            try {
                return p.notify_disconnect(connector_id);
            }
            catch (SystemException e) {
                rtcout.println(Logbuf.WARN, "Exception caught: minor code("+e.minor+")."+e.toString());
                continue;
            }
            catch (Exception e) {
                rtcout.println(Logbuf.WARN, "Unknown exception caught.");
                continue;
            }
        }
        rtcout.println(Logbuf.ERROR, "notify_disconnect() for all ports failed.");
        return ReturnCode_t.RTC_ERROR;
    }

    /**
     * {@.ja [CORBA interface] Port の接続解除通知を行う}
     * {@.en [CORBA interface] Notify the Ports disconnection}
     *
     * <p>
     * {@.ja このオペレーションは、Port間の接続解除が行われる際に、Port間で内
     * 部的に呼ばれるオペレーションであり、通常アプリケーションプログラ
     * ムや、 Port 以外の RTC 関連オブジェクト等から呼び出されることは
     * 想定されていない。
     *
     * notify_disconnect() 自体はテンプレートメソッドパターンとして、サ
     * ブクラスで実装されることが前提の unsubscribeInterfaces() 関数を
     * 内部で呼び出す。処理の手順は以下の通りである。
     *
     * - ConnectorProfile の検索
     * - 次の Port の notify_disconnect() 呼び出し
     * - unsubscribeInterfaces()
     * - ConnectorProfile の削除
     *
     * notify_disconnect() は ConnectorProfile::ports に格納されている
     * Port の順序に従って、カスケード状に呼び出しを行うことにより、接
     * 続の解除をすべての Port に通知する。
     * </p>
     * <p>
     * 事前条件:<br>
     * Port は与えられた connector_id に対応する ConnectorProfile
     * を保持していなければならない。
     * </p>
     * <p>
     * 事後条件:<br>
     * connector_id に対応する ConnectorProfile が見つからない場
     * 合はBAD_PARAMETER エラーを返す。
     *
     * カスケード呼び出しを行う際には ConnectorProfile::ports に
     * 保持されている Port の参照リストのうち、自身の参照の次の参照に対
     * して notify_disconnect() を呼び出すが、その呼び出しで例外が発生
     * した場合には、呼び出しをスキップしリストの次の参照に対して
     * notify_disconnect() を呼び出す。一つも呼び出しに成功しない場合、
     * RTC_ERROR エラーコードを返す。
     *
     * なお、ConnectorProfile::ports のリストの最初 Port の
     * notify_disconnet() が終了した時点では、すべての関連する Port の
     * notify_disconnect() の呼び出しが完了する。}
     * {@.en This operation is invoked between Ports internally when the
     * connection is destroied. Generally it is not premised on
     * calling from application programs or RTC objects except Port
     * object.
     *
     * According to the template method pattern, the
     * notify_disconnect() calls unsubsctiveInterfaces() function,
     * which are premised on implementing in the subclasses. The
     * processing sequence is as follows.
     *
     * - Searching ConnectorProfile
     * - Calling notify_disconnect() for the next port
     * - Unsubscribing interfaces
     * - Deleting ConnectorProfile
     *
     * notify_disconnect() notifies disconnection to all the ports by
     * cascaded call to the stored ports in the
     * ConnectorProfile::ports in order.
     *
     * </p>
     * <p>
     * Precondition:<br>
     * The port shall store the ConnectorProfile having same id
     * with connector_id.
     * </p>
     * <p>
     * Postcondition:<br>
     * If ConnectorProfile of same ID with connector_id does not
     * exist, it returns BAD_PARAMETER error.
     * For the cascaded call, this operation calls
     * noify_disconnect() for the port that is stored in the next of
     * this port in the ConnectorProfile::ports.  If the operation
     * call raises exception for some failure, it tries to call
     * notify_disconnect() and skips until the operation succeeded.
     * If none of operation call succeeded, it returns RTC_ERROR.
     *
     * At the end of notify_disconnect() operation for the first
     * port stored in the ConnectorProfile::ports sequence, the
     * related ports' notify_disconnect() invocations complete.}
     * 
     * @param connector_id
     *   {@.ja ConnectorProfile の ID}
     *   {@.en The ID of the ConnectorProfile.}
     * @return 
     *   {@.ja ReturnCode_t 型のリターンコード}
     *   {@.en The return code of ReturnCode_t type.}
     *
     *
     */
    public ReturnCode_t notify_disconnect(final String connector_id) {

        rtcout.println(Logbuf.TRACE, "notify_disconnect("+connector_id+")");

        // The Port of which the reference is stored in the beginning of
        // ConnectorProfile's PortList is master Port.
        // The master Port has the responsibility of disconnecting all Ports.
        // The slave Ports have only responsibility of deleting its own
        // ConnectorProfile.

        synchronized (m_connectorsMutex){
            synchronized (m_profile_mutex) {
                // find connector_profile
                int index = findConnProfileIndex(connector_id);
                if (index < 0) {
                    rtcout.println(Logbuf.ERROR, 
                               "Invalid connector id: "+connector_id);
	             return ReturnCode_t.BAD_PARAMETER;
                 }

                ConnectorProfile prof 
                    = this.m_profile.connector_profiles[index];
                onNotifyDisconnect(getName(), prof);
                ReturnCode_t retval = disconnectNext(prof);
                onDisconnectNextport(getName(), prof, retval);
                synchronized (m_onUnsubscribeInterfaces_mutex){
                    if (m_onUnsubscribeInterfaces != null) {
                        ConnectorProfileHolder holder 
                            = new ConnectorProfileHolder(prof);
                        m_onUnsubscribeInterfaces.run(holder);
                        prof = holder.value;
                    }
                }
                onUnsubscribeInterfaces(getName(), prof);
                unsubscribeInterfaces(prof);

                synchronized (m_onDisconnected_mutex){
                    if (m_onDisconnected != null) {
                        ConnectorProfileHolder holder 
                            = new ConnectorProfileHolder(prof);
                        m_onDisconnected.run(holder);
                        prof = holder.value;
                    }
                }

                ConnectorProfileListHolder holder 
                    = new ConnectorProfileListHolder(
                        this.m_profile.connector_profiles);

                CORBA_SeqUtil.erase(holder, index);
                this.m_profile.connector_profiles = holder.value;
                onDisconnected(getName(), prof, retval);
                return retval;
           }
        }
    }

    /**
     * {@.ja [CORBA interface] Port の全接続を解除する}
     * {@.en [CORBA interface] Disconnect the All Ports}
     *
     * <p>
     * {@.ja このオペレーションはこの Port に関連した全ての接続を解除する。}
     * {@.en This operation destroys all connections associated with this Port.}
     *
     * @return 
     *   {@.ja ReturnCode_t 型のリターンコード}
     *   {@.en ReturnCode_t The return code of ReturnCode_t type.}
     *
     */
    public ReturnCode_t disconnect_all() {

        rtcout.println(Logbuf.TRACE, "disconnect_all()");
        ConnectorProfileListHolder plist = null;
        synchronized (m_profile_mutex) {
            plist = 
              new ConnectorProfileListHolder(this.m_profile.connector_profiles);
        }
        ReturnCode_t retcode = ReturnCode_t.RTC_OK;
        int len = plist.value.length;
        rtcout.println(Logbuf.DEBUG, "disconnecting "+len+" connections.");
        for (int i=0; i < len; ++i) {
            ReturnCode_t tmpret;
            tmpret = this.disconnect(plist.value[i].connector_id);
            if (!tmpret.equals(ReturnCode_t.RTC_OK)) { 
                retcode = tmpret;
            }
        }
    
        return retcode;

    }

    /**
     * {@.ja Port の全てのインターフェースを activates する}
     * {@.en Activate all Port interfaces}
     *
     * <p>
     * {@.ja Port に登録されている全てのインターフェースを activate する。}
     * {@.en This operation activate all interfaces that is registered in the
     * ports.}
     */
    public abstract void activateInterfaces();

    /**
     * {@.ja 全ての Port のインターフェースを deactivates する}
     * {@.en Deactivate all Port interfaces}
     * <p>
     * {@.ja Port に登録されている全てのインターフェースを deactivate する。}
     * {@.en This operation deactivate all interfaces that is registered in the
     * ports.}
     */
    public abstract void deactivateInterfaces();
    /**
     * {@.ja Port の名前を設定する}
     * {@.en Set the name of this Port}
     * <p>
     * {@.ja Port の名前を設定する。この名前は Port が保持する PortProfile.name
     * に反映される。}
     * {@.en This operation sets the name of this Port. The given Port's name is
     * applied to Port's PortProfile.name.}
     *
     * @param name 
     *   {@.ja Port の名前}
     *   {@.en The name of this Port.}
     */
    public void setName(final String name) {
        synchronized (this.m_profile) {
            this.m_profile.name = name;
        }
    }

    /**
     * {@.ja Port の名前を取得する}
     * {@.en Get the name of this Port}
     * <p>
     * {@.ja Port の名前を取得する。}
     * {@.en This operation returns the name of this Port.}
     *
     * @return 
     *   {@.ja Port の名前}
     *   {@.en The name of this Port.}
     */
    public final String getName() {
        rtcout.println(Logbuf.TRACE, "getName() = "+m_profile.name);
        return m_profile.name;
    }

    /**
     * {@.ja PortProfileを取得する}
     * {@.en Get the PortProfile of the Port}
     * <p>
     * {@.ja Portが保持する PortProfile の const 参照を返す。}
     * {@.en This operation returns const reference of the PortProfile.}
     *
     * @return 
     *   {@.ja この Port の PortProfile}
     *   {@.en PortProfile of the Port}
     */
    public final PortProfile getProfile() {
        synchronized (this.m_profile) {
            return this.m_profile;
        }
    }

    /**
     * {@.ja Port のオブジェクト参照を設定する}
     * {@.en Set the object reference of this Port}
     * <p>
     * {@.ja このオペレーションは Port の PortProfile にこの Port 自身の
     * オブジェクト参照を設定する。}
     * {@.en This operation sets the object reference itself
     * to the Port's PortProfile.}
     *
     * @param port_ref 
     *   {@.ja この Port のオブジェクト参照}
     *   {@.en port_ref The object reference of this Port.}
     */
    public void setPortRef(PortService port_ref) {
        synchronized (this.m_profile) {
            m_profile.port_ref = port_ref;
        }
    }

    /**
     * {@.ja Port のオブジェクト参照を取得する}
     * {@.en Get the object reference of this Port}
     * <p>
     * {@.ja このオペレーションは Port の PortProfile が保持している
     * この Port 自身のオブジェクト参照を取得する。}
     * {@.en This operation returns the object reference
     * that is stored in the Port's PortProfile.}
     *
     * @return 
     *   {@.ja この Port のオブジェクト参照}
     *   {`.en The object reference of this Port.}
     */
    public PortService getPortRef() {
        synchronized (this.m_profile) {
            return m_profile.port_ref;
        }
    }

    /**
     * {@.ja Port の owner の RTObject を指定する}
     * {@.en Set the owner RTObject of the Port}
     *
     * <p>
     * {@.ja このオペレーションは Port の PortProfile.owner を設定する。}
     * {@.en This operation sets the owner RTObject of this Port.}
     * </p>
     * @param owner 
     *   {@.ja この Port を所有する RTObject の参照}
     *   {@.en The owner RTObject's reference of this Port}
     *
     */
    public void setOwner(RTObject owner) {


        RTC.ComponentProfile prof = owner.get_component_profile();
        m_ownerInstanceName = prof.instance_name;
        rtcout.println(Logbuf.TRACE, "setOwner("+m_ownerInstanceName+")");
        synchronized (this.m_profile) {
            String portname = m_profile.name;
            String[] port = portname.split("\\.");
            // Now Port name is <instance_name>.<port_name>. 
            portname = m_ownerInstanceName +"."+ port[port.length-1];
            this.m_profile.owner = (RTObject)owner._duplicate();
            this.m_profile.name = portname;
        }
    }
    //============================================================
    // callbacks
    //============================================================
    /**
     * <p> Setting callback called on publish interfaces </p>
     *
     * <p>This operation sets a functor that is called after publishing
     * interfaces process when connecting between ports. </p>
     *
     * <p>Since the ownership of the callback functor object is owned by
     * the caller, it has the responsibility of object destruction. </p>
     * 
     * <p>The callback functor is called after calling
     * publishInterfaces() that is virtual member function of the
     * PortBase class with an argument of ConnectorProfile type that
     * is same as the argument of publishInterfaces() function.
     * Although by using this functor, you can modify the ConnectorProfile
     * published by publishInterfaces() function, the modification
     * should be done carefully for fear of causing connection
     * inconsistency.</p>
     *
     * @param on_publish ConnectionCallback's subclasses
     *
     */
    public void setOnPublishInterfaces(ConnectionCallback on_publish) {
        synchronized (m_onPublishInterfaces_mutex){
            m_onPublishInterfaces = on_publish;
        }
    }

    /**
     * <p> Setting callback called on publish interfaces </p>
     *
     * <p>This operation sets a functor that is called before subscribing
     * interfaces process when connecting between ports.</p>
     *
     * <p>Since the ownership of the callback functor object is owned by
     * the caller, it has the responsibility of object destruction.</p>
     * 
     * <p>The callback functor is called before calling
     * subscribeInterfaces() that is virtual member function of the
     * PortBase class with an argument of ConnectorProfile type that
     * is same as the argument of subscribeInterfaces() function.
     * Although by using this functor, you can modify ConnectorProfile
     * argument for subscribeInterfaces() function, the modification
     * should be done carefully for fear of causing connection
     * inconsistency.</p>
     *
     * @param on_subscribe ConnectionCallback's subclasses
     *
     */
    public void setOnSubscribeInterfaces(ConnectionCallback on_subscribe) {
        synchronized (m_onSubscribeInterfaces_mutex){
            m_onSubscribeInterfaces = on_subscribe;
        }
    }

    /**
     * <p> Setting callback called on connection established </p>
     *
     * <p>This operation sets a functor that is called when connection
     * between ports established.</p>
     *
     * <p>Since the ownership of the callback functor object is owned by
     * the caller, it has the responsibility of object destruction.</p>
     * 
     * <p>The callback functor is called only when notify_connect()
     * function successfully returns. In case of error, the functor
     * will not be called.</p>
     *
     * <p>Since this functor is called with ConnectorProfile argument
     * that is same as out-parameter of notify_connect() function, you
     * can get all the information of published interfaces of related
     * ports in the connection.  Although by using this functor, you
     * can modify ConnectorProfile argument for out-paramter of
     * notify_connect(), the modification should be done carefully for
     * fear of causing connection inconsistency.</p>
     *
     * @param on_connected ConnectionCallback's subclasses
     *
     */
    public void setOnConnected(ConnectionCallback on_connected) {
        synchronized (m_onConnected_mutex){
            m_onConnected = on_connected;
        }
    }

    /**
     *
     * <p> Setting callback called on unsubscribe interfaces </p>
     *
     * <p>This operation sets a functor that is called before unsubscribing
     * interfaces process when disconnecting between ports.</p>
     *
     * <p>Since the ownership of the callback functor object is owned by
     * the caller, it has the responsibility of object destruction.</p>
     * 
     * <p>The callback functor is called before calling
     * unsubscribeInterfaces() that is virtual member function of the
     * PortBase class with an argument of ConnectorProfile type that
     * is same as the argument of unsubscribeInterfaces() function.
     * Although by using this functor, you can modify ConnectorProfile
     * argument for unsubscribeInterfaces() function, the modification
     * should be done carefully for fear of causing connection
     * inconsistency.</p>
     *
     * @param on_unsubscribe ConnectionCallback's subclasses
     *
     */
    public void setOnUnsubscribeInterfaces(ConnectionCallback on_unsubscribe) {
        synchronized (m_onUnsubscribeInterfaces_mutex){
            m_onUnsubscribeInterfaces = on_unsubscribe;
        }
    }

    /**
     *
     * <p> Setting callback called on disconnected </p>
     *
     * <p>This operation sets a functor that is called when connection
     * between ports is destructed.</p>
     *
     * <p>Since the ownership of the callback functor object is owned by
     * the caller, it has the responsibility of object destruction.</p>
     * 
     * <p>The callback functor is called just before notify_disconnect()
     * that is disconnection execution function returns.</p>
     *
     * <p>This functor is called with argument of corresponding
     * ConnectorProfile.  Since this ConnectorProfile will be
     * destructed after calling this functor, modifications never
     * affect others.</p>
     *
     * @param on_disconnected ConnectionCallback's subclasses
     *
     */
    public void setOnDisconnected(ConnectionCallback on_disconnected){
        synchronized (m_onDisconnected_mutex){
            m_onDisconnected = on_disconnected;
        }
    }

    public void setOnConnectionLost(ConnectionCallback on_connection_lost) {
        synchronized (m_onConnectionLost_mutex){
            m_onConnectionLost = on_connection_lost;
        }
    }

    /**
     * {@.ja PortConnectListeners のホルダをセットする}
     * {@.en Setting PortConnectListener holder}
     * <p>
     * {@.ja ポートの接続に関するリスナ群を保持するホルダクラスへのポインタを
     * セットする。この関数は通常親のRTObjectから呼ばれ、RTObjectが持つ
     * ホルダクラスへのポインタがセットされる。}
     * {@.en This operation sets a functor that is called when connection
     * of this port does lost. }
     *
     * @param portconnListeners 
     *   {@.ja PortConnectListeners オブジェクトのポインタ}
     *   {@.en a pointer to ConnectionCallback's subclasses}
     *
     */
    public void setPortConnectListenerHolder(PortConnectListeners portconnListeners){
        m_portconnListeners = portconnListeners;
    }

    /**
     * <p>Interface情報を公開します。
     * このメソッドは、notify_connect()処理シーケンスの始めに呼び出されるテンプレートメソッドです。</p>
     * 
     * <p>notify_connect()内では、以下の順にテンプレートメソッドが呼び出されて接続処理が行われます。
     * <ol>
     * <li>publishInterfaces()</li>
     * <li>connectNext()</li>
     * <li>subscribeInterfaces()</li>
     * <li>updateConnectorProfile()</li>
     * </ol>
     * 
     * <p>具象Portクラスでは、このメソッドをオーバーライドし、引数として与えられた
     * ConnectorProfileに従って処理を行い、パラメータが不適切であれば、ReturnCode_t型の
     * エラーコードを返します。</p>
     * 
     * <p>通常、publishInterafaces()内においては、このポートに属するインターフェースに関する情報を
     * ConnectorProfileに対して適切に設定し、他のポートに通知しなければなりません。
     * また、このメソッドが呼び出される段階では、他のポートのInterfaceに関する情報は
     * すべては含まれていないので、他のポートのInterfaceを取得する処理はsubscribeInterfaces()内で
     * 行われるべきです。</p>
     * 
     * <p>このメソッドは、新規のコネクタIDに対しては接続の生成が、既存のコネクタIDに対しては更新が
     * 適切に行われる必要があります。</p>
     *
     * @param connector_profile 接続プロファイル
     * @return ReturnCode_t型の戻り値
     */
    protected abstract ReturnCode_t publishInterfaces(ConnectorProfileHolder connector_profile);

    /**
     * {@.ja 次の Port に対して notify_connect() をコールする}
     * {@.en Call notify_connect() of the next Port}
     *
     * <p>
     * {@.ja ConnectorProfile の port_ref 内に格納されている Port のオブジェクト
     * リファレンスのシーケンスの中から、自身の Port の次の Port に対して
     * notify_connect() をコールする。}
     * {@.en This operation calls the notify_connect() of the next Port's 
     * that stored in ConnectorProfile's port_ref sequence.}
     * </p>
     *
     * @param connector_profile 
     *   {@.ja 接続に関するプロファイル情報}
     *   {@.en The connection profile information}
     *
     * @return 
     *   {@.ja ReturnCode_t 型のリターンコード}
     *   {@.en The return code of ReturnCode_t type.}
     *
     */
    protected 
    ReturnCode_t connectNext(ConnectorProfileHolder connector_profile) {

        PortServiceListHolder portsHolder 
            = new PortServiceListHolder(connector_profile.value.ports);
        int index 
            = CORBA_SeqUtil.find(portsHolder, 
                                    new find_port_ref(this.m_profile.port_ref));
        connector_profile.value.ports = portsHolder.value;
        
        if (index < 0) {
            return ReturnCode_t.BAD_PARAMETER;
        }
        
        if (++index < connector_profile.value.ports.length) {
            PortService p = connector_profile.value.ports[index];
            ReturnCode_t rc = p.notify_connect(connector_profile);
            return rc;
        }
        
        return ReturnCode_t.RTC_OK;
    }

    /**
     * <p>当該ポートの次のポートに対して接続解除通知を行います。</p>
     * 
     * <p>ConnectorProfile内に設定されているPortのシーケンスを調べて、
     * 当該Portの次のPortを特定し、そのPortに対してnotify_disconnect()を呼び出します。</p>
     * 
     * @param connector_profile 接続プロファイル
     */
    protected ReturnCode_t disconnectNext(ConnectorProfile connector_profile) {

        PortServiceListHolder holder = 
                          new PortServiceListHolder(connector_profile.ports);
        int index = 
                CORBA_SeqUtil.find(holder, 
                                   new find_port_ref(this.m_profile.port_ref));

        connector_profile.ports = holder.value;
        
        if (index < 0) {
            return ReturnCode_t.BAD_PARAMETER;
        }
        if (index == connector_profile.ports.length - 1) {
            return ReturnCode_t.RTC_OK;
        }
        
        int len = connector_profile.ports.length;

        ++index;
        for (int i=index; i < len; ++i) {
            PortService p = connector_profile.ports[i];
            try {
                return p.notify_disconnect(connector_profile.connector_id);
            }
            catch (SystemException e) {
                rtcout.println(Logbuf.WARN, 
                               "Exception caught: minor code."+e.minor);
                continue;
            } 
            catch (Exception e) {
                rtcout.println(Logbuf.WARN, 
                               "Unknown exception caught.");
                continue;
            }
        }
        
        return ReturnCode_t.RTC_ERROR;
    }

    /**
     * <p>Interface 情報を公開します。</p>
     *
     * <p>このメソッドは、notify_connect()処理シーケンスの中間に呼び出されるテンプレートメソッドです。
     * notify_connect()では、
     * <ol>
     * <li>publishInterfaces()</li>
     * <li>connectNext()</li>
     * <li>subscribeInterfaces()</li>
     * <li>updateConnectorProfile()</li>
     * </ol>
     * の順にprotectedメソッドが呼び出されて接続処理が行われます。
     * </p>
     * 
     * <p>このメソッドの処理は、新規のconnector_idに対しては接続の生成が適切に行われる必要があります。<br />
     * また、既存のconnector_idに対しては更新が適切に行われる必要があります。</p>
     *
     * @param connector_profile 接続プロファイル情報
     * @return ReturnCode_t 戻り値
     */
    protected abstract ReturnCode_t subscribeInterfaces(
            final ConnectorProfileHolder connector_profile);

    /**
     * <p>Interfaceの接続を解除します。</p>
     *
     * <p>このメソッドは、notify_disconnect()処理シーケンスの終わりに呼び出されるテンプレートメソッドです。
     * notify_disconnect()では、
     * <ol>
     * <li>disconnectNext()</li>
     * <li>unsubscribeInterfaces()</li>
     * <li>eraseConnectorProfile()</li>
     * </ol>
     * の順にprotectedメソッドが呼び出されて接続解除処理が行われます。
     * </p>
     * 
     * <p>具象クラスでは、このメソッドをオーバーライドし、引数として
     * 与えられた ConnectorProfile に従い接続解除処理を行います。</p>
     *
     * @param connector_profile 接続プロファイル情報
     */
    protected abstract void unsubscribeInterfaces(final ConnectorProfile connector_profile);

    /**
     * <p>指定されたConnectorProfileオブジェクト内のconnector_idメンバが空かどうか判定します。</p>
     * 
     * @return 空であればtrueを、さもなくばfalseを返します。
     */
    protected boolean isEmptyId(final ConnectorProfile connector_profile) {
        return connector_profile.connector_id.length() == 0;
    }

    /**
     * <p>UUIDを生成します。<p>
     * 
     * @return 生成されたUUID
     */
    protected String getUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * <p>UUIDを生成し、指定されたConnectorProfileオブジェクトのconnector_idメンバに設定します。</p>
     * 
     * @param connector_profile 設定先となるConnectorProfileオブジェクト
     */
    protected void setUUID(ConnectorProfileHolder connector_profile) {
        connector_profile.value.connector_id = getUUID();
        assert (connector_profile.value.connector_id.length() != 0);
    }

    /**
     * <p>指定された接続IDを持つ接続プロファイルが、当該ポートのPortProfileに設定されている
     * ConnectorProfileオブジェクトシーケンス内に存在するかどうかを判定します。</p>
     * 
     * @return 存在すればtrueを、さもなくばfalseを返します。
     */
    protected boolean isExistingConnId(final String id) {
        ConnectorProfileListHolder holder =
            new ConnectorProfileListHolder(this.m_profile.connector_profiles);
        int index = CORBA_SeqUtil.find(holder, new find_conn_id(id));
        this.m_profile.connector_profiles = holder.value;
        
        return (index >= 0);
    }
    
    /**
     * <p>指定された接続IDを持つ接続プロファイルが、当該ポートのPortProfileに設定されている
     * ConnectorProfileオブジェクトシーケンス内に存在するかどうかを調べ、存在する場合は
     * そのConnectorProfileオブジェクトが取得されます。</p>
     * 
     * @param id 接続ID
     * @return 指定した接続IDを持つConnectorProfileオブジェクトが見つかった場合は、
     * そのオブジェクトを返します。<br />
     * 見つからない場合は、空のConnectorProfileオブジェクトを返します。
     */
    protected ConnectorProfile findConnProfile(final String id) {

        ConnectorProfileListHolder holder =
            new ConnectorProfileListHolder(this.m_profile.connector_profiles);
        int index = CORBA_SeqUtil.find(holder, new find_conn_id(id));
        this.m_profile.connector_profiles = holder.value;
        
        if (index < 0) {
            return ConnectorProfileFactory.create();
        }
        
        return this.m_profile.connector_profiles[index];
    }

    /**
     * <p>指定された接続IDを持つ接続プロファイルが、当該ポートのPortProfileに設定されている
     * ConnectorProfileオブジェクトシーケンス内に存在するかどうかを調べ、存在する場合は
     * そのConnectorProfileオブジェクトのシーケンス内でのインデクスが取得されます。</p>
     * 
     * @return 指定した接続IDを持つConnectorProfileオブジェクトが見つかった場合は、
     * そのオブジェクトのシーケンス内インデクスを返します。<br />
     * 見つからない場合は、-1を返します。
     */
    protected int findConnProfileIndex(final String id) {

        ConnectorProfileListHolder holder =
            new ConnectorProfileListHolder(this.m_profile.connector_profiles);
        int index = CORBA_SeqUtil.find(holder, new find_conn_id(id));
        this.m_profile.connector_profiles = holder.value;
        
        return index;
    }

    /**
     * <p>指定された接続プロファイルを、当該ポートのPortProfileに追加します。
     * 同一の接続IDを持つ接続プロファイルがすでにある場合は、その内容を上書きします。</p>
     * 
     * @param connector_profile 追加もしくは更新する接続プロファイル
     */
    protected void updateConnectorProfile(final ConnectorProfile connector_profile) {
        
        ConnectorProfileListHolder cprof_list =
            new ConnectorProfileListHolder(this.m_profile.connector_profiles);
        
        int index = CORBA_SeqUtil.find(
                cprof_list, new find_conn_id(connector_profile.connector_id));
        this.m_profile.connector_profiles = cprof_list.value;
        
        
        if (index < 0) {
            CORBA_SeqUtil.push_back(cprof_list, connector_profile);
            this.m_profile.connector_profiles = cprof_list.value;

        } else {
            this.m_profile.connector_profiles[index] = connector_profile;
        }
    }
    
    /**
     * <p>指定された接続IDに対応する接続プロファイルを、当該ポートのPortProfileから削除します。</p>
     * 
     * @param connector_id 削除対象となる接続プロファイルの接続ID
     * @return 削除された場合はtrueを返します。<br />
     * また、指定された接続IDの接続プロファイルが見つからない場合はfalseを返します。
     */
    protected boolean eraseConnectorProfile(final String connector_id) {
        
        synchronized (this.m_profile) {
            
            ConnectorProfileListHolder cprof_list =
                new ConnectorProfileListHolder(this.m_profile.connector_profiles);

            int index = CORBA_SeqUtil.find(cprof_list, new find_conn_id(connector_id));
            this.m_profile.connector_profiles = cprof_list.value;
            
            if (index < 0) return false;
            
            CORBA_SeqUtil.erase(cprof_list, index);
            this.m_profile.connector_profiles = cprof_list.value;
            
            return true;
        }
    }
    
    /**
     * <p>当該ポートが持つPortProfile内に含まれるPortInterfaceProfileメンバに、
     * 引数で指定された情報を追加します。この情報は、get_port_profile()メソッドによって
     * 得られるPortProfileにうち、PortInterfaceProfileの値を変更するのみであり、
     * 実際にインタフェースを提供したり要求したりする場合には、サブクラス側で
     * publishInterface(), subscribeInterface()などのメソッドを適切にオーバライドし、
     * インタフェースの提供や要求処理を行う必要があります。</p>
     * 
     * <p>なお、インタフェース（のインスタンス）名は、ポート内で一意でなければなりません。</p>
     *
     * @param instance_name インタフェースのインスタンス名
     * @param type_name インタフェースの型名称
     * @param polarity インタフェースの属性（提供インタフェースまたは要求インタフェース）
     * @return 正常に登録できた場合はtrueを返します。
     * また、同名のインタフェースがすでに登録済みの場合はfalseを返します。
     */
    protected boolean appendInterface(final String instance_name,
            final String type_name, PortInterfacePolarity polarity) {
        
        PortInterfaceProfileListHolder port_if_prof_list =
            new PortInterfaceProfileListHolder(this.m_profile.interfaces);
        
        int index = CORBA_SeqUtil.find(
                port_if_prof_list, new find_interface(instance_name, polarity));
        this.m_profile.interfaces = port_if_prof_list.value;
        
        if (index >= 0) return false;
        
        // setup PortInterfaceProfile
        PortInterfaceProfile prof = new PortInterfaceProfile(instance_name, type_name, polarity);
        CORBA_SeqUtil.push_back(port_if_prof_list, prof);
        this.m_profile.interfaces = port_if_prof_list.value;
        
        return true;
    }
    
    /**
     * <p>指定されたインスタンス名と属性を持つインタフェースを、当該ポートが持つPortProfile内の
     * PortInterfaceProfileから削除します。</p>
     * 
     * @param instance_name 削除対象インタフェースのインスタンス名
     * @param polarity 削除対象インタフェースの属性
     * @return 正常に削除できた場合はtrueを返します。
     * また、該当するインタフェースが見つからない場合はfalseを返します。
     */
    protected boolean deleteInterface(final String instance_name, PortInterfacePolarity polarity) {

        PortInterfaceProfileListHolder port_if_prof_list =
            new PortInterfaceProfileListHolder(this.m_profile.interfaces);

        int index = CORBA_SeqUtil.find(port_if_prof_list, new find_interface(instance_name, polarity));
        this.m_profile.interfaces = port_if_prof_list.value;
        
        if (index < 0) return false;
        
        CORBA_SeqUtil.erase(port_if_prof_list, index);
        this.m_profile.interfaces = port_if_prof_list.value;
        
        return true;
    }
    
    /**
     * <p>当該ポートが持つPortProfile内のpropertiesメンバに、
     * 指定されたキーと値を持つNameValueオブジェクトを追加します。</p>
     * 
     * @param key キー
     * @param value 値
     */
    protected <T> void addProperty(final String key, T value, Class<T> klass) {
        NVListHolder holder = new NVListHolder(this.m_profile.properties);
        CORBA_SeqUtil.push_back(holder, NVUtil.newNV(key, value, klass));
        this.m_profile.properties = holder.value;
    }

    /**
     * <p>当該ポートに関連付けられているPortProfileオブジェクトです。</p>
     */
    protected PortProfile m_profile = new PortProfile();
    protected static String m_profile_mutex = new String();
    protected static String m_connectorsMutex = new String();

    /**
     * <p>当該ポートのCORBAオブジェクト参照です。</p>
     */
    protected PortService m_objref;

    /**
     * <p>指定された接続IDを持つ接続プロファイルを検索するためのヘルパクラスです。</p>
     */
    protected class find_conn_id implements equalFunctor {
        /**
         * <p>コンストラクタです。</p>
         * 
         * @param connector_id 検索対象の接続ID
         */
        public find_conn_id(final String connector_id) {
            this.m_connector_id = connector_id;
        }
        
        /**
         * <p>検索対象の接続IDを持つ接続プロファイルか否かを判定します。</p>
         * 
         * @param elem 判定対象となるオブジェクト
         * @return 検索対象であればtrueを、さもなくばfalseを返します。
         */
        public boolean equalof(final Object elem) {
            return equalof((ConnectorProfile) elem);
        }
        
        /**
         * <p>検索対象の接続IDを持つ接続プロファイルか否かを判定します。</p>
         * 
         * @param cprof 判定対象となる接続プロファイル
         * @return 検索対象であればtrueを、さもなくばfalseを返します。
         */
        public boolean equalof(final ConnectorProfile cprof) {
            return this.m_connector_id.endsWith(cprof.connector_id);
        }
        
        public String m_connector_id;
    }
    
    /**
     * <p>指定されたPortオブジェクトと同じCORBAオブジェクト参照を持つPortオブジェクトを
     * 検索するためのヘルパクラスです。</p>
     */
    protected class find_port_ref implements equalFunctor {
        
        /**
         * <p>コンストラクタです。</p>
         * 
         * @param port 判定基準となるCORBAオブジェクト参照を持つPortオブジェクト
         */
        public find_port_ref(PortService port) {
            this.m_port = port;
        }
        
        /**
         * <p>検索対象となるCORBAオブジェクト参照を持つPortか否かを判定します。</p>
         * 
         * @param elem 判定対象となるオブジェクト
         * @return 検索対象である場合はtrueを、さもなくばfalseを返します。
         */
        public boolean equalof(final Object elem) {
            return equalof((PortService) elem);
        }
        
        /**
         * <p>検索対象となるCORBAオブジェクト参照を持つPortか否かを判定します。</p>
         * 
         * @param port 判定対象となるPortオブジェクト
         * @return 検索対象である場合はtrueを、さもなくばfalseを返します。
         */
        public boolean equalof(final PortService port) {
            return this.m_port._is_equivalent(port);
        }
        
        public PortService m_port;
    }
    
    /**
     * <p>ポート接続のためのヘルパクラスです。</p>
     */
    protected class connect_func implements operatorFunc {
        public PortService m_port_ref;
        public ConnectorProfileHolder m_connector_profile;
        public ReturnCode_t m_return_code;
        
        /**
         * <p>コンストラクタです。</p>
         */
        public connect_func() {
        }

        /**
         * <p>コンストラクタです。</p>
         * 
         * @param p 接続を行うポート
         * @param prof コネクタ・プロファイル
         */
        public connect_func(PortService p, ConnectorProfileHolder prof) {
            this.m_port_ref = p;
            this.m_connector_profile = prof;
            this.m_return_code = ReturnCode_t.RTC_OK;
        }

        /**
         * <p>接続を行います。</p>
         * 
         * @param elem 接続プロファイルオブジェクト
         */
        public void operator(Object elem) {
            if( ! this.m_port_ref._is_equivalent((PortService)elem) ) {
                ReturnCode_t retval;
                retval = ((PortService)elem).notify_connect(this.m_connector_profile);
                if( !retval.equals(ReturnCode_t.RTC_OK) ) {
                    this.m_return_code = retval;
                }
            }
        }
        
    }

    /**
     * <p>ポート接続解除のためのヘルパクラスです。</p>
     */
    protected class disconnect_func implements operatorFunc {
        public PortService m_port_ref;
        public ConnectorProfileHolder m_connector_profile;
        public ReturnCode_t m_return_code;
        
        /**
         * <p>コンストラクタです。</p>
         */
        public disconnect_func() {
            this.m_return_code = ReturnCode_t.RTC_OK;
        }

        /**
         * <p>コンストラクタです。</p>
         * 
         * @param p 接続を行うポート
         * @param prof コネクタ・プロファイル
         */
        public disconnect_func(PortService p, ConnectorProfileHolder prof) {
            this.m_port_ref = p;
            this.m_connector_profile = prof;
            this.m_return_code = ReturnCode_t.RTC_OK;
        }

        /**
         * <p>接続解除を行います。</p>
         * 
         * @param elem 接続プロファイルオブジェクト
         */
        public void operator(Object elem) {
            if( ! this.m_port_ref._is_equivalent((PortService)elem) ) {
                ReturnCode_t retval;
                retval = ((PortService)elem).disconnect(this.m_connector_profile.value.connector_id);
                if( !retval.equals(ReturnCode_t.RTC_OK) ) {
                    this.m_return_code = retval;
                }
            }
        }
        
    }
    /**
     * <p>ポート接続解除のためのヘルパクラスです。</p>
     */
    protected class disconnect_all_func implements operatorFunc {
        
        /**
         * <p>コンストラクタです。</p>
         * 
         * @param port 接続解除を行うポート
         */
        public disconnect_all_func(PortBase port) {
            this.m_return_code = ReturnCode_t.RTC_OK;
            this.m_port = port;
        }
        
        /**
         * <p>接続解除を行います。</p>
         * 
         * @param elem 接続プロファイルオブジェクト
         */
        public void operator(Object elem) {
            operator((ConnectorProfile) elem);
        }
        
        /**
         * <p>接続解除を行います。</p>
         * 
         * @param cprof 接続プロファイルオブジェクト
         */
        public void operator(ConnectorProfile cprof) {
            ReturnCode_t retval = this.m_port.disconnect(cprof.connector_id);
            if (! retval.equals(ReturnCode_t.RTC_OK)) {
                this.m_return_code = retval;
            }
        }
        
        public ReturnCode_t m_return_code;
        public PortBase m_port;
    }

    /**
     * <p>指定されたインスタンス名と属性を持つインタフェースを検索するためのヘルパクラスです。</p>
     */
    protected class find_interface implements equalFunctor {

        /**
         * <p>コンストラクタです。</p>
         * 
         * @param instance_name 検索対象インタフェースのインスタンス名
         * @param polarity 検索対象インタフェースの属性
         */
        public find_interface(final String instance_name, PortInterfacePolarity polarity) {
            this.m_instance_name = instance_name;
            this.m_polarity = polarity;
        }

        /**
         * <p>検索対象となるインタフェースか否かを判定します。</p>
         * 
         * @param elem 判定対象のオブジェクト
         * @return 検索対象のインタフェースであればtrueを、さもなくばfalseを返します。
         */
        public boolean equalof(final Object elem) {
            return equalof((PortInterfaceProfile) elem);
        }
        
        /**
         * <p>検索対象となるインタフェースか否かを判定します。</p>
         * 
         * @param prof 判定対象のPortInterfaceProfileオブジェクト
         * @return 検索対象のインタフェースであればtrueを、さもなくばfalseを返します。
         */
        public boolean equalof(final PortInterfaceProfile prof) {
            return this.m_instance_name.equals(prof.instance_name)
                && this.m_polarity.equals(prof.polarity);
        }

        private String m_instance_name;
        private PortInterfacePolarity m_polarity;
    }
    /**
     * <p> Add NameValue data to PortProfile's properties </p>
     *
     * <p> Add NameValue data to PortProfile's properties.
     * Type of additional data is specified by ValueType. </p>
     *
     * @param key The name of properties
     * @param value The value of properties
     *
     */
    protected void appendProperty(final String key, final String value) {
        NVListHolder holder = new NVListHolder(this.m_profile.properties);
        NVUtil.appendStringValue(holder, key, value);
        this.m_profile.properties = holder.value;
    }
    /**
     *
     */
    protected void updateConnectors() {
        Vector<String> connector_ids = new Vector<String>();
        {
            synchronized (this.m_profile) {
                RTC.ConnectorProfile[] clist = m_profile.connector_profiles;
  
                for (int i=0; i < clist.length; ++i) {
                    if (!checkPorts(clist[i].ports)) {
                        String id = clist[i].connector_id;
                        connector_ids.add(id);
                        rtcout.println(Logbuf.WARN,"Dead connection:"+id);
                    }
                }
            }
        }
  

        Iterator it = connector_ids.iterator();
        while (it.hasNext()) {
            this.disconnect((String)it.next());
        }
    }
    /**
     *
     */
    protected boolean checkPorts(RTC.PortService[] ports) {
        for (int i=0, len=ports.length; i < len; ++i) {
            try {
                if (ports[i]._non_existent()) {
                    rtcout.println(Logbuf.WARN,"Dead Port reference detected.");
                    return false;
                }
            }
            catch (Exception ex) {
                return false;
            }
        }
        return true;
    }

    protected void onNotifyConnect(final String portname,
                                ConnectorProfile profile) {
        if (m_portconnListeners != null) {
            m_portconnListeners.portconnect_[PortConnectListenerType.ON_NOTIFY_CONNECT].notify(portname, profile);
        }
    }

    protected void onNotifyDisconnect(final String portname,
                                   RTC.ConnectorProfile profile) {
        if (m_portconnListeners != null) {
            m_portconnListeners.portconnect_[PortConnectListenerType.ON_NOTIFY_DISCONNECT].notify(portname, profile);
        }
    }
    protected void onUnsubscribeInterfaces(final String portname,
                                        RTC.ConnectorProfile profile) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnect_[PortConnectListenerType.ON_UNSUBSCRIBE_INTERFACES].notify(portname, profile);
        }
    }

    protected void onPublishInterfaces(final String portname,
                                    RTC.ConnectorProfile profile,
                                    ReturnCode_t ret) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnret_[PortConnectRetListenerType.ON_PUBLISH_INTERFACES].notify(portname, profile, ret);
        }
    }

    protected void onConnectNextport(final String portname,
                                  RTC.ConnectorProfile profile,
                                  ReturnCode_t ret) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnret_[PortConnectRetListenerType.ON_CONNECT_NEXTPORT].notify(portname, profile, ret);
        }
    }

    protected void onSubscribeInterfaces(final String portname,
                                      RTC.ConnectorProfile profile,
                                      ReturnCode_t ret) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnret_[PortConnectRetListenerType.ON_SUBSCRIBE_INTERFACES].notify(portname, profile, ret);
        }
    }

    protected void onConnected(final String portname,
                            RTC.ConnectorProfile profile,
                            ReturnCode_t ret) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnret_[PortConnectRetListenerType.ON_CONNECTED].notify(portname, profile, ret);
        }
    }

    protected void onDisconnectNextport(final String portname,
                                 RTC.ConnectorProfile profile,
                                 ReturnCode_t ret) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnret_[PortConnectRetListenerType.ON_DISCONNECT_NEXT].notify(portname, profile, ret);
        }
    }

    protected void onDisconnected(final String portname,
                               RTC.ConnectorProfile profile,
                               ReturnCode_t ret) {
      if (m_portconnListeners != null) {
          m_portconnListeners.
            portconnret_[PortConnectRetListenerType.ON_DISCONNECTED].notify(portname, profile, ret);
        }
    }

  
    /**
     * <p> Publish interface information </p>
     *
     * Publish interface information.
     *
     * @return The return code of ReturnCode_t type
     */
    protected ReturnCode_t _publishInterfaces() {
        if(!(m_connectionLimit < 0)) {
            if(m_connectionLimit<=m_profile.connector_profiles.length) {
                rtcout.println(Logbuf.PARANOID, 
                    "Connected number has reached the limitation.");
                rtcout.println(Logbuf.PARANOID, 
                    "Can connect the port up to "+m_connectionLimit+" ports.");
                rtcout.println(Logbuf.PARANOID, 
                    m_profile.connector_profiles.length+" connectors are existing");
                return ReturnCode_t.RTC_ERROR;
             }
        }
        return ReturnCode_t.RTC_OK;
    }

    /**
     * <p> Set the maximum number of connections </p>
     *
     * @param limit_value The maximum number of connections
     *
     */
    protected void setConnectionLimit(int limit_value) {
        m_connectionLimit = limit_value;
    }

    protected Logbuf rtcout;
    /**
     * <p>Callback functor objects</p>
     */
    private final Object m_onPublishInterfaces_mutex = new Object();
    protected ConnectionCallback m_onPublishInterfaces;

    private final Object m_onSubscribeInterfaces_mutex = new Object();
    protected ConnectionCallback m_onSubscribeInterfaces;

    private final Object m_onConnected_mutex = new Object();
    protected ConnectionCallback m_onConnected;

    private final Object m_onUnsubscribeInterfaces_mutex = new Object();
    protected ConnectionCallback m_onUnsubscribeInterfaces;

    private final Object m_onDisconnected_mutex = new Object();
    protected ConnectionCallback m_onDisconnected;

    protected final Object m_onConnectionLost_mutex = new Object();
    protected ConnectionCallback m_onConnectionLost;
    /**
     * {@.ja PortConnectListenerホルダ}
     * {@.en PortConnectListener holder}
     * <p>
     * {@.ja  PortConnectListenrを保持するホルダ}
     * {@.en Holders of PortConnectListeners}
     *
     */
    //protected PortConnectListeners m_portconnListeners = new PortConnectListeners();
    protected PortConnectListeners m_portconnListeners = null;
    /**
     * <p> The maximum number of connections </p>
     */
    protected int m_connectionLimit;
    /*!
     * @if jp
     * @brief インスタンス名
     * @else
     * @brief Instance name
     * @endif
     */
    protected String m_ownerInstanceName;
}
