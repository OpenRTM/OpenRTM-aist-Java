package jp.go.aist.rtm.RTC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.Boolean;
import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.ConsoleHandler;

import jp.go.aist.rtm.Constants;
import jp.go.aist.rtm.RTC.executionContext.ECFactoryBase;
import jp.go.aist.rtm.RTC.executionContext.ECFactoryJava;
import jp.go.aist.rtm.RTC.executionContext.ExecutionContextBase;
import jp.go.aist.rtm.RTC.executionContext.ExtTrigExecutionContext;
import jp.go.aist.rtm.RTC.executionContext.OpenHRPExecutionContext;
import jp.go.aist.rtm.RTC.executionContext.PeriodicECSharedComposite;
import jp.go.aist.rtm.RTC.executionContext.PeriodicExecutionContext;
import jp.go.aist.rtm.RTC.log.Logbuf;
import jp.go.aist.rtm.RTC.port.PortBase;
import jp.go.aist.rtm.RTC.util.CallbackFunction;
import jp.go.aist.rtm.RTC.util.CORBA_SeqUtil;
import jp.go.aist.rtm.RTC.util.CORBA_RTCUtil;
import jp.go.aist.rtm.RTC.util.DataRef;
import jp.go.aist.rtm.RTC.util.IiopAddressComp;
import jp.go.aist.rtm.RTC.util.NVUtil;
import jp.go.aist.rtm.RTC.util.ORBUtil;
import jp.go.aist.rtm.RTC.util.Properties;
import jp.go.aist.rtm.RTC.util.StringUtil;
import jp.go.aist.rtm.RTC.util.TimeValue;
import jp.go.aist.rtm.RTC.util.Timer;
import jp.go.aist.rtm.RTC.util.equalFunctor;
import java.io.ByteArrayOutputStream;

import org.omg.CORBA.ORB;
import org.omg.CORBA.OctetSeqHelper;
import org.omg.CORBA.Object;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.BindingListHolder;
import org.omg.CosNaming.BindingType;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.IOP.TaggedProfile;
import org.omg.IOP.TaggedProfileHelper;
import org.omg.IOP.TAG_MULTIPLE_COMPONENTS;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;

import RTC.ExecutionContext;
import RTC.ExecutionContextListHolder;
import RTC.ReturnCode_t;
import RTC.ConnectorProfile;
import RTC.ConnectorProfileHolder;
import RTC.PortProfile;
import RTC.PortService;
import RTC.PortServiceHelper;
import RTC.PortServiceHolder;
import RTC.PortServiceListHolder;

import _SDOPackage.NVListHolder;
import _SDOPackage.NameValue;

/**
 * {@.ja 各コンポーネントの管理を行うクラス.}
 * {@.en This is a manager class that manages various information 
 * such as components.}
 *
 * <p>
 * {@.ja コンポーネントなど各種の情報管理を行うマネージャクラス。}
 */
public class Manager {

    private class SignalAction extends Thread {
        public SignalAction() {
            super();
        }
        public void run(){
            terminate(0.0);
            try {
                Thread.sleep(100);
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * {@.ja コンストラクタ。}
     * {@.en Constructor}
     * 
     * <p>    
     * {@.ja Protected コンストラクタ}
     * {@.en Protected Constructor}
     *
     */
    protected Manager() {
        
        m_initProc = null;
        rtcout = new Logbuf("Manager");
        m_runner = null;
        m_terminator = null;
        Runtime.getRuntime().addShutdownHook(new SignalAction());
    }

    /**
     * {@.ja コピーコンストラクタ。}
     * {@.en Copy Constructor}
     *
     * <p>
     * {@.ja Protected コピーコンストラクタ}
     * {@.en Protected Copy Constructor}
     *
     * @param rhs 
     *   {@.ja コピー元のManagerオブジェクト}
     *   {@.en Manager object of copy source}
     */
    public Manager(final Manager rhs) {
        
        m_initProc = null;
        rtcout = new Logbuf("Manager");
        m_runner = null;
        m_terminator = null;
        Runtime.getRuntime().addShutdownHook(new SignalAction());
    }
    
    /**
     * {@.ja マネージャの初期化。}
     * {@.en Initialize manager}
     *
     * <p>
     * {@.ja マネージャを初期化する static メンバ関数。
     * マネージャをコマンドライン引数を与えて初期化する。
     * マネージャを使用する場合は、必ずこの初期化メンバ関数 init() を
     * 呼ばなければならない。
     * マネージャのインスタンスを取得する方法として、init(), instance() の
     * 2つの static メンバ関数が用意されているが、初期化はinit()でしか
     * 行われないため、Manager の生存期間の一番最初にはinit()を呼ぶ必要がある。
     *
     * ※マネージャの初期化処理<ul>
     * <li> initManager: 引数処理、configファイルの読み込み、サブシステム初期化
     * <li> initLogger: Logger初期化
     * <li> initORB: ORB 初期化
     * <li> initNaming: NamingService 初期化
     * <li> initExecutionContext: ExecutionContext factory 初期化
     * <li> initTimer: Timer 初期化</li></ul>}
     * {@.en This is the static member function to initialize the Manager.
     * The Manager is initialized by given commandline arguments.
     * To use the manager, this initialization member function init() must be
     * called. The manager has two static functions to get the instance such as
     * init() and instance(). Since initializing process is only performed by
     * the init() function, the init() has to be called at the beginning of
     * the lifecycle of the Manager.
     *
     * *Initialization of manager<ul>
     * <li> initManager: Argument processing, reading config file,
     *                initialization of subsystem
     * <li> initLogger: Initialization of Logger
     * <li> initORB: Initialization of ORB
     * <li> initNaming: Initialization of NamingService
     * <li> initExecutionContext: Initialization of ExecutionContext factory
     * <li> initTimer: Initialization of Timer</li></ul>}
     *
     * @param argv 
     *   {@.ja コマンドライン引数の配列}
     *   {@.en The array of the command line arguments.}
     * 
     * @return 
     *   {@.ja Manager の唯一のインスタンスの参照}
     *   {@.en Reference of the unique instance of Manager}
     */
    public static Manager init(String[] argv) {
        
        synchronized (manager_mutex) {
            if (manager == null) {
                try {
                    manager = new Manager();
                    manager.initManager(argv);
                    manager.initLogger();
                    manager.initORB();
                    manager.initNaming();
                    manager.initFactories();
                    manager.initExecContext();
                    manager.initComposite();
                    manager.initTimer();
                    manager.initManagerServant();
                    
                } catch (Exception e) {
                    manager = null;
                }
            }
        }
        return manager;
    }
    
    /**
     * {@.ja マネージャのインスタンスの取得。}
     * {@.en Get instance of the manager}
     *
     * <p>
     * {@.ja マネージャのインスタンスを取得する static メンバ関数。
     * この関数を呼ぶ前に、必ずこの初期化メンバ関数 init() が呼ばれている
     * 必要がある。}
     * {@.en This is the static member function to get the instance 
     * of the Manager.
     * Before calling this function, ensure that the initialization function
     * "init()" is called.}
     *
     * @return 
     *   {@.ja Manager の唯一のインスタンスの参照}
     *   {@.en The only instance reference of the manager}
     */ 
    public static Manager instance() {
        
        synchronized (manager_mutex) {
            if (manager == null) {
                try {
                    manager = new Manager();
                    manager.initManager(null);
                    manager.initLogger();
                    manager.initORB();
                    manager.initNaming();
                    manager.initFactories();
                    manager.initExecContext();
                    manager.initComposite();
                    manager.initTimer();
                    
                } catch (Exception e) {
                    manager = null;
                }
            }
        }

        return manager;
    }
    
    /**
     * {@.ja マネージャ終了処理。}
     * {@.en Terminate manager}
     *
     * <p>
     * {@.ja マネージャの終了処理を実行する。}
     * {@.en Terminate manager's processing}
     *
     */
    public void terminate(double tm) {
        
        if (m_terminator != null) {
            m_terminator.terminate(tm);
        }
    }
    
    /**
     * {@.ja マネージャインスタンスが生成済みかチェック。}
     * {@.en Check Manager Instance}
     *
     * <p>
     * {@.ja マネージャインスタンスが生成済みかどうか確認する。}
     * {@.en Check manager instance}
     *
     * @return 
     *   {@.ja Manager の唯一のインスタンスの有無}
     *   {@.en Existence of the only instance reference of the manager}
     */
    public static boolean isActive() {
        synchronized (manager_mutex) {
            if(manager==null) {
                return false;
            }
            return true;
        }
    }
    
    /**
     * {@.ja マネージャ・シャットダウン}
     * {@.en Shutdown Manager}
     *
     * <p>
     * {@.ja マネージャの終了処理を実行する。
     * ORB終了後、同期を取って終了する。}
     * {@.en Terminate manager's processing.
     * After terminating ORB, shutdown manager in sync.}
     */
    public void shutdown() {
        
        rtcout.println(Logbuf.TRACE, "Manager.shutdown()");
        m_listeners.manager_.preShutdown();        
        shutdownComponents();
        shutdownNaming();
        shutdownORB();
        shutdownManager();
        
        // 終了待ち合わせ
        if (m_runner != null) {
            synchronized (m_runner) {
                try {
                    m_runner.wait();
                
                } catch (InterruptedException e) {
                    rtcout.println(Logbuf.DEBUG, "Exception: Caught InterruptedException in Manager.shutdown().");
                    rtcout.println(Logbuf.DEBUG, e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            join();
        }
       
        m_listeners.manager_.postShutdown(); 
        shutdownLogger();
    }
    
    /**
     * {@.ja マネージャ終了処理の待ち合わせ。}
     * {@.en Wait for Manager's termination}
     *
     * <p>
     * {@.ja 同期を取るため、マネージャ終了処理の待ち合わせを行う。}
     * {@.en Wait for Manager's termination to synchronize.}
     */
    public void join() {
        
        rtcout.println(Logbuf.TRACE, "Manager.join()");
        
        synchronized (Integer.valueOf(m_terminate_waiting)) {
            ++m_terminate_waiting;
        }
        
        while (true) {
            synchronized (Integer.valueOf(m_terminate_waiting)) {
                if (m_terminate_waiting > 1) {
                    break;
                }
            }
            
            try {
                Thread.sleep(100);
                
            } catch (InterruptedException e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught InterruptedException in Manager.join().");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * {@.ja マネージャコンフィギュレーションの取得。}
     * {@.en Get the manager configuration}
     *
     * <p>
     * {@.ja マネージャに設定したコンフィギュレーションを取得する。}
     * {@.en Get the manager configuration that has been set to manager.}
     *
     * @return 
     *   {@.ja マネージャのコンフィギュレーション}
     *   {@.en Manager's configuration}
     *
     */
    public Properties getConfig() {
        return m_config;
    }


  
    /**
     *
     * {@.ja 指定したRTコンポーネントの保持するポートを
     * NamingServiceにバインドする}
     * {@.en Bind ports in NamingService. }
     * <p>
     * {@.ja ポートのpublish_topicというプロパティでトピック名を設定し、
     * トピック名のコンテキストの下に登録}
     *
     * @param comp
     *   {@.ja RTコンポーネント}
     *   {@.en RT Component}
     * void publishPorts(RTObject_impl* comp)
     */
    public void publishPorts(RTObject_impl comp){
        //PortServiceListHolder ports = new PortServiceListHolder();
        PortService[] ports = comp.get_ports();
        for(int ic=0;ic<ports.length;++ic){
            PortProfile prof = ports[ic].get_port_profile();
            NVListHolder nvholder = 
                new NVListHolder(prof.properties);
            Properties prop = new Properties();
            NVUtil.copyToProperties(prop, nvholder);

            if(
               ( prop.hasKey("publish_topic")==null ||
                 prop.getProperty("publish_topic")=="" )
               &&
               ( prop.hasKey("subscribe_topic")==null ||
                 prop.getProperty("subscribe_topic")=="" )
               &&
               ( prop.hasKey("rendezvous_point")==null ||
                 prop.getProperty("rendezvous_point")=="" )
            ){
               
                
                continue;
            }
            String name = new String();
            if(prop.getProperty("port.port_type").equals("DataOutPort")){
                name  = "dataports.port_cxt/";
                name += prop.getProperty("publish_topic") + ".topic_cxt/";
                name += prof.name;
                name += ".outport";
            }
            else if(prop.getProperty("port.port_type").equals("DataInPort")){
                name  = "dataports.port_cxt/";
                name += prop.getProperty("publish_topic") + ".topic_cxt/";
                name += prof.name;
                name += ".inport";
            }
            else if(prop.getProperty("port.port_type").equals("CorbaPort")){
                name  = "svcports.port_cxt/";
                name += prop.getProperty("publish_topic") + ".topic_cxt/";
                name += prof.name;
                name += ".svc";
            }
            else{
        
                rtcout.println(Logbuf.WARN, 
                "Unknown port type: "+prop.getProperty("port.port_type"));
                continue;
            }
            try {
                POA poa = Manager.instance().getPOA();
                PortBase port = (PortBase)(poa.reference_to_servant(ports[ic]));
      
                m_namingManager.bindPortObject(name, port);
            }
            catch  (Exception e) {
                rtcout.println(Logbuf.DEBUG, 
                            "Peer port might be a remote port.");
            }
        }
/*
        for p in ports{
            prof = p.get_port_profile()
            prop = OpenRTM_aist.Properties()
            OpenRTM_aist.NVUtil.copyToProperties(prop, prof.properties)
      
            if (prop.hasKey("publish_topic") is None or not str(prop.getProperty("publish_topic"))) and (prop.hasKey("subscribe_topic") is None or not str(prop.getProperty("subscribe_topic"))) and (prop.hasKey("rendezvous_point") is None or not str(prop.getProperty("rendezvous_point"))){
                continue
            }


            if prop.getProperty("port.port_type") == "DataOutPort"{
                name  = "dataports.port_cxt/"
                name += str(prop.getProperty("publish_topic")) + ".topic_cxt/"
                name += prof.name
                name += ".outport"
            }
            elif prop.getProperty("port.port_type") == "DataInPort"{
                name  = "dataports.port_cxt/"
                name += str(prop.getProperty("publish_topic")) + ".topic_cxt/"
                name += prof.name
                name += ".inport"
            }
            elif prop.getProperty("port.port_type") == "CorbaPort"{
                name  = "svcports.port_cxt/"
                name += str(prop.getProperty("publish_topic")) + ".topic_cxt/"
                name += prof.name
                name += ".svc"
            }
            else{
        
                self._rtcout.RTC_WARN("Unknown port type: %s" % str(prop.getProperty("port.port_type")))
                continue
            }
      
            port = self._poa.reference_to_servant(p)
      
            self._namingManager.bindPortObject(name, port)
        }
*/
    }
    /**
     *
     * {@.ja 指定したRTコンポーネントの保持するポートを
     * 同じトピック名以下の接続可能なポートと接続}
     * {@.en connects a port by a topic name.}
     *
     *
     * @param comp
     *   {@.ja RTコンポーネント}
     *   {@.en RT Component}
     *
     *  # void subscribePorts(RTObject_impl* comp)
     */
    public void subscribePorts(RTObject_impl comp){
        rtcout.println(Logbuf.TRACE, "Manager.subscribePorts()");
        PortService[] ports = comp.get_ports();
    
        for(int ic=0; ic<ports.length; ++ic ){
            PortProfile prof = ports[ic].get_port_profile();
            NVListHolder nvholder = 
                new NVListHolder(prof.properties);
            Properties prop = new Properties();
            NVUtil.copyToProperties(prop, nvholder);
      
            if(
               ( prop.hasKey("publish_topic")==null ||
                 prop.getProperty("publish_topic")=="" )
               &&
               ( prop.hasKey("subscribe_topic")==null ||
                 prop.getProperty("subscribe_topic")=="" )
               &&
               ( prop.hasKey("rendezvous_point")==null ||
                 prop.getProperty("rendezvous_point")=="" )
            ){
               
                continue;
            }
            PortService[] nsports;
            String name = new String();
            if(prop.getProperty("port.port_type").equals("DataOutPort")){
                name  = "dataports.port_cxt/";
                name += prop.getProperty("publish_topic") + ".topic_cxt";
                nsports = getPortsOnNameServers(name, "inport");
                connectDataPorts(ports[ic], nsports);
            }
            else if(prop.getProperty("port.port_type").equals("DataInPort")){
                name  = "dataports.port_cxt/";
                name += prop.getProperty("publish_topic") + ".topic_cxt";
                nsports = getPortsOnNameServers(name, "outport");
                connectDataPorts(ports[ic], nsports);
            }
            else if(prop.getProperty("port.port_type").equals("CorbaPort")){
                name  = "svcports.port_cxt/";
                name += prop.getProperty("publish_topic") + ".topic_cxt";
                nsports = getPortsOnNameServers(name, "svc");
                connectServicePorts(ports[ic], nsports);
            }
/*      
            if prop.getProperty("port.port_type") == "DataOutPort"{
                name  = "dataports.port_cxt/"
                name += str(prop.getProperty("publish_topic")) + ".topic_cxt"
        
                nsports = self.getPortsOnNameServers(name, "inport")
        
                self.connectDataPorts(p, nsports)
            }
            elif prop.getProperty("port.port_type") == "DataInPort"{
                name  = "dataports.port_cxt/"
                name += str(prop.getProperty("publish_topic")) + ".topic_cxt"
                nsports = self.getPortsOnNameServers(name, "outport")
                self.connectDataPorts(p, nsports)
            }
            elif prop.getProperty("port.port_type") == "CorbaPort"{
                name  = "svcports.port_cxt/"
                name += str(prop.getProperty("publish_topic")) + ".topic_cxt"
                nsports = self.getPortsOnNameServers(name, "svc")
                self.connectServicePorts(p, nsports)
            }
*/
        }
    }
    /**
     *
     * {@.ja 与えられたパス以下の指定されたkindのポートを取得する}
     * {@.en Gets specified ports from the path.}
     * 
     * @param nsname 
     *   {@.ja パス}
     *   {@.en path}
     *
     * @param kind 
     *   {@.ja kind}
     *   {@.en kind}
     *
     * @return 
     *   {@.ja ポートのオブジェクトリファレンスのリスト}
     *   {@.en List of port objects}
     *
     * PortServiceList_var getPortsOnNameServers(std::string nsname,std::string kind)
     */
    public PortService[] getPortsOnNameServers(String nsname, String kind){
        rtcout.println(Logbuf.PARANOID,
                            "getPortsOnNameServers("+nsname+","+kind+")");
        ArrayList<PortService> ports = new ArrayList<PortService>();
/*
        Vector<NamingService> ns = m_namingManager.getNameServices();
        Iterator<NamingService> it = ns.iterator();
*/
        ArrayList<CorbaNaming> ns = m_namingManager.getCorbaNamings();
        Iterator<CorbaNaming> it = ns.iterator();
        while (it.hasNext()) {
/*
            NamingBase noc = it.next().ns;
            if(noc == null){
                continue;
            }
            CorbaNaming cns = noc.getCorbaNaming();
            if(cns == null){
                continue;
            }
*/
            CorbaNaming cns = it.next();
            BindingListHolder bl = new BindingListHolder();
            cns.listByKind(nsname,kind,bl);
            for(int ic=0;ic<bl.value.length;++ic){
                if(bl.value[ic] == null){
                    continue;
                }
                if(bl.value[ic].binding_type != BindingType.nobject){
                    continue;
                }
                String tmp = bl.value[ic].binding_name[0].id + "." 
                                + bl.value[ic].binding_name[0].kind;
                //String nspath = "/" + nsname + "/" + tmp;
                String nspath = nsname + "/" + tmp;
                nspath = nspath.replace("\\","");

                Object obj;
                try {
                    obj = cns.resolve(nspath);
                }
                catch (Exception e) {
                    continue;
                }
                PortService portsvc  = PortServiceHelper.narrow(obj);
                if(portsvc == null){
                    continue;
                } 
                try{
                    PortProfile prof = portsvc.get_port_profile();
                }          
                catch(Exception ex){
                    continue;
                }
                ports.add(portsvc);
            }
        }
        PortService[] ret = (PortService[])ports.toArray(new PortService[0]);
        return ret;
    }

    /**
     *
     * {@.ja 指定したデータポートを指定したリスト内のデータポート全てと接続する}
     * {@.en Connects all specified ports.}
     *
     * @param port
     *   {@.ja 対象のデータポート}
     *   {@.en Target DataPort}
     *
     * @param target_ports 
     *   {@.ja 接続対象のデータポートのリスト}
     *   {@.en List of connected DataPorts}
     *
  # void connectDataPorts(PortService_ptr port,PortServiceList_var& target_ports)
     */
    public void connectDataPorts(PortService port,PortService[] target_ports){
        rtcout.println(Logbuf.TRACE, "Manager.connectDataPorts()");
        for(int ic=0;ic<target_ports.length;++ic){
            if(port._is_equivalent(target_ports[ic])){
                continue;
            }
            String con_name = "";
            PortProfile prof0 = port.get_port_profile();
            PortProfile prof1 = target_ports[ic].get_port_profile();
            con_name += prof0.name;
            con_name += ":";
            con_name += prof1.name;
            Properties prop = new Properties();
            ReturnCode_t ret = CORBA_RTCUtil.connect(
                        con_name,prop,port,target_ports[ic]);
            if(ret != ReturnCode_t.RTC_OK){
                rtcout.println(Logbuf.ERROR, 
                            "Connection error in topic connection.");
            }
        }
    }    
/*
    for p in target_ports:
      if port._is_equivalent(p):
        continue
      con_name = ""
      p0 = port.get_port_profile()
      p1 = p.get_port_profile()
      con_name += p0.name
      con_name += ":"
      con_name += p1.name
      prop = OpenRTM_aist.Properties()
      if RTC.RTC_OK != OpenRTM_aist.CORBA_RTCUtil.connect(con_name,prop,port,p):
        self._rtcout.RTC_ERROR("Connection error in topic connection.")
    }
*/

    /**
     * 
     * {@.ja 指定したサービスポートを指定したリスト内のサービスポート全てと
     * 接続する}
     * {@.en Connects all specified serviceports.}
     *
     *
     * @param port
     *   {@.ja 対象のサービスポート}
     *   {@.en Target ServicePort}
     *
     * @param target_ports 
     *   {@.ja 接続対象のサービスポートのリスト}
     *   {@.en List of connected ServicePorts}
  # void connectServicePorts(PortService_ptr port,PortServiceList_var& target_ports)
     */
    public void connectServicePorts(PortService port, 
                    PortService[] target_ports){
        for(int ic=0;ic<target_ports.length;++ic){
            if(port._is_equivalent(target_ports[ic])){
                continue;
            }

            String con_name = "";
            PortProfile prof0 = port.get_port_profile();
            PortProfile prof1 = target_ports[ic].get_port_profile();
            con_name += prof0.name;
            con_name += ":";
            con_name += prof1.name;
            Properties prop = new Properties();
            ReturnCode_t ret = CORBA_RTCUtil.connect(
                        con_name,prop,port,target_ports[ic]);
            if(ret != ReturnCode_t.RTC_OK){
                rtcout.println(Logbuf.ERROR, 
                            "Connection error in topic connection.");
            }
        }
    }    
/*
    for p in target_ports:
      if port._is_equivalent(p):
        continue
      con_name = ""
      p0 = port.get_port_profile()
      p1 = p.get_port_profile()
      con_name += p0.name
      con_name += ":"
      con_name += p1.name
      prop = OpenRTM_aist.Properties()
      if RTC.RTC_OK != OpenRTM_aist.CORBA_RTCUtil.connect(con_name,prop,port,p):
        self._rtcout.RTC_ERROR("Connection error in topic connection.")

*/

    /**
     *
     * {@.ja ManagerServantを取得する}
     * {@.en Gets ManagerServant Managfer has.}
     *
     *
     * @return 
     *   {@.ja ManagerServant}
     *   {@.en ManagerServant}
     *
     * # ManagerServant* getManagerServant()
     */
    public ManagerServant  getManagerServant(){

        rtcout.println(Logbuf.TRACE, "Manager.getManagerServant()");
        return m_mgrservant;
    }



    /**
     *
     * {@.ja NamingManagerを取得する}
     * {@.en Gets NamingManager Managfer has.}
     *
     *
     * @return 
     *   {@.ja NamingManager}
     *   {@.en NamingManager}
     *
     *  # NamingManager* getNaming()
     */
    public NamingManager getNaming(){
        rtcout.println(Logbuf.TRACE, "Manager.getNaming()");
        return m_namingManager;

    }    
    /**
     * {@.ja 初期化プロシージャのセット。}
     * {@.en Set initial procedure}
     *
     * <p>
     * {@.ja このオペレーションはユーザが行うモジュール等の初期化プロシージャ
     * を設定する。ここで設定されたプロシージャは、マネージャが初期化され、
     * アクティブ化された後、適切なタイミングで実行される。}
     * {@.en This operation sets the initial procedure call to process module
     * initialization, other user defined initialization and so on.
     * The given procedure will be called at the proper timing after the 
     * manager initialization, activation and run.}
     *
     * @param initProc 
     *   {@.ja コールバックインタフェース}
     *   {@.en Callback interface}
     *
     */
    public void setModuleInitProc(ModuleInitProc initProc) {
        m_initProc = initProc;
    }
    
    /**
     * {@.ja Managerのアクティブ化。}
     * {@.en Activate the Manager}
     * <p>
     * {@.ja 初期化後に runManager() 呼び出しに先立ってこのメソッドを
     * 呼び出す必要があります。
     * このオペレーションは以下の処理を行う<ul>
     * <li> CORBA POAManager のアクティブ化
     * <li> マネージャCORBAオブジェクトのアクティブ化
     * <li> Manager のオブジェクト参照の登録
     * </ul>
     * このオペレーションは、マネージャの初期化後、runManager()
     * の前に呼ぶ必要がある。}
     * {@.en This operation do the following:
     * <li> Activate CORBA POAManager
     * <li> Activate Manager CORBA object
     * <li> Bind object reference of the Manager to the nameserver
     * </ul>
     *
     * This operation should be invoked after Manager:init(),
     * and before runManager().}
     * </p>
     *
     *
     * @return 
     *   {@.ja 処理結果(アクティブ化成功:true、失敗:false)}
     *   {@.en Activation result (Successful:true, Failed:false)}
     *
     */
    public boolean activateManager() {
        
        rtcout.println(Logbuf.TRACE, "Manager.activateManager()");
        
        try {
            if(this.getPOAManager() == null) {
                rtcout.println(Logbuf.ERROR, "Could not get POA manager.");
                return false;
            }
            this.getPOAManager().activate();
            rtcout.println(Logbuf.TRACE, "POA Manager activated.");
        } catch (Exception e) {
            rtcout.println(Logbuf.DEBUG, 
                "Exception: Caught unknown Exception in Manager.activateManager().");
            rtcout.println(Logbuf.DEBUG, "POA Manager activation failed.");
            rtcout.println(Logbuf.DEBUG, e.getMessage());
            return false;
        }

        bindManagerServant();

        String[] lsvc 
            = m_config.getProperty("manager.local_service.modules").split(",");
        loadComponent(lsvc);

        initLocalService();
         
        lsvc = m_config.getProperty("manager.modules.preload").split(",");
        loadComponent(lsvc);

        if (m_initProc != null) {
            m_initProc.myModuleInit(this);
        }

        precreateComponent();
        initPreConnection();
        initPreActivation();
        return true;
    }

    /**
     * {@.ja コンポーネントをロードする。}
     * {@.en Loads components.}
     * 
     * <p>
     * {@.ja コンポーネントをロードする。}
     * {@.en This method loads components .}
     *
     * @param mods
     *   {@.ja ロードするモジュール名}
     *   {@.en Loaded module names}
     * 
     */
    private void loadComponent(String[] mods) {

        for (int i=0; i < mods.length; ++i) {
            if ( mods[i].length() == 0) {
                continue;
            }
	    mods[i] = mods[i].trim();
            String[] str = mods[i].split("\\.");
            try {
                m_module.load(mods[i], "registerModule");
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                rtcout.println(Logbuf.ERROR, "Module load error: " + mods[i]);
            } catch (ClassNotFoundException e) {
                rtcout.println(Logbuf.ERROR, "Module not found: " + mods[i]);
            } catch (Exception ex) {
                rtcout.println(Logbuf.ERROR, "Unknown Exception");
            }
        }
    }
    /**
     * {@.ja コンポーネントを生成する。}
     * {@.en Creates components}
     *
     * <p>
     * {@.ja このメソッドは"manager.components.precreate"に設定されている
     * コンポーネントを生成する。}
     * {@.en This method creates components set to 
     * "Manager.components.precreate".}
     */
    private void precreateComponent() {
        rtcout.println(Logbuf.TRACE, 
           "Components pre-creation: " 
           + m_config.getProperty("manager.components.precreate").split(","));
        String[] comp 
            = m_config.getProperty("manager.components.precreate").split(",");
        for (int i=0; i < comp.length; ++i) {
            if ( comp[i].length() == 0) {
                continue;
            }
	    comp[i] = comp[i].trim();
            this.createComponent(comp[i]);
        }
    }
    /**
     * {@.ja 起動時にrtc.confで指定したRTCをアクティベーションする。}
     * {@.en Activates RTC designated in rtc.conf,when starting.}
     *
     * <p>
     * {@.ja このメソッドは"manager.components.preactivation"に設定される
     * コンポーネントを活性化する。
     * 例:
     * manager.components.preactivation: RTC1,RTC2~}
     * {@.en This method activates components set to 
     * "manager.components.preactivation".}
     *
     */
    private void initPreActivation() {
        String preactivation 
            = m_config.getProperty("manager.components.preactivation");
        String[] comps = new String[0];
        if ( preactivation == null || preactivation.length() == 0 ) {
            rtcout.println(Logbuf.TRACE, 
                "There are no pre-activation components." );
            return;
        }
        rtcout.println(Logbuf.TRACE, 
                "Components pre-activation: " 
                + Arrays.toString(preactivation.split(",")));
        comps = preactivation.split(",");

        for (int ic=0; ic < comps.length; ++ic) {
            comps[ic] = comps[ic].trim();
            if(!comps[ic].isEmpty()) { 
                 RTC.RTObject comp_ref;
                if(comps[ic].indexOf("://") == -1) { 
                    RTObject_impl comp = getComponent(comps[ic]);
                    if (comp == null) { 
                        rtcout.println(Logbuf.ERROR, comps[ic] + " not found.");
                        continue; 
                    }
                    comp_ref = comp.getObjRef();
                }
                else {
                    RTC.RTObject[] rtcs = m_namingManager.string_to_component(comps[ic]);
                    if(rtcs == null || rtcs.length == 0) {
                        rtcout.println(Logbuf.ERROR, comps[ic] + " not found.");
                        continue;
                    }
                    comp_ref = rtcs[0];
                }
                ReturnCode_t ret = CORBA_RTCUtil.activate(comp_ref);
                if (ret != ReturnCode_t.RTC_OK) { 
                    rtcout.println(Logbuf.ERROR, comps[ic] + " activation filed.");
                        continue;
                }
                else {
                    rtcout.println(Logbuf.INFO, comps[ic] + " activated.");
                }
            }
        }
    }
    /**
     * {@.ja 起動時にrtc.confで指定したポートを接続する。}
     * {@.en Connects ports of RTC designated in rtc.conf,when starting.}
     *
     * <p>
     * {@.ja このメソッドは"manager.components.preactivation"に設定される
     * コンポーネントを活性化する。
     * 例:
     * manager.components.preconnect: RTC0.port0?port=RTC0.port1&interface_type=corba_cdr&dataflow_type=pull&~,~
     * "manager.components.preconnect".}
     *
     */
    private void initPreConnection() { // pre-connection
        String preconnect 
            = m_config.getProperty("manager.components.preconnect");
        rtcout.println(Logbuf.TRACE, "Connection pre-connection: " 
                + preconnect);
        String[] connectors = new String[0];
        if ( preconnect == null || preconnect.length() == 0 ) {
        }
        else {
            connectors = preconnect.split(",");
            rtcout.println(Logbuf.TRACE, "connectors: " 
                +Arrays.toString(connectors));
        }
        for (int ic=0; ic < connectors.length; ++ic) {
            // ConsoleIn.out?Console.in&dataflow_type=push&....
            String[] conn_prop = connectors[ic].split("\\?");
            if ( conn_prop == null || conn_prop.length == 0 ) {
            }
            //String[] params = connectors[ic].split("&");
            ArrayList<String> params 
                    = new ArrayList(Arrays.asList(conn_prop[1].split("&")));
            HashMap<String,String> config = new HashMap<>();
            ArrayList<String> ports = new ArrayList<String>();
            for(String param :params){
                String[] elem = param.split("=");
                if(elem.length != 2){
                    continue;
                }
	        elem[0] = elem[0].trim();
	        elem[1] = elem[1].trim();
                String exp = "port\\d*";
                if(elem[0].matches(exp)){
                    ports.add(elem[1]);
                    continue;
                }
                config.put(elem[0],elem[1]);
            }

            if(ports.size() == 0) {
                rtcout.println(Logbuf.ERROR, 
                    "Invalid format for pre-connection.");
                rtcout.println(Logbuf.ERROR, 
                    "Format must be RTC0.port0?port=RTC0.port1");
                continue;
            }

            String comp0_name = conn_prop[0].split("\\.")[0];
            //String port0_name 
            //    = conn_prop[0].substring(conn_prop[0].lastIndexOf(".") + 1);
            String port0_name = conn_prop[0];

            RTC.RTObject comp0_ref;
            if(comp0_name.indexOf("://") == -1){
                RTObject_impl comp0 = getComponent(comp0_name);
                if (comp0 == null) { 
                    rtcout.println(Logbuf.ERROR, comp0_name + " not found.");
                    continue;
                }
                comp0_ref = comp0.getObjRef();
            }
            else {
                RTC.RTObject[] rtcs = m_namingManager.string_to_component(comp0_name);
                if(rtcs == null || rtcs.length == 0) {
                    rtcout.println(Logbuf.ERROR, comp0_name + " not found.");
                    continue;
                }
                comp0_ref = rtcs[0];
                String[] array = conn_prop[0].split("/");
                port0_name = array[array.length-1];
            }

            PortService port0_var = CORBA_RTCUtil.get_port_by_name(comp0_ref, port0_name);
        
            if(port0_var == null){
                rtcout.println(Logbuf.ERROR, "port " + port0_name + " not found.");
                continue;
            }
            
            //Makes ConnectorProfile 
            NVListHolder nvholder = new NVListHolder();
            if( nvholder.value==null ) {
                nvholder.value = new NameValue[0];
            }
            if(config.get("dataflow_type")==null){
                config.put("dataflow_type","push");
            }
            if(config.get("interface_type")==null){
                config.put("interface_type","corba_cdr");
            }
            config.forEach((k, v) -> {
                rtcout.println(Logbuf.TRACE, "options: " + k +","+ v);
                CORBA_SeqUtil.push_back(nvholder,
                        NVUtil.newNVString("dataport." + k,v));
            });
            Properties prop = new Properties();
            NVUtil.copyToProperties(prop, nvholder);

            //Connects ports
            for(String port : ports){
                //String port_name = port.substring(port.lastIndexOf(".")+1);
                String port_name = port;
                String comp_name = port.split("\\.")[0];

                RTC.RTObject comp_ref;
                if(comp_name.indexOf("://") == -1){
                    RTObject_impl comp = getComponent(comp_name);
                    if (comp == null) { 
                        rtcout.println(Logbuf.ERROR, 
                                            comp_name + " not found.");
                        continue;
                    }
                    comp_ref = comp.getObjRef();
                }
                else{
                    RTC.RTObject[] rtcs 
                            = m_namingManager.string_to_component(comp_name);
                    if(rtcs == null || rtcs.length == 0) {
                        rtcout.println(Logbuf.ERROR, comp_name + " not found.");
                        continue;
                    }
                    comp_ref = rtcs[0];
                    String[] array = port.split("/");
                    port_name = array[array.length-1];
                }


                PortService port_var 
                    = CORBA_RTCUtil.get_port_by_name(comp_ref, port_name);
                if(port_var == null){
                    rtcout.println(Logbuf.ERROR, 
                            "port " + port_name + " not found.");
                    continue;
                }


                //ReturnCode_t ret = CORBA_RTCUtil.connect(
                //        connectors[ic],prop,port0_var, port_var);
                ReturnCode_t ret = CORBA_RTCUtil.connect(
                        conn_prop[0]+"?"+port_name,prop,port0_var, port_var);
                if(ret != ReturnCode_t.RTC_OK){
                    rtcout.println(Logbuf.ERROR, 
                            "Connection error in topic connection.");
                }
                NVUtil.dump(nvholder);
            }
        }
    }

    /**
     * {@.ja Managerの実行。}
     * {@.en Run the Manager}
     *
     * <p>
     * {@.ja このオペレーションはマネージャのメインループを実行する。
     * runManager(false)の呼び出しと同等。
     * このメインループ内では、CORBA ORBのイベントループ等が
     * 処理される。このオペレーションはブロックし、
     * Manager::destroy() が呼ばれるまで処理を戻さない。}
     * {@.en This operation processes the main event loop of the Manager.
     * In this main loop, CORBA's ORB event loop or other processes
     * are performed. This operation is going to
     * blocking mode and never returns until Manager::destroy() is called.}
     *
     */
    public void runManager() {
        this.runManager(false);
    }
    
    /**
     * <p>Managerのメインループを実行します。
     * このメインループ内では、CORBA ORBのイベントループなどが処理されます。<br />
     * ブロッキングモードで起動された場合は、Manager#destroy()メソッドが呼び出されるまで、
     * 本runManager()メソッドは処理を戻しません。<br />
     * 非ブロッキングモードで起動された場合は、内部でイベントループを別スレッドで開始後、
     * ブロックせずに処理を戻します。</p>
     * 
     * @param noBlocking 非ブロッキングモードの場合はtrue、ブロッキングモードの場合はfalse
     */
    /**
     * {@.ja Managerの実行。}
     * {@.en Run the Manager}
     *
     * <p>
     * {@.ja このオペレーションはマネージャのメインループを実行する。
     * このメインループ内では、CORBA ORBのイベントループ等が
     * 処理される。デフォルトでは、このオペレーションはブロックし、
     * Manager::destroy() が呼ばれるまで処理を戻さない。
     * 引数 no_block が true に設定されている場合は、内部でイベントループ
     * を処理するスレッドを起動し、ブロックせずに処理を戻す。}
     * {@.en This operation processes the main event loop of the Manager.
     * In this main loop, CORBA's ORB event loop or other processes
     * are performed. As the default behavior, this operation is going to
     * blocking mode and never returns until Manager::destroy() is called.
     * When the given argument "no_block" is set to "true", this operation
     * creates a thread to process the event loop internally, and it doesn't
     * block and returns.}
     *
     * @param noBlocking
     *   {@.ja false: ブロッキングモード, true: ノンブロッキングモード}
     *   {@.en false: Blocking mode, true: non-blocking mode.}
     */
    public void runManager(boolean noBlocking) {
        
        if (noBlocking) {
            rtcout.println(Logbuf.TRACE, "Manager.runManager(): non-blocking mode");
            
            m_runner = new OrbRunner(m_pORB);
            m_runner.open("");
        } else {
            rtcout.println(Logbuf.TRACE, "Manager.runManager(): blocking mode");
            
            m_pORB.run();

            rtcout.println(Logbuf.TRACE, "Manager.runManager(): ORB was terminated");
            
            join();
        }
    }
    
    /**
     * {@.ja [CORBA interface] モジュールのロード。}
     * {@.en [CORBA interface] Load module}
     *
     * <p>
     * {@.ja コンポーネントのモジュールをロードして、
     * 初期化メソッドを実行します。}
     * {@.en Load specified module (shared library, DLL etc..),
     * and invoke initialize function.}
     *
     * @param moduleFileName 
     *   {@.ja モジュールファイル名}
     *   {@.en The module file name}
     * @param initFunc 
     *   {@.ja 初期化メソッド名}
     *   {@.en The initialize function name}
     * @return 
     *   {@.ja RTC_OK               正常終了
     *         RTC_ERROR            ロード失敗・不明なエラー 
     *         PRECONDITION_NOT_MET 設定により許可されない操作
     *         BAD_PARAMETER        不正なパラメータ}
     *   {@.en RTC_OK               Normal return
     *         RTC_ERROR            Load failed, or unknown error
     *         PRECONDITION_NOT_MET Not allowed operation by conf
     *         BAD_PARAMETER        Invalid parameter}
     * 
     */
    //public String load(final String moduleFileName, final String initFunc) {
    public ReturnCode_t load(final String moduleFileName, final String initFunc) {

        
        rtcout.println(Logbuf.TRACE, 
                        "Manager.load("+moduleFileName+","+initFunc+")");
        String crlf = System.getProperty("line.separator");
        
        String file_name = moduleFileName;
        String init_func = initFunc;
        m_listeners.module_.preLoad(file_name, init_func);
        try {
            if (init_func==null||init_func.equals("")) {
                init_func = "registerModule";
            }
            String path = m_module.load(file_name, init_func);
            rtcout.println(Logbuf.DEBUG, "module path: "+path);
            m_listeners.module_.postLoad(path, init_func);
            
        } 
        catch (IllegalArgumentException e) {
            rtcout.println(Logbuf.ERROR, 
                "Caught Illegal Argument Exception in Manager.load()."
                + crlf 
                + e.getMessage());
            return ReturnCode_t.BAD_PARAMETER;
        }
        catch (ClassNotFoundException e) {
            rtcout.println(Logbuf.ERROR, 
                "Caught Class NotFound Exception in Manager.load()."
                + crlf 
                + e.getMessage());
            return ReturnCode_t.RTC_ERROR;
        }
        catch (InvocationTargetException e) {
            rtcout.println(Logbuf.ERROR, 
                "Caught Invocation Target Exception in Manager.load()."
                + crlf 
                + e.getMessage());
            return ReturnCode_t.PRECONDITION_NOT_MET;
        }
        catch (Exception e) {
            rtcout.println(Logbuf.ERROR, 
                "Caught unknown Exception in Manager.load()."
                + crlf 
                + e.getMessage());
            return ReturnCode_t.BAD_PARAMETER;
        }
        return ReturnCode_t.RTC_OK;
    }
    
    /**
     * {@.ja モジュールのアンロード。}
     * {@.en Unload module}
     *
     * <p>
     * {@.ja モジュールをアンロードする}
     * {@.en Unload module.}
     *
     * @param moduleFileName 
     *   {@.ja モジュールのファイル名}
     *   {@.en The module file name}
     */ 
    public void unload(final String moduleFileName) throws Exception {
        
        rtcout.println(Logbuf.TRACE, "Manager.unload("+moduleFileName+")");
       
        m_listeners.module_.preUnload(moduleFileName); 
        m_module.unload(moduleFileName);
        m_listeners.module_.postUnload(moduleFileName); 
    }
    
    /**
     * {@.ja 全モジュールのアンロード。}
     * {@.en Unload all modules}
     *
     * <p>
     * {@.ja モジュールをすべてアンロードする}
     * {@.en Unload all modules.}
     */ 
    public void unloadAll() {
        
        rtcout.println(Logbuf.TRACE, "Manager.unloadAll()");
        
        m_module.unloadAll();
    }
    
    /**
     * {@.ja ロード済みのモジュールリストを取得する。}
     * {@.en Get a list of loaded modules}
     *
     * <p>
     * {@.ja 現在マネージャにロードされているモジュールのリストを取得する。}
     * {@.en Get module list that is currently loaded into manager.}
     *
     * @return 
     *   {@.ja ロード済みモジュールリスト}
     *   {@.en Module list that has been loaded.}
     */
    public Vector<Properties> getLoadedModules() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getLoadedModules()");

        return m_module.getLoadedModules();
/*
        Set<String> key = m_module.getLoadedModules().keySet();

        Iterator it = key.iterator();
        Vector<Properties> props = new Vector<Properties>();
        while (it.hasNext()) {
            String str  = (String)it.next();
            Properties prop = new Properties(str);
            props.add(prop);
        } 
        return props;
*/
    }
    
    /**
     * {@.ja ロード可能なモジュールリストを取得する。}
     * {@.en Get a list of loadable modules}
     *
     * <p>
     * {@.ja ロード可能モジュールのリストを取得する。
     * (現在はModuleManager側で未実装)}
     * {@.en Get loadable module list.
     * (Currently, unimplemented on ModuleManager side)}
     *
     * @return
     *   {@.ja ロード可能モジュールリスト}
     *   {@.en Loadable module list}
     */
    public Vector<Properties> getLoadableModules() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getLoadableModules()");
        
        return m_module.getLoadableModules();
    }
    
    /**
     * {@.ja RTコンポーネント用ファクトリを登録する。}
     * {@.en Register RT-Component Factory}
     *
     * <p>
     * {@.ja RTコンポーネントのインスタンスを生成するための
     * Factoryを登録する。}
     * {@.en Register Factory to create RT-Component's instances.}
     *
     * @param profile 
     *   {@.ja コンポーネントプロファイル}
     *   {@.en RT-Component profile}
     * @param new_func 
     *   {@.ja コンポーネント生成オブジェクト}
     *   {@.en RT-Component creation function}
     * @param delete_func 
     *   {@.ja コンポーネント削除オブジェクト}
     *   {@.en RT-Component destruction function}
     *
     * @return 
     *   {@.ja 登録処理結果(登録成功:true、失敗:false)}
     *   {@.en Registration result (Successful:true, Failed:false)}
     */
    public boolean registerFactory(Properties profile, RtcNewFunc new_func,
            RtcDeleteFunc delete_func) {
        
        rtcout.println(Logbuf.TRACE, "Manager.registerFactory("
                + profile.getProperty("type_name") + ")");

        try {
            String policy_name 
                = m_config.getProperty("manager.components.naming_policy");
      
            //if(!policy_name.isEmpty()){ 
            if(policy_name.isEmpty()){ 
                policy_name = "process_unique";
            }
            final NumberingPolicyFactory<NumberingPolicy,String> np_factory 
                    = NumberingPolicyFactory.instance();
            NumberingPolicy policy 
                = np_factory.createObject(policy_name);
            //NumberingPolicy policy 
            //    = NumberingPolicyFactory.instance().createObject(policy_name);
            FactoryBase factory 
                = new FactoryJava(profile, new_func, delete_func, policy);
            m_factory.registerObject(factory, new FactoryPredicate(factory));
            return true;
            
        } catch (Exception ex) {
            rtcout.println(Logbuf.DEBUG, 
           "Exception: Caught unknown Exception in Manager.registerFactory().");
            rtcout.println(Logbuf.DEBUG, ex.getMessage());
            return false;
        }
    }

    /**
     * {@.ja ファクトリのプロファイルを取得。}
     * {@.en Get profiles of factories.}
     *
     * <p>
     * {@.ja ファクトリのプロファイルを取得する。}
     * {@.en Get profiles of factories.}
     *
     * @return 
     *   {@.ja ファクトリのプロファイル}
     *   {@.en profiles of factories}
     */
    public Vector<Properties> getFactoryProfiles() {
        rtcout.println(Logbuf.TRACE, "Manager.getFactoryProfiles()");

        Vector<FactoryBase> factories = m_factory.getObjects();
        Vector<Properties> props = new Vector<Properties>();
        for (int i=0, len=factories.size(); i < len; ++i) {
            props.add(factories.elementAt(i).profile());
        }

        return props;
    }

    /**
     * {@.ja ExecutionContext用ファクトリを登録する。}
     * {@.en Register ExecutionContext Factory}
     *
     * {@.ja ExecutionContextのインスタンスを生成するための
     * Factoryを登録する。}
     * {@.en Register Factory to create ExecutionContext's instances.}
     *
     * @param name 
     *   {@.ja ExecutionContext名称}
     *   {@.en ExecutionContext name}
     * @return 
     *   {@.ja 登録に成功した場合はtrueを、さもなくばfalseを返す。}
     *   {@.en Registration result (Successful:true, Failed:false)}
     */
    public boolean registerECFactory(final String name) {
        
        rtcout.println(Logbuf.TRACE, "Manager.registerECFactory(" + name + ")");
        
        try {
            ECFactoryBase factory = new ECFactoryJava(name);
            if( !m_ecfactory.registerObject(factory, new ECFactoryPredicate(factory))) {
                factory = null;
                return false;
            }
            return true;
            
        } catch (Exception ex) {
            rtcout.println(Logbuf.DEBUG, "Exception: Caught unknown Exception in Manager.registerECFactory().");
            rtcout.println(Logbuf.DEBUG, ex.getMessage());
            return false;
        }
    }

    /**
     * {@.ja ファクトリ全リストを取得する。}
     * {@.en Get the list of all Factories}
     *
     * <p>
     * {@.ja 登録されているファクトリの全リストを取得する。}
     * {@.en Get the list of all factories that have been registered.}
     *
     * @return 
     *   {@.ja 登録ファクトリ リスト}
     *   {@.en Registered factory list}
     */
    public Vector<String> getModulesFactories() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getModulesFactories()");

        Vector<String> factoryIds = new Vector<String>();
        for (int i=0, len=m_factory.m_objects.size(); i < len; ++i) {
            factoryIds.add(m_factory.m_objects.elementAt(i).profile().getProperty("implementation_id"));
        }
        
        return factoryIds;
    }
    
    /**
     * {@.ja RTコンポーネントファクトリをクリアする。}
     * {@.en Clears the factory for RT component.}
     */
    public void clearModulesFactories() {
        m_factory = new ObjectManager<String, FactoryBase>();
    }
    /**
     * {@.ja RTコンポーネントマネージャをクリアする。}
     * {@.en Clears the RT component manager.}
     */
    public void clearModules() {
        m_compManager = new ObjectManager<String, RTObject_impl>();
    }
    
    /**
     * {@.ja RTコンポーネントを生成する。}
     * {@.en Create RT-Components}
     * <p>
     * {@.ja 指定したRTコンポーネントのインスタンスを登録されたFactory経由
     * で生成する。
     *
     * 生成されるコンポーネントの各種プロファイルは以下の優先順位で
     * 設定される。
     * <ul>
     * <li># createComponent() の引数で与えられたプロファイル
     * <li># rtc.confで指定された外部ファイルで与えられたプロファイル<ul>
     *   <li># category.instance_name.config_file
     *   <li># category.component_type.config_file</ul>
     * <li># コードに埋め込まれたプロファイル 
     * </ul>
     *
     * インスタンス生成が成功した場合、併せて以下の処理を実行する。<ul>
     *  <li> 外部ファイルで設定したコンフィギュレーション情報の読み込み，設定
     *  <li> ExecutionContextのバインド，動作開始
     *  <li> ネーミングサービスへの登録</li></ul>}
     * {@.en Create specified RT-Component's instances via registered Factory.
     * When its instances have been created successfully, the following
     * processings are also executed.
     *  <li> Read and set configuration information that was set by external 
     *  file.</li>
     *  <li> Bind ExecutionContext and start operation.
     *  <li> Register to naming service.</li></ul>}
     * </p>
     * @param comp_args
     *   {@.ja 生成対象RTコンポーネントIDおよびコンフィギュレー
     *         ション引数。フォーマットは大きく分けて "id" と "configuration" 
     *         部分が存在する。
     * comp_args:     [id]?[configuration] <br>
     *                id は必須、configurationはオプション<ul>
     * <li>id: RTC:[vendor]:[category]:[implementation_id]:[language]:[version]
     *                RTC は固定かつ必須
     *                vendor, category,language,version はオプション
     *                implementation_id は必須
     *                オプションを省略する場合でも ":" は省略不可</li>
     * <li>configuration: [key0]=[value0]&[key1]=[value1]&[key2]=[value2].....
     *                RTCが持つPropertiesの値をすべて上書きすることができる。
     *                key=value の形式で記述し、"&" で区切る</li>
     * </ul>
     *
     * 例えば、<br>
     * RTC:jp.go.aist:example:ConfigSample::1.0?conf.default.str_param0=munya
     * RTC::example:ConfigSample:?conf.default.int_param0=100}
     *
     *   {@.en Target RT-Component names for the creation.<br>
     *   comp_args:     [id]?[configuration] <br>
     *   for examples,<br> 
     * RTC:jp.go.aist:example:ConfigSample::1.0?conf.default.str_param0=munya
     * RTC::example:ConfigSample:?conf.default.int_param0=100}
     * @return
     *   {@.ja 生成したRTコンポーネントのインスタンス}
     *   {@.en Created RT-Component's instances}
     */
    public RTObject_impl createComponent(final String comp_args) {
        rtcout.println(Logbuf.TRACE, 
                            "Manager.createComponent(" + comp_args + ")");
        
        if( comp_args == null || comp_args.equals("") ) {
            return null;
        }

        //------------------------------------------------------------
        // extract "comp_type" and "comp_prop" from comp_arg
        Properties comp_prop = new Properties();
        Properties comp_id = new Properties();
        if (!procComponentArgs(comp_args, comp_id, comp_prop)) {
            return null;
        }

        rtcout.println(Logbuf.PARANOID, 
                "comp_prop.getProperty(\"instance_name\"):" 
                + comp_prop.getProperty("instance_name"));
        if(!comp_prop.getProperty("instance_name").equals("")){
            RTObject_impl comp = 
                    getComponent(comp_prop.getProperty("instance_name"));
            if(comp!=null){
                rtcout.println(Logbuf.PARANOID, 
                    "A RTC with the instance name ("
                    + comp_prop.getProperty("instance_name")
                    +") has started already.");
                return comp;
            }
        }
        m_listeners.rtclifecycle_.preCreate(comp_args);

        //------------------------------------------------------------
        // Because the format of port-name had been changed from <port_name> 
        // to <instance_name>.<port_name>, the following processing was added. 
        if (comp_prop.findNode("exported_ports") != null) {
            String[] exported_ports;
            exported_ports = comp_prop.getProperty("exported_ports").split(",");
            String exported_ports_str = "";
            for (int i=0, len=exported_ports.length; i < len; ++i) {
               String[] keyval = exported_ports[i].split("\\.");
               if (keyval.length > 2) {
                   exported_ports_str 
                           += (keyval[0] + "." + keyval[keyval.length-1]);
               }
               else {
                   exported_ports_str += exported_ports[i];
               }
	        
               if (i != exported_ports.length - 1) {
                   exported_ports_str += ",";
               }
            }
            comp_prop.setProperty("exported_ports",exported_ports_str);
            comp_prop.setProperty("conf.default.exported_ports",
                                                    exported_ports_str);
        }

        //------------------------------------------------------------
        // Create Component
        RTObject_impl comp = null;
        Properties prop = new Properties();
        int i,len;
        FactoryBase factory = findPropertyFormFactory(comp_id);
        rtcout.println(Logbuf.PARANOID, 
                        "findPropertyFormFactory(comp_id):" +factory);
        if (factory == null) {
            rtcout.println(Logbuf.ERROR, 
                "Factory not found: " 
                + comp_id.getProperty("implementaion_id"));

            if (!StringUtil.toBool(
                   prop.getProperty("manager.modules.search_auto"), 
                   "YES", "NO", true)) {
                return null;
            }
            // automatic module loading
            Vector<Properties> mp = m_module.getLoadableModules();
            rtcout.println(Logbuf.INFO, 
                + mp.size() + " loadable modules found");

            boolean find = false;
            Properties mprop = new Properties();
            java.util.Iterator it = mp.iterator();
            while (it.hasNext()) {
                mprop = (Properties)it.next();
                if( new find_conf(comp_id).equalof(mprop) ) {
                    find = true;
                    break;
                }
            }
            if(find==false){
                rtcout.println(Logbuf.ERROR, 
                "No module for " 
                + comp_id.getProperty("implementation_id")
                +" in loadable modules list");
                return null;
            }
            if(mprop.findNode("module_file_name")==null){
                rtcout.println(Logbuf.ERROR, 
                "Hmm...module_file_name key not found. "); 
                return null;
            }
            // module loading
            rtcout.println(Logbuf.INFO, 
                "Loading module: "+ mprop.getProperty("module_file_name"));
            load(mprop.getProperty("module_file_name"), "");
            factory = findPropertyFormFactory(comp_id);
            if (factory == null){
                rtcout.println(Logbuf.ERROR, 
                    "Factory not found for loaded module: "
                    + comp_id.getProperty("implementation_id"));
                return null;
            }

        }
        prop = factory.profile();


        final String[] inherit_prop = {
            "config.version",
            "openrtm.name",
            "openrtm.version",
            "os.name",
            "os.release",
            "os.version",
            "os.arch",
            "os.hostname",
            "corba.endpoints",
            "corba.endpoints_ipv4",
            "corba.endpoints_ipv6",
            "corba.id",
            "exec_cxt.periodic.type",
            "exec_cxt.periodic.rate",
            "exec_cxt.evdriven.type",
            "exec_cxt.sync_transition",
            "exec_cxt.sync_activation",
            "exec_cxt.sync_deactivation",
            "exec_cxt.sync_reset",
            "exec_cxt.transition_timeout",
            "exec_cxt.activation_timeout",
            "exec_cxt.deactivation_timeout",
            "exec_cxt.reset_timeout",
            "exec_cxt.cpu_affinity",
            "logger.enable",
            "logger.log_level",
            "naming.enable",
            "naming.type",
            "naming.formats",
            "manager.instance_name",
            ""
        };

        Properties prop_ = prop.getNode("port");
        prop_.merge(m_config.getNode("port"));
         
       
        comp = factory.create(this);
        rtcout.println(Logbuf.PARANOID, 
                        "factory.create(this):" +comp);
        if (comp == null) {
            rtcout.println(Logbuf.ERROR, 
                "RTC creation failed: " 
                + comp_id.getProperty("implementaion_id"));
            return null;
        }

        if (m_config.getProperty("corba.endpoints_ipv4").equals("")) {
            setEndpointProperty(comp.getObjRef());
        }

        for (int ic=0; inherit_prop[ic].length() != 0; ++ic) {
            
            String key = inherit_prop[ic];
            if (m_config.findNode(key) != null) {
                prop.setProperty(key,m_config.getProperty(key));
            }
        }


        rtcout.println(Logbuf.TRACE, 
            "RTC Created: " + comp_id.getProperty("implementaion_id"));

        m_listeners.rtclifecycle_.postCreate(comp);
        prop.merge(comp_prop);
    
        //------------------------------------------------------------
        // Load configuration file specified in "rtc.conf"
        //
        // rtc.conf:
        // [category].[type_name].config_file = file_name
        // [category].[instance_name].config_file = file_name
        m_listeners.rtclifecycle_.preConfigure(prop);
        configureComponent(comp, prop);

        m_listeners.rtclifecycle_.postConfigure(prop);
        // comp.setProperties(prop);

        //------------------------------------------------------------
        // Component initialization
        m_listeners.rtclifecycle_.preInitialize();
        if( comp.initialize() != ReturnCode_t.RTC_OK ) {
            rtcout.println(Logbuf.TRACE, 
                "RTC initialization failed: " 
                + comp_id.getProperty("implementaion_id"));
            if (comp.exit() != ReturnCode_t.RTC_OK) {
                rtcout.println(Logbuf.DEBUG, 
                    comp_id.getProperty("implementation_id")
                    + " finalization was failed.");
            }
            return null;
        }
        rtcout.println(Logbuf.TRACE, 
            "RTC initialization succeeded: " 
            + comp_id.getProperty("implementaion_id"));

        m_listeners.rtclifecycle_.postInitialize();
        //------------------------------------------------------------
        // Bind component to naming service
        registerComponent(comp);

        return comp;
    }
    
    /**
     * {@.ja m_factoryから指定のプロパティと同じimplementation_idをもつ
     * FactoryBaseを探す。}
     *
     */
    private FactoryBase findPropertyFormFactory(Properties comp_id) {
        FactoryBase factory = null;
        for (int i=0, len=m_factory.m_objects.size(); i < len; ++i) {
            factory = m_factory.m_objects.elementAt(i);
            if(factory == null){
                continue;
            }
            String str = factory.m_Profile.getProperty("implementation_id");
            if (str.equals(comp_id.getProperty("implementation_id"))) {
                return factory;
            }
        }
        return null;
    }
    /**
     * {@.ja RTコンポーネントの登録解除。}
     * {@.en Unregister RT-Components}
     *
     * <p>
     * {@.ja 指定したRTコンポーネントのインスタンスをネーミングサービスから
     * 登録解除する。}
     * {@.en Unregister specified RT-Component's instances 
     * from naming service.}
     *
     * @param comp 
     *   {@.ja 登録解除対象RTコンポーネントオブジェクト}
     *   {@.en Target RT-Components for the unregistration}
     */
    public void cleanupComponent(RTObject_impl comp) {
        
        rtcout.println(Logbuf.TRACE, "Manager.cleanupComponent()");
        
        unregisterComponent(comp);
    }
    
    /**
     * {@.ja RTコンポーネントの削除するためのリスナークラス}
     * {@.en Listener Class for deletion of RT component}
     *
     */
    class cleanupComponentsClass implements CallbackFunction {

        private Manager m_mgr;
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         */
        public cleanupComponentsClass() {
//            m_mgr = Manager.instance(); 
        }
        /**
         * {@.ja コールバックメソッド}
         * {@.en Callback method}
         */
        public void doOperate() {
            cleanupComponents();
        }
        /**
         * {@.ja RTコンポーネントの削除する。}
         * {@.en This method deletes RT-Components.}
         *
         * <p>
         * {@.ja notifyFinalized()によって登録されたRTコンポーネントを
         * 削除する。}
         * {@.en This method deletes RT-Components registered by 
         * notifyFinalized().} 
         *
         */
        public void cleanupComponents() {
            rtcout.println(Logbuf.VERBOSE, "Manager.cleanupComponents()");
            synchronized (m_finalized.mutex){
                rtcout.println(Logbuf.VERBOSE,  
                    m_finalized.comps.size()
                    +" components are marked as finalized.");
                for (int i=0; i < m_finalized.comps.size(); ++i) {
                    deleteComponent(m_finalized.comps.elementAt(i));
                }
                m_finalized.comps.clear();
            }
        }

    }
    /**
     * {@.ja タイマー処理用リスナー}
     * {@.en Listener for timer processing}
     */
    cleanupComponentsClass m_cleanupComponents = new cleanupComponentsClass();

    /**
     * {@.ja RTコンポーネントの削除する}
     * {@.en This method deletes RT-Components.}
     *
     * <p>
     * {@.ja 削除するRTコンポーネントを登録する。
     * 登録されたRTコンポーネントは cleanupComponents() で削除される。}
     * {@.en The deleted RT-Component is registered. 
     * The registered RT-Components 
     * are deleted by cleanupComponents().}
     *
     * @param comp
     *   {@.ja 削除するRTコンポーネント}
     *   {@.en Deleted RT component}
     *
     *
     */
    public void notifyFinalized(RTObject_impl comp) {
        rtcout.println(Logbuf.TRACE, "Manager.notifyFinalized()");
        synchronized (m_finalized.mutex){
            m_finalized.comps.add(comp);
        }
        if (m_timer == null) {
            cleanupComponents_();
        }
    }

    private void cleanupComponents_() {
        m_finalizer.exec();
    }
    protected class Finalizer implements Runnable {
        public Finalizer(){
        }
        public void exec(){
            Thread t = new Thread(this);
            t.start();
        }
        public void run(){
            m_cleanupComponents.doOperate();
        }
    }
    private Finalizer m_finalizer = new Finalizer(); 
    /**
     * <p> procComponentArgs </p>
     *
     * @param comp_arg String
     * @param comp_id Properties
     * @param comp_conf Propertie
     * @return boolean
     *
     */
    /**
     * {@.ja 引数文字列からコンポーネント型名・プロパティを抽出する。}
     * {@.en Extracting component type/properties from the given string}
     *
     * <p>
     * {@.ja 文字列からコンポーネント型とコンポーネントのプロパティを抽出する。
     * 与えられる文字列のフォーマットは RTC の ID とコンフィギュレーショ
     * ンからなる
     *
     * [RTC type]?[key(0)]=[val(0)]&[key(1)]=[val(1)]&...&[key(n)]=[val(n)]
     * 
     * である。なお、RTC type は implementation_id のみ、もしくは、下記
     * の RTC ID 形式
     *
     * RTC:[vendor]:[category]:[impl_id]:[version]
     *
     * を受け付ける。戻り値である、comp_id は、
     * "vendor", "category", "implementation_id", "version" のキーを持つ
     * Properties 型のオブジェクトとして返される。
     * comp_conf には "?" 以下に記述されるコンポーネントに与えるプロパティ
     * が Properties 型のオブジェクトとして返される。}
     * {@.en This operation extracts component type name and its properties
     * from the figen character string.
     * The given string formats is the following.
     *
     * [RTC type]?[key(0)]=[val(0)]&[key(1)]=[val(1)]...[key(n)]=[val(n)]
     *
     * Returned value "comp_id" has keys of "vendor", "category",
     * "implementation_id", "version", and returned as Properties type
     * object. "comp_conf" is returned as Properties type object
     * includeing component properties to be given to component.}
     * 
     * @param comp_arg 
     *   {@.ja 処理すべき文字列}
     *   {@.en character string to be processed}
     * @param comp_id 
     *   {@.ja 抽出されたコンポーネントの型名}
     *   {@.en extracted component type name}
     * @param comp_conf
     *   {@.ja 抽出されたコンポーネントのプロパティ}
     *   {@.en extracted component's properties}
     *
     * @return 
     *   {@.ja comp_arg にコンポーネント型が含まれていない場合false}
     *   {@.en comp_arg false will returned if no component type in arg}
     *
     */
    public boolean procComponentArgs(final String comp_arg,
                                     Properties comp_id,
                                     Properties comp_conf) {
        rtcout.println(Logbuf.TRACE, "Manager.procComponentArgs("+comp_arg+")");

        String[] id_and_conf = comp_arg.split("\\?");
        // arg should be "id?key0=value0&key1=value1...".
        // id is mandatory, conf is optional
        if (id_and_conf.length != 1 && id_and_conf.length != 2) {
            rtcout.println(Logbuf.ERROR, 
                "args devided into " + id_and_conf.length);
            rtcout.println(Logbuf.ERROR, 
                "Invalid arguments. Two or more '?' in arg : " + comp_arg);
            return false;
        }
        /*
        final String[] prof = {
          "RTC",
          "vendor",
          "category",
          "implementation_id",
          "language",
          "version"
        };
        */
        String[] prof = (String[])CompParam.prof_list.toArray(new String[0]);
        int param_num = prof.length;

        if (id_and_conf[0].indexOf(":") == -1) {
            id_and_conf[0] = prof[0] + ":::" + id_and_conf[0];
            id_and_conf[0] = id_and_conf[0].concat("::");
        }
        String[] id = id_and_conf[0].split(":",-1);

        // id should be devided into 1 or 6 elements
        // RTC:[vendor]:[category]:impl_id:[language]:[version] => 6
        if (id.length != param_num) {
            rtcout.println(Logbuf.ERROR, 
                    "Invalid RTC id format.: " + id_and_conf[0]);
            return false;
        }

        if (id[0].trim().equals(prof[0]) == false) {
            rtcout.println(Logbuf.ERROR, "Invalid id type: " + id[0]);
            return false;
        }
        for (int i = 1; i < param_num; ++i) {
            comp_id.setProperty(prof[i], id[i].trim());
            rtcout.println(Logbuf.TRACE, 
                "RTC basic propfile " + prof[i] + ":" + id[i].trim());
        }

        if (id_and_conf.length == 2) {
            String[] conf = id_and_conf[1].split("&");
            for (int i = 0, len = conf.length; i < len; ++i) {
                if (conf[i]==null) { 
                    continue; 
                }
                String[] keyval = conf[i].split("=", -1);
                if (keyval.length != 2) { 
                    continue; 
                }
                comp_conf.setProperty(keyval[0].trim(), keyval[1].trim());
                rtcout.println(Logbuf.TRACE, 
                    "RTC property " + keyval[0] + ":" + keyval[1]);
            }
        }
        return true;
    }

    /**
     * {@.ja RTコンポーネントを直接 Manager に登録する。}
     * {@.en Register RT-Component directly without Factory}
     *
     * <p>
     * {@.ja 指定したRTコンポーネントのインスタンスを
     * ファクトリ経由ではなく直接マネージャに登録する。}
     * {@.en Register specified RT-Component's instances not via Factory
     * to Manager directly.}
     *
     * @param comp 
     *   {@.ja 登録対象のRTコンポーネントオブジェクト}
     *   {@.en Target RT-Component's instances for the registration}
     *
     * @return 
     *   {@.ja 正常に登録できた場合はtrueを、さもなくばfalseを返す。}
     *   {@.en Registration result (Successful:true, Failed:false)}
     */
    public boolean registerComponent(RTObject_impl comp) {
        
        rtcout.println(Logbuf.TRACE, "Manager.registerComponent("
                + comp.getInstanceName() + ")");
        
        // NamingManagerのみで代用可能
        m_compManager.registerObject(comp, new InstanceName(comp));

        
        String[] names = comp.getNamingNames();
        m_listeners.naming_.preBind(comp,names);
        for (int ic = 0; ic < names.length; ++ic) {
            rtcout.println(Logbuf.TRACE, "Bind name: " + names[ic]);
            
            m_namingManager.bindObject(names[ic], comp);
        }
        m_listeners.naming_.postBind(comp,names);

        publishPorts(comp);
        subscribePorts(comp);
       
        try {
            Object obj = m_pORB.resolve_initial_references("omniINSPOA");
            POA poa = POAHelper.narrow(obj);
            poa.the_POAManager().activate();
            String id = comp.getCategory() + "/" + comp.getInstanceName();
            byte[] oid = id.getBytes();
            poa.activate_object_with_id(oid, comp);
        }
        catch(Exception ex){
                rtcout.println(Logbuf.DEBUG, 
                        "Exception caught."+ex.toString());
        } 
        return true;
    }
    
    /**
     * {@.ja RTコンポーネントの登録を解除する。}
     * {@.en Unregister RT-Components}
     *
     * <p>
     * {@.ja 指定したRTコンポーネントの登録を解除する。}
     * {@.en Unregister specified RT-Components}
     *
     * @param comp 
     *   {@.ja 登録解除するRTコンポーネントオブジェクト}
     *   {@.en Target RT-Component's instances for the unregistration}
     *
     * @return 
     *   {@.ja 登録解除処理結果(解除成功:true、解除失敗:false)}
     *   {@.en Unregistration result (Successful:true, Failed:false)}
     */
    public boolean unregisterComponent(RTObject_impl comp) {
        
        rtcout.println(Logbuf.TRACE, "Manager.unregisterComponent("
                + comp.getInstanceName() + ")");
        
        // NamingManager のみで代用可能
        m_compManager.unregisterObject(new InstanceName(comp));
        
        String[] names = comp.getNamingNames();
        m_listeners.naming_.preUnbind(comp, names);
        for (int i = 0; i < names.length; ++i) {
            rtcout.println(Logbuf.TRACE, "Unbind name: " + names[i]);
            
            m_namingManager.unbindObject(names[i]);
        }
        m_listeners.naming_.postUnbind(comp, names);
        
        try {
            Object obj = m_pORB.resolve_initial_references("omniINSPOA");
            POA poa = POAHelper.narrow(obj);
            poa.the_POAManager().activate();
            String id = comp.getCategory() + "/" + comp.getInstanceName();
            byte[] oid = id.getBytes();
            poa.deactivate_object(oid);
        }
        catch(Exception ex){
                rtcout.println(Logbuf.DEBUG, 
                        "Exception caught."+ex.toString());
        } 
        return true;
    }
    
    /**
     * {@.ja Contextを生成する。}
     * {@.en Create Context}
     *
     * @param ec_args
     *   {@.ja 引数}
     *   {@.en Arguments}
     * @return 
     *   {@.ja 生成したConetextのインスタンス}
     *   {@.en @return Created Context's instances}
     */
    public ExecutionContextBase createContext(final String ec_args) {
        rtcout.println(Logbuf.TRACE, "Manager.createContext("+ec_args+")");
        rtcout.println(Logbuf.TRACE, "ExecutionContext type: " + m_config.getProperty("exec_cxt.periodic.type") );

        StringBuffer ec_id = new StringBuffer();
        Properties ec_prop = new Properties();
        if (!procContextArgs(ec_args, ec_id, ec_prop)) {
            return null;
        }

        ECFactoryBase factory = (ECFactoryBase)m_ecfactory.find(new ECFactoryPredicate(ec_id.toString()));

        if(factory == null) {
            rtcout.println(Logbuf.ERROR, "Factory not found: " + ec_id);
            return null;
        }

        ExecutionContextBase ec;
        ec = factory.create();
        return ec;

  }
    /**
     * {@.ja 引数文字列からExecutionContext名・プロパティを抽出する。}
     * {@.en Extracting ExecutionContext's name/properties from the given 
     *        string}
     *
     * <p>
     * {@.ja 文字列からExecutionContext名とプロパティを抽出する。
     * 与えられる文字列のフォーマットは RTC の ID とコンフィギュレーショ
     * ンからなる
     *
     * [ExecutionContext名]?[key(0)]
     * =[val(0)]&[key(1)]=[val(1)]&...&[key(n)]=[val(n)]
     * 
     * である。
     *
     * ec_conf には "?" 以下に記述されるコンポーネントに与えるプロパティ
     * が Properties 型のオブジェクトとして返される。}
     * {@.en This operation extracts ExecutionContext's name and its properties
     * from the figen character string.
     * The given string formats is the following.
     *
     * [ExecutionContext's name]?[key(0)]
     * =[val(0)]&[key(1)]=[val(1)]...[key(n)]=[val(n)]
     *
     * "ec_conf" is returned as Properties type object
     * includeing component properties to be given to component.}
     * 
     * @param ec_args 
     *   {@.ja 処理すべき文字列}
     *   {@.en character string to be processed}
     * @param ec_id 
     *   {@.ja 抽出されたExecutionContext名}
     *   {@.en extracted ExecutionContext's name}
     * @param ec_conf 
     *   {@.ja 抽出されたExecutionContextのプロパティ}
     *   {@.en extracted ExecutionContext's properties}
     * @return 
     *   {@.ja ec_args にExecutionContext名が含まれていない場合false}
     *   {@.en ec_arg false will returned if no ExecutionContext's name in arg}
     */
    public boolean procContextArgs(final String ec_args,
                                   StringBuffer ec_id,
                                   Properties ec_conf) {

        rtcout.println(Logbuf.TRACE, "Manager.procContextArgs("+ec_args+","+ec_id.toString()+")");

        String[] id_and_conf = ec_args.split("\\?");
        if (id_and_conf.length != 1 && id_and_conf.length != 2) {
            rtcout.println(Logbuf.ERROR, "Invalid arguments. Two or more '?' in arg : " + ec_args);
            return false;
        }
        if (id_and_conf[0].length() == 0) {
            rtcout.println(Logbuf.ERROR, "Empty ExecutionContext's name");
            return false;
        }
        ec_id.append(id_and_conf[0]);

        if (id_and_conf.length == 2) {
            String[] conf = id_and_conf[1].split("&");
            for (int i=0, len=conf.length; i < len; ++i) {
                String[] k = conf[i].split("=");
                ec_conf.setProperty(k[0], k[1]);
                rtcout.println(Logbuf.TRACE, "EC property "+ k[0] + ":" + k[1]);
             }
        }

        return true;
    }
    /**
     * <p>指定したRTコンポーネントに、ExecutionContextをバインドします。</p>
     * 
     * @param comp バインド対象のRTコンポーネントオブジェクト
     * @return 正常にバインドできた場合はtrueを、さもなくばfalseを返します。
     */
/*
    public boolean bindExecutionContext(RTObject_impl comp) {
        
        rtcout.println(Logbuf.TRACE, "Manager.bindExecutionContext()");
        rtcout.println(Logbuf.TRACE, "ExecutionContext type: "
                + m_config.getProperty("exec_cxt.periodic.type"));

        RTObject rtobj = comp.getObjRef();

        ExecutionContextBase exec_cxt;

        if (RTCUtil.isDataFlowComponent(rtobj)) {
            final String ectype = m_config.getProperty("exec_cxt.periodic.type");

            ECFactoryBase ecfactory = (ECFactoryBase)(m_ecfactory.find(new ECFactoryPredicate(ectype)));
            if( ecfactory == null ) {
                return false;
            }
            exec_cxt = ecfactory.create();
            
            try {
                m_objManager.activate(exec_cxt);
                
            } catch (ServantAlreadyActive e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught ServantAlreadyActive Exception in Manager.bindExecutionContext() DataFlowParticipant.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                
            } catch (WrongPolicy e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught WrongPolicy Exception in Manager.bindExecutionContext() DataFlowParticipant.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                
            } catch (ObjectNotActive e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught ObjectNotActive Exception in Manager.bindExecutionContext() DataFlowParticipant.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
            }
            
            final String rate = m_config.getProperty("exec_cxt.periodic.rate");
            exec_cxt.set_rate(Double.valueOf(rate).doubleValue());
        }
        else {
            final String ectype = m_config.getProperty("exec_cxt.evdriven.type");
            exec_cxt = ((ECFactoryBase) (m_ecfactory.find(new ECFactoryPredicate(ectype)))).create();
            
            try {
                m_objManager.activate(exec_cxt);
                
            } catch (ServantAlreadyActive e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught ServantAlreadyActive Exception in Manager.bindExecutionContext() FsmParticipant.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                
            } catch (WrongPolicy e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught WrongPolicy Exception in Manager.bindExecutionContext() FsmParticipant.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                
            } catch (ObjectNotActive e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught ObjectNotActive Exception in Manager.bindExecutionContext() FsmParticipant.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
            }
        }

        exec_cxt.add_component(rtobj);
        exec_cxt.start();
        m_ecs.add(exec_cxt);
        
        return true;
    }
*/
    
    /**
     * {@.ja  Manager に登録されているRTコンポーネントを削除する。}
     * {@.en Unregister RT-Components that have been registered to Manager}
     *
     * <p>
     * {@.ja マネージャに登録されているRTコンポーネントを削除する。
     * 指定されたRTコンポーネントをネーミングサービスから削除し、
     * RTコンポーネント自体を終了させるとともに、インスタンスを解放する。}
     * {@.en Unregister RT-Components that have been registered to manager
     * Remove specified RT-Component from naming service, terminate itself
     * and release its instances.}
     * </p>
     *
     * @param comp 
     *   {@.ja 削除対象RTコンポーネントのインスタンス}
     *   {@.en Target RT-Component's instances for the unregistration}
     *
     */
    public void deleteComponent(RTObject_impl comp) {
        rtcout.println(Logbuf.TRACE, 
                "Manager.deleteComponent(RTObject)");
        // cleanup from manager's table, and naming serivce
        unregisterComponent(comp);

        // find factory
        Properties comp_id = comp.getProperties();
	FactoryBase factory 
            = (FactoryBase)m_factory.find(
                  new FactoryPredicate(
                      comp.getProperties().getProperty("implementation_id")));
        if (factory == null) {
            rtcout.println(Logbuf.DEBUG, 
                "Factory not found: "+comp_id.getProperty("implementation_id"));
            return;
        }
        else {
            rtcout.println(Logbuf.DEBUG, 
                "Factory found: "+comp_id.getProperty("implementation_id"));
            comp = factory.destroy(comp);
        } 
        String shutdown_on_nortcs 
            = m_config.getProperty("manager.shutdown_on_nortcs") ;
        String is_master  = m_config.getProperty("manager.is_master");
        if (StringUtil.toBool(shutdown_on_nortcs, "YES", "NO", true)
            && !StringUtil.toBool(is_master, "YES", "NO", false)) {
            Vector<RTObject_impl> comps = getComponents();
            if (comps.size() == 0) {
                shutdown();
            }
         }
    } 

    /**
     * {@.ja Manager に登録されているRTコンポーネントを削除する。}
     * {@.en Unregister RT-Components that have been registered to Manager}
     *
     * <p>
     * {@.ja マネージャに登録されているRTコンポーネントを削除する。
     * 指定されたRTコンポーネントをネーミングサービスから削除し、
     * RTコンポーネント自体を終了させるとともに、インスタンスを解放する。}
     * {@.en Unregister RT-Components that have been registered to manager
     * Remove specified RT-Component from naming service, terminate itself
     * and release its instances.}
     * </p>
     *
     * @param instanceName
     *     {@.ja 削除対象RTコンポーネントのインスタンス名}
     *     {@.en Target RT-Component's instances for the unregistration}
     */
    public void deleteComponent(final String instanceName) {
        
        rtcout.println(Logbuf.TRACE, 
                "Manager.deleteComponent(" + instanceName + ")");
        RTObject_impl comp = null;
        comp = m_compManager.find(new InstanceName(instanceName));
        if (comp == null) {
            rtcout.println(Logbuf.TRACE, 
                "RTC "+instanceName+" was not found in manager.");
            return;
        }
        deleteComponent(comp);
    }
    
    /**
     * <p>Managerに登録されているRTコンポーネントを取得します。</p>
     * <p>※未実装</p>
     *
     */
    /**
     * {@.ja Manager に登録されているRTコンポーネントを検索する。}
     * {@.en Get RT-Component's pointer}
     *
     * <p>
     * {@.ja Manager に登録されているRTコンポーネントを指定した名称で検索し、
     * 合致するコンポーネントを取得する。}
     * {@.en Search RT-Component that has been registered to Manager 
     * by its specified name, and get it that matches.}
     *
     *
     * @param instanceName 
     *   {@.ja 取得対象RTコンポーネント名}
     *   {@.en Target RT-Component's name for searching}
     *
     * @return 
     *   {@.ja 対象RTコンポーネントオブジェクト}
     *   {@.en Target RT-Component's instances that matches}
     */
    public RTObject_impl getComponent(final String instanceName) {
        
        rtcout.println(Logbuf.TRACE, "Manager.getComponent(" + instanceName + ")");
        return m_compManager.find(new InstanceName(instanceName));
    }
    
    /**
     * {@.ja Manager に登録されている全RTコンポーネントを取得する。}
     * {@.en Get all RT-Components registered in the Manager}
     *
     * <p>
     * {@.ja Manager に登録されているRTコンポーネントの全インスタンスを
     * 取得する。}
     * {@.en Get all RT-Component's instances that have been registered 
     * to Manager.}
     *
     * @return 
     *   {@.ja 全RTコンポーネントのインスタンスリスト}
     *   {@.en List of all RT-Component's instances}
     */
    public Vector<RTObject_impl> getComponents() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getComponents()");
        
        return m_compManager.getObjects();
    }
    
    /**
     *
     */
    public void
    addManagerActionListener(ManagerActionListener listener,
                             boolean autoclean) {
        m_listeners.manager_.addObserver(listener);
    }
    /**
     *
     */
    public void
    addManagerActionListener(ManagerActionListener listener){
        addManagerActionListener(listener,true);
    }
    /**
     *
     */
    public void
    removeManagerActionListener(ManagerActionListener listener){
        m_listeners.manager_.deleteObserver(listener);
    }

    /**
     *
     */
    public void
    addModuleActionListener(ModuleActionListener listener,
                             boolean autoclean) {
        m_listeners.module_.addObserver(listener);
    }
    /**
     *
     */
    public void
    addModuleActionListener(ModuleActionListener listener){
        addModuleActionListener(listener,true);
    }
    /**
     *
     */
    public void
    removeModuleActionListener(ModuleActionListener listener){
        m_listeners.module_.deleteObserver(listener);
    }

    /**
     *
     */
    public void
    addRtcLifecycleActionListener(RtcLifecycleActionListener listener,
                                  boolean autoclean){
        m_listeners.rtclifecycle_.addObserver(listener);
    }
    /**
     *
     */
    public void
    addRtcLifecycleActionListener(RtcLifecycleActionListener listener){
        addRtcLifecycleActionListener(listener,true);
    }
    /**
     *
     */
    public void
    removeRtcLifecycleActionListener(RtcLifecycleActionListener listener){
        m_listeners.rtclifecycle_.deleteObserver(listener);
    }
    
    /**
     *
     */
    public void
    addNamingActionListener(NamingActionListener listener,
                            boolean autoclean){
        m_listeners.naming_.addObserver(listener);
    }
    /**
     *
     */
    public void
    addNamingActionListener(NamingActionListener listener){
        addNamingActionListener(listener,true);
    }
    /**
     *
     */
    public void
    removeNamingActionListener(NamingActionListener listener){
        m_listeners.naming_.deleteObserver(listener);
    }
    
    /**
     *
     */
    public void
    addLocalServiceActionListener(LocalServiceActionListener listener,
                                       boolean autoclean){
        m_listeners.localservice_.addObserver(listener);
    }
    /**
     *
     */
    public void
    addLocalServiceActionListener(LocalServiceActionListener listener){
        addLocalServiceActionListener(listener,true);
    }
    /**
     *
     */
    public void
    removeLocalServiceActionListener(LocalServiceActionListener listener){
        m_listeners.localservice_.deleteObserver(listener);
    }

 
    /**
     * {@.ja ORB のポインタを取得する。}
     * {@.en Get the pointer to ORB}
     *
     * <p>
     * {@.ja Manager に設定された ORB のポインタを取得する。}
     * {@.en Get the pointer to ORB that has been set to Manager.}
     *
     * @return 
     *   {@.ja ORB オブジェクト}
     *   {@.en ORB object}
     *
     */
    public ORB getORB() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getORB()");
        
        return m_pORB;
    }
    
    /**
     * {@.ja Manager が持つ RootPOA のポインタを取得する。}
     * {@.en Get a pointer to RootPOA held by Manager}
     *
     * <p>
     * {@.ja Manager に設定された RootPOA へのポインタを取得する。}
     * {@.en Get the pointer to RootPOA that has been set to Manager.}
     *
     * @return 
     *   {@.ja RootPOAオブジェクト}
     *   {@.en RootPOA object}
     */
    public POA getPOA() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getPOA()");
        
        return m_pPOA;
    }

    /**
     * {@.ja Manager が持つ POAManager を取得する。}
     * {@.en Get POAManager that Manager has}
     *
     * <p>
     * {@.ja Manager に設定された POAMAnager を取得する。}
     * {@.en Get POAMAnager that has been set to Manager.}
     *
     * @return 
     *   {@.ja POAマネージャ}
     *   {@.en POA manager}
     */
    public POAManager getPOAManager() {
        
        rtcout.println(Logbuf.TRACE, "Manager.getPOAManager()");
        
        return m_pPOAManager;
    }
    
    /**
     * {@.ja Manager の内部初期化処理。}
     * {@.en Manager internal initialization}
     *
     * <p> 
     * {@.ja Manager の内部初期化処理を実行する。
     * <ul>
     * <li> Manager コンフィギュレーションの設定
     * <li> ログ出力ファイルの設定
     * <li> 終了処理用スレッドの生成
     * <li> タイマ用スレッドの生成(タイマ使用時)
     * </ul>}
     * {@.en Execute Manager's internal initialization processing.
     * <ul>
     * <li> Set Manager configuration
     * <li> Set log output file
     * <li> Create termination processing thread
     * <li> Create timer thread (when using timer)
     * </ul>}
     *
     * @param argv 
     *   {@.ja コマンドライン引数}
     *   {@.en Commandline arguments}
     */
    protected void initManager(String[] argv) throws Exception {
        
        ManagerConfig config = null;
        try{
            config = new ManagerConfig(argv);
        }
        catch(IllegalArgumentException e){
            rtcout.println(Logbuf.WARN, "Could not parse arguments.");
        }
        if (m_config == null) {
            m_config = new Properties();
        }
        
        config.configure(m_config);
        m_config.setProperty("logger.file_name",
              formatString(m_config.getProperty("logger.file_name"), m_config));
        m_module = new ModuleManager(m_config);
        m_terminator = new Terminator(this);

        synchronized (m_terminator) {
            m_terminate_waiting = 0;
        }

        if (StringUtil.toBool(m_config.getProperty("timer.enable"), 
                              "YES", "NO", true)) {
            TimeValue tm = new TimeValue(0, 100);
            String tick = m_config.getProperty("timer.tick");
            if (! (tick == null || tick.equals(""))) {
                tm.convert((Double.valueOf(tick)).doubleValue());
                m_timer = new Timer(tm);
                m_timer.start();
            }
        }
        if (StringUtil.toBool(m_config.getProperty("manager.shutdown_auto"), 
                              "YES", "NO", true)  &&
            !StringUtil.toBool(m_config.getProperty("manager.is_master"), 
                              "YES", "NO", false) ) {
            TimeValue tm = new TimeValue(10, 0);
            if (m_config.findNode("manager.auto_shutdown_duration") != null) {
                double duration;
                String s = m_config.getProperty("manager.auto_shutdown_duration");
                try{
                    duration = Double.parseDouble(s);
                    tm.convert(duration);
                }
                catch (NumberFormatException ex){
                }
            }
            if (m_timer != null) {
                m_timer.registerListenerObj(m_shutdownOnNoRtcs, tm);
            }
        }
    
        {
            TimeValue tm = new TimeValue(1, 0);
            if (m_timer != null) {
                m_timer.registerListenerObj(m_cleanupComponents, tm);
            }
        }

        // To load SSL communication modules.
        // ex)
        //   manager.modules.preload: SSLTransport
        String[] mods
            = m_config.getProperty("manager.preload.modules").split(",");

        for (int ic=0; ic < mods.length; ++ic) {
            if ( mods[ic].length() == 0) {
                continue;
            }
	    mods[ic] = mods[ic].trim();
            String[] str = mods[ic].split("\\.");
            String basename = str[0]+"Init";
            try {
                m_module.load(mods[ic], basename);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                rtcout.println(Logbuf.ERROR, "Module load error: " + mods[ic]);
            } catch (ClassNotFoundException e) {
                rtcout.println(Logbuf.ERROR, "Module not found: " + mods[ic]);
            } catch (Exception ex) {
                rtcout.println(Logbuf.ERROR, "Unknown Exception");
            }
        }

        String form = m_config.getProperty("manager.instance_name");
        String i_name = formatString(form,m_config);
        m_config.setProperty("manager.instance_name",i_name);
    }
    
    /**
     * {@.ja Manager の終了処理}
     * {@.en Shutdown Manager}
     *
     * <p>
     * {@.ja Manager を終了する}
     * {@.en Shutdown Manager}
     * </p>
     *
     */
    protected void shutdownManager() {
        
        rtcout.println(Logbuf.TRACE, "Manager.shutdownManager()");
        if (m_timer != null) {
            m_timer.stop();
        }
    }
    /**
     * {@.ja Manager の終了のためのリスナークラス}
     * {@.en Listener Class for deletion of Manager}
     */
    class shutdownOnNoRtcsClass implements CallbackFunction {
        private Manager m_mgr;
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         */
        public shutdownOnNoRtcsClass() {
//            m_mgr = Manager.instance(); 
        }
        /**
         * {@.ja コールバックメソッド}
         * {@.en Callback method}
         */
        public void doOperate() {
            shutdownOnNoRtcs();
        }
        /**
         * {@.ja Manager の終了処理}
         * {@.en Shutdown Manager}
         *
         * <p>
         * {@.ja configuration の "manager.shutdown_on_nortcs" YES で、
         * コンポーネントが登録されていない場合 Manager を終了する。}
         * {@.en This method shutdowns Manager as follows.
         * <ul>
         * <li> "Manager.shutdown_on_nortcs" of configuration is YES. 
         * <li> The component is not registered.
         * </ul>}
         *
         */
        protected void shutdownOnNoRtcs(){
            rtcout.println(Logbuf.TRACE, "Manager.shutdownOnNoRtcs()");
            if (StringUtil.toBool(
                m_config.getProperty("manager.shutdown_on_nortcs"), 
                "YES", "NO", true)) {

                Vector<RTObject_impl> comps = getComponents();
                if (comps.size() == 0) {
                    shutdown();
                }
            }
        }

    }
    /**
     * {@.ja タイマー処理用リスナー}
     * {@.en Listener for timer processing}
     */
    shutdownOnNoRtcsClass m_shutdownOnNoRtcs = new shutdownOnNoRtcsClass();
  
    /**
     * {@.ja System logger の初期化。}
     * {@.en System logger initialization}
     *
     * <p>
     * {@.ja System logger の初期化を実行する。
     * コンフィギュレーションファイルに設定された情報に基づき、
     * ロガーの初期化，設定を実行する。}
     * {@.en Initialize System logger.
     * Initialize logger and set it according to the set information in
     * configuration file,}
     *
     * @return 
     *   {@.ja 正常に初期化できた場合はtrueを、さもなくばfalseを返す。}
     *   {@.en Initialization result (Successful:true, Failed:false)}
     */
    protected boolean initLogger() {
        
        rtcout.setLevel("SILENT");
        
        if (StringUtil.toBool(m_config.getProperty("logger.enable"), "YES", "NO", true)) {
            
            rtcout.setEnabled();
            String[] logouts = m_config.getProperty("logger.file_name").split(",");
            for (int i=0; i < logouts.length; ++i) {
                String logfile = logouts[i].trim();
                if (logfile == null) continue;

                // Open logfile
                if (logfile.equals("STDOUT") || logfile.equals("stdout")) {

                    rtcout.addStream(new ConsoleHandler());
                    continue;
                }
                
                try {
                    rtcout.addStream(new FileHandler(logfile));
                }
                catch(IOException ex) {
                    continue;
                }
            }
            
            // Set date format for log entry header
            rtcout.setDateFormat(m_config.getProperty("logger.date_format"));

            rtcout.setClockType(m_config.getProperty("logger.clock_type"));
            
            // Loglevel was set from configuration file.
            rtcout.setLevel(m_config.getProperty("logger.log_level"));

            // Log stream mutex locking mode
            rtcout.setLogLock(StringUtil.toBool(
                    m_config.getProperty("logger.stream_lock"), 
                                                        "enable", "disable", false));

            rtcout.println(Logbuf.INFO, m_config.getProperty("openrtm.version"));
            rtcout.println(Logbuf.INFO, "Copyright (C) 2003-2024, Noriaki Ando and OpenRTM development team,");
            rtcout.println(Logbuf.INFO, "  Intelligent Systems Research Institute, AIST,");
            rtcout.println(Logbuf.INFO, "Copyright (C) 2024, Noriaki Ando and OpenRTM development team,");
            rtcout.println(Logbuf.INFO, "  Industrial Cyber-Physical Research Center, AIST,");
            rtcout.println(Logbuf.INFO, "  All right reserved.");
            rtcout.println(Logbuf.INFO, "Manager starting.");
            rtcout.println(Logbuf.INFO, "Starting local logging.");
        } else {
            rtcout.setDisabled();
            m_config.setProperty("logger.log_level","SILENT");
        }
        
        return true;
    }
    
    /**
     * {@.ja System Logger の終了処理。}
     * {@.en System Logger finalization}
     */
    protected void shutdownLogger() {
        rtcout.println(Logbuf.TRACE, "Manager.shutdownLogger()");
    }
    
    /**
     * {@.ja CORBA ORB の初期化処理}
     * {@.en CORBA ORB initialization}
     * <p>
     * {@.ja 引数により与えられた設定を元にORBを初期化する。}
     * {@.en Initialize ORB based on the configuration given by arguments.}
     * </p>
     * @return  
     *   {@.ja ORB 初期化処理結果(初期化成功:true、初期化失敗:false)}
     *   {@.en ORB initialization result (Successful:true, Failed:false)}
     */
    protected boolean initORB() {

        rtcout.println(Logbuf.TRACE, "Manager.initORB()");
        
        // Initialize ORB
        try {
            String[] args = createORBOptions().split(" ");
            java.util.Properties prop = createORBProperties();

            // ORB initialization
            m_pORB = ORBUtil.getOrb(args, prop);

            // Sets ports of TAG_ALTERNATE_IIOP_ADDRESS(IOR).
            IopIorInterceptor.replacePort0(m_pORB);

            // Get the RootPOA
            Object obj = m_pORB.resolve_initial_references("RootPOA");
            m_pPOA = POAHelper.narrow(obj);
            if (m_pPOA == null) {
                rtcout.println(Logbuf.ERROR, "Could not resolve RootPOA.");
                return false;
            }

            
            // Get the POAManager
            m_pPOAManager = m_pPOA.the_POAManager();
            //m_objManager = new CorbaObjectManager(m_pORB, m_pPOA);

            
        } catch (Exception ex) {
            rtcout.println(Logbuf.DEBUG, 
                "Exception: Caught unknown Exception in Manager.initORB().");
            rtcout.println(Logbuf.DEBUG, ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * {@.ja ORBのコマンドラインオプションを生成します。}
     * {@.en Create ORB command options}
     * <p>
     * {@.ja コンフィギュレーション情報に設定された内容から
     *       ORB の起動時オプションを作成する。}
     * {@.en Create ORB launch options from configuration information
     *       that has been set.}
     * </p>
     * @return 
     *   {@.ja ORB 起動時オプション}
     *   {@.en ORB launch options}
     */
    protected String createORBOptions() {

        String opt = m_config.getProperty("corba.args");
        rtcout.println(Logbuf.DEBUG, "corba.args: "+opt);
        String dumpString = new String();
        //dumpString = m_config._dump(dumpString, m_config, 0);
        //rtcout.println(Logbuf.DEBUG, dumpString);

        Vector<String> endpoints = new Vector<String>();
        
        //Sets ORBInitRef if manager is slave.
        if (!StringUtil.toBool(m_config.getProperty("manager.is_master"), 
                                                    "YES", "NO", false)) {
            //This property is subject to change in future releases
            String mm = m_config.getProperty("corba.master_manager", 
                                            "localhost:2810");
            Properties config = getConfig();
            String name = config.getProperty("manager.name");
            String mgrloc = "corbaloc:iiop:1.2@"+mm+"/"+name;
            rtcout.println(Logbuf.DEBUG, "corbaloc: "+mgrloc);
            opt = opt + " -ORBInitRef manager="  + mgrloc +" ";
     
        }
/*
        createORBEndpoints(endpoints);
        createORBEndpointOption(opt, endpoints);
*/
        rtcout.println(Logbuf.PARANOID, "ORB options: "+opt);
        
        return opt;
    }

    /**
     * {@.ja エンドポイントの生成}
     * {@.en Create Endpoints}
     * <p>
     * {@.ja コンフィグレーションからエンドポイントを生成する。}
     * {@.en Create Endpoints from the configuration.}
     * </p>
     * @param endpoints 
     *   {@.ja エンドポイントリスト}
     *   {@.en endpoints Endpoints list}
     */
    protected void createORBEndpoints(Vector<String> endpoints) {
        // If this process has master manager,
        // master manager's endpoint inserted at the top of endpoints
        rtcout.println(Logbuf.DEBUG, 
            "manager.is_master: "+m_config.getProperty("manager.is_master"));

/* zxc
        if(StringUtil.toBool(m_config.getProperty("manager.is_master"), 
                                                        "YES", "NO", false)){
            String  mm = m_config.getProperty("corba.master_manager", ":2810");
            String[] mmm = mm.split(":");
            if (mmm.length == 2) {
                endpoints.add(0, ":" + mmm[1]);
            }
            else {
                endpoints.add(0, ":2810");
            }
        }
*/
    }

    /**
     * {@.ja ORB の Endpoint のコマンドラインオプション作成}
     * {@.en Create a command optional line of Endpoint of ORB.}
     * 
     * @param opt 
     *   {@.ja コマンドラインオプション}
     *   {@.en ORB options}
     * @param endpoints
     *   {@.ja エンドポイントリスト}
     *   {@.en Endpoints list}
     */
    protected void createORBEndpointOption(String opt, 
                                            Vector<String> endpoints) {
        String corba = m_config.getProperty("corba.id");
        rtcout.println(Logbuf.DEBUG, "corba.id: "+corba);

        for (int i=0; i < endpoints.size(); ++i) {
            String endpoint = endpoints.elementAt(i);
            rtcout.println(Logbuf.DEBUG, "Endpoint is : "+endpoint);
            if (endpoint.indexOf(":") == -1) {
                endpoint += ":"; 
            }

            if (corba.equals("omniORB")) {
                endpoint = StringUtil.normalize(endpoint);
                if (StringUtil.normalize(endpoint).equals("all:")) {
                    // omniORB 4.1 or later
                    opt += " -ORBendPointPublish all(addr)";
                }
                else{
                    opt += " -ORBendPoint giop:tcp:" + endpoint;
                }
            }
            else if (corba == "TAO") {
                opt += "-ORBEndPoint iiop://" + endpoint;
            }
            else if (corba == "MICO") {
                opt += "-ORBIIOPAddr inet:" + endpoint;
            }
        }
    }

    /**
     * {@.jp "corba.endpoints" を分析してエンドポイントを 
     *       IiopAddressComp の listへ出力する。}
     * @param endpoint
     *   {@.jp }
     * @param result
     *   {@.jp }
     */
     private void parsesCorbaEndpointOutputToList(String endpoint ,
                                      java.util.ArrayList result){

        if(endpoint != null)  {
            String[] endPointInfo = endpoint.split(":");
            if( !endPointInfo[0].equals("") ) {
            }
            else{
                rtcout.println(Logbuf.WARN, 
                    "Host of corba.endpoints is illegal." +endPointInfo[0]);
                return;
            }
            short port = 0;
            if( endPointInfo.length>1 ) {
                try {
                    port = (short)Integer.parseInt(endPointInfo[1]);
                }
                catch(Exception ex){
                    rtcout.println(Logbuf.WARN, 
                        "Port of corba.endpoints is illegal." +endPointInfo[1]);
                }
            }
            IiopAddressComp comp = new IiopAddressComp(endPointInfo[0],port);
            result.add(comp);
        }
    }
    /**
     * {@.jp "corba.endpoint" を分析してエンドポイントを 
     *        Map へ出力する。}
     * {@.en Analyzes "corba.endpoint" and outputs the end point to Map.}
     *
     */
     private void parsesCorbaEndpoint(String endpoint ,
                                      java.util.Map result){

        if(endpoint != null)  {
            String[] endPointInfo = endpoint.split(":");
            if( !endPointInfo[0].equals("") ) {
                result.put(Constants.SERVER_HOST, endPointInfo[0]);
            }
            if( endPointInfo.length>1 ) {
                try {
                    short port = (short)Integer.parseInt(endPointInfo[1]);
                    result.put(Constants.SERVER_PORT, endPointInfo[1]);
                }
                catch(Exception ex){
                    rtcout.println(Logbuf.WARN, ""+endPointInfo[1]);
                }
            }
        }
    }
    /**
     * {@.ja プロパティの生成.}
     * {@.en Creates ORB Properties.}
     * @return java.util.Properties
     *   {@.ja  ORB.init() のプロパティ}
     *   {@.en  Property of ORB.init().}
     *
     */
    protected java.util.Properties createORBProperties() {
        java.util.Properties result = new java.util.Properties();

        //Registers Initializers.
        result.put("org.omg.PortableInterceptor.ORBInitializerClass.jp.go.aist.rtm.RTC.InterceptorInitializer","");

        if (StringUtil.toBool(m_config.getProperty("manager.is_master"), 
                                                    "YES", "NO", true)) {
            //This property is subject to change in future releases
            String mm = m_config.getProperty("corba.master_manager", 
                                            "localhost:2810");

            String portNumber[] = mm.split(":");
            result.put(Constants.LISTENER_PORT,
                                                            portNumber[1]);
        }

        //Parses "corba.endpoint".
        String endpoint = m_config.getProperty("corba.endpoint");
        parsesCorbaEndpoint(endpoint, result);

        //Parses "corba.endpoints".
        // Multiple endpoint addresses and ports can be specified using 
        // this option.
        //  Example:
        //   corba.endpoints: 192.168.1.10:1111, 192.168.10.11:2222
        //   corba.endpoints: 192.168.1.10:, 192.168.10.11:
        String endpoints = m_config.getProperty("corba.endpoints");
        if(endpoints != null) {
            endpoints = endpoints.trim();
            endpoints = StringUtil.normalize(endpoints);
            if(endpoints.equals("all")||endpoints.equals("all:")){
                try{
                    java.util.Enumeration<java.net.NetworkInterface> nic 
                         = java.net.NetworkInterface.getNetworkInterfaces();
                    if(nic != null) {
                        endpoints = new String();
                        while(nic.hasMoreElements()) {
                            java.net.NetworkInterface netIf = nic.nextElement();
                            java.util.Enumeration<java.net.InetAddress> enumAddress 
                                    = netIf.getInetAddresses();
                            while(enumAddress.hasMoreElements()){
                                java.net.InetAddress inetAdd 
                                    = enumAddress.nextElement();
                                String hostString = inetAdd.getHostAddress();
                                if(isIpAddressFormat(hostString)){
                                    if(endpoints.length()!=0){
                                        endpoints 
                                            = endpoints + "," + hostString + ":";
                                    }
                                    else{
                                        endpoints = hostString + ":";
                                    }
                                }
                            }
                        }
                    }
                }
                catch(Exception ex){
                }
                if(endpoints == null) {
                    return result;
                }
            }
            java.util.ArrayList<IiopAddressComp> endpointsList = 
                    new java.util.ArrayList<IiopAddressComp>();
            if(endpoints.indexOf(",")!=-1){
                String[] endPoints = endpoints.split(",");
                int loopstart = 0;
                for(int ic=loopstart;ic<endPoints.length;++ic) {
                    if(result.getProperty(Constants.SERVER_HOST)==null){
                        parsesCorbaEndpoint(endPoints[ic], result);
                    }
                    else{
                        parsesCorbaEndpointOutputToList(endPoints[ic], 
                                                    endpointsList);
                    }
                }
                IopIorInterceptor.setEndpoints(endpointsList);
            
            }
            else{
                if(result.getProperty(Constants.SERVER_HOST)==null){
                    parsesCorbaEndpoint(endpoints, result);
                }
                else {
                    parsesCorbaEndpointOutputToList(endpoints, endpointsList);
                }
            }
            IopIorInterceptor.setEndpoints(endpointsList);
        }


        return result;
    }
    
    /**
     * {@.en Checks that the string is IPaddress.}
     */
    private boolean isIpAddressFormat(String string){
        java.util.regex.Pattern pattern 
            = java.util.regex.Pattern.compile(
               "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
        java.util.regex.Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }

    /**
     * {@.ja ORB の終了処理。}
     * {@.en ORB finalization}
     *
     * <p>
     * {@.ja ORB の終了処理を実行する。
     * 実行待ちの処理が存在する場合には、その処理が終了するまで待つ。
     * 実際の終了処理では、POA Managerを非活性化し、 ORB のシャットダウンを実行
     * する。}
     * {@.en Finalize ORB .
     * When the waiting process exists, wait until it completes.
     * In actual finalization, deactivate POA Manager and then shutdown of ORB.}
     */
    protected void shutdownORB() {
        
        rtcout.println(Logbuf.TRACE, "Manager.shutdownORB()");
        
        try {
            while (m_pORB.work_pending()) {
                rtcout.println(Logbuf.PARANOID, "Pending work still exists.");
                
                if (m_pORB.work_pending()) {
                    m_pORB.perform_work();
                }
            }
        } catch (Exception e) {
            rtcout.println(Logbuf.DEBUG, "Exception: Caught unknown Exception in Manager.shutdownORB().");
            rtcout.println(Logbuf.DEBUG, e.getMessage());
        }
        
        rtcout.println(Logbuf.DEBUG, "No pending works of ORB. Shutting down POA and ORB.");

        if (m_pPOA != null) {
            try {
                if (m_pPOAManager != null) {
                    m_pPOAManager.deactivate(false, true);
                    rtcout.println(Logbuf.DEBUG, "POA Manager was deactivated.");
                }
                
                m_pPOA = null;
                
//                rtcout.println(Logbuf.DEBUG, "POA was destroid.");
                
            } catch (SystemException ex) {
                rtcout.println(Logbuf.ERROR, "Caught SystemException during root POA destruction");
                
            } catch (Exception ex) {
                rtcout.println(Logbuf.ERROR, "Caught unknown exception during POA destruction.");
            }
        }

        if (m_pORB != null) {
            try {
                m_pORB.shutdown(true);
                
                rtcout.println(Logbuf.DEBUG, "ORB was shutdown.");
                rtcout.println(Logbuf.DEBUG, "ORB was destroied.");
                
                //m_pORB.destroy();
                m_pORB = null;
                ORBUtil.clearOrb();
                
            } catch (SystemException ex) {
                rtcout.println(Logbuf.ERROR, "Caught SystemException during ORB shutdown");
                
            } catch (Exception ex) {
                rtcout.println(Logbuf.ERROR, "Caught unknown exception during ORB shutdown.");
            }
        }
    }
    
    /**
     * {@.ja NamingManager の初期化。}
     * {@.en NamingManager initialization}
     *
     * <p>
     * {@.ja NamingManager の初期化処理を実行する。
     * ただし、NamingManager を使用しないようにプロパティ情報に設定されている
     * 場合には何もしない。
     * NamingManager を使用する場合、プロパティ情報に設定されている
     * デフォルト NamingServer を登録する。
     * また、定期的に情報を更新するように設定されている場合には、指定された周期
     * で自動更新を行うためのタイマを起動するとともに、更新用メソッドをタイマに
     * 登録する。}
     * {@.en Initialize NamingManager .
     * However, operate nothing, if it is set to property that NamingManager
     * is not used.
     * Register default NamingServer that is set to property information,
     * when NamingManager is used.
     * Also, launch a timer that updates information automatically at specified
     * cycle and register the method for the update to the timer, when it is set
     * to update it reguraly.}
     *
     * @return 
     *   {@.ja 初期化処理結果(初期化成功:true、初期化失敗:false)}
     *   {@.en Initialization result (Successful:true, Failed:false)}
     */
    protected boolean initNaming() {
        
        rtcout.println(Logbuf.TRACE, "Manager.initNaming()");
        
        m_namingManager = new NamingManager(this);

        // If NameService is disabled, return immediately
        if (! StringUtil.toBool(m_config.getProperty("naming.enable"), "YES", "NO", true)) {
            return true;
        }

        // NameServer registration for each method and servers
        String[] meth = m_config.getProperty("naming.type").split(",");

        for (int i = 0; i < meth.length; ++i) {
            String names[] = m_config.getProperty(meth[i] + ".nameservers").split(",");

            for (int j = 0; j < names.length; ++j) {
                rtcout.println(Logbuf.TRACE, 
                    "Register Naming Server: " + meth[i] + " " + names[j]);
                
                String[] nameServer = names[j].split(":");
                if (nameServer.length == 1 && !nameServer[0].equals("")) {
                    names[j] += ":2809";
                }
                
                if (!names[j].equals("")) {
                    m_namingManager.registerNameServer(meth[i], names[j]);
                }
            }
        }

        // NamingManager Timer update initialization
        if (StringUtil.toBool(m_config.getProperty("naming.update.enable"), "YES", "NO", true)) {
            TimeValue tm = new TimeValue(10, 0); // default interval = 10sec
                                                  // for safty
            
            String intr = m_config.getProperty("naming.update.interval");
            if (! (intr == null || intr.equals(""))) {
                tm.convert(Double.valueOf(intr).doubleValue());
            }
            
            if (m_timer != null) {
                m_timer.registerListenerObj(m_namingManager, tm);
            }
        }
        
        return true;
    }
    
    /**
     * {@.ja NamingManager の終了処理。}
     * {@.en NamingManager finalization}
     *
     * <p>
     * {@.ja NamingManager を終了する。
     * 登録されている全要素をアンバインドし、終了する。}
     * {@.en Finalize NamingManager.
     * Unbind all registered elements and shutdown them.}
     */
    protected void shutdownNaming() {
        
        rtcout.println(Logbuf.TRACE, "Manager.shutdownNaming()");
        if(m_namingManager!=null) {
            m_namingManager.unbindAll();
        }
    }
    
    /**
     * {@.ja NamingManager に登録されている RTコンポーネントの終了処理。}
     * {@.en NamingManager finalization}
     *
     * <p>
     * {@.ja NamingManager に登録されているRTコンポーネントのリストを取得し、
     * 全コンポーネントを終了する。}
     * {@.en Get a list of RT-Components that have been registered 
     * to NamingManager,
     * and shutdown all components.}
     */
    protected void shutdownComponents() {
        
        rtcout.println(Logbuf.TRACE, "Manager.shutdownComponents()");
        if(m_namingManager!=null){
        
            Vector<RTObject_impl> comps = m_namingManager.getObjects();
            for (int i=0, len=comps.size(); i < len; ++i) {
                try {
                    comps.elementAt(i).exit();
                    Properties p = new Properties(comps.elementAt(i).getInstanceName());
                    p.merge(comps.elementAt(i).getProperties());
                
                    rtcout.level(Logbuf.PARANOID);

                } catch (Exception e) {
//                e.printStackTrace();
                }
            }
        }
        
        for (int i=0, len=m_ecs.size(); i < len; ++i) {
            try {
                //m_pPOA.deactivate_object(m_pPOA.servant_to_id(m_ecs.elementAt(i).getObjRef()));
                m_pPOA.deactivate_object(m_pPOA.servant_to_id((RTC.ExecutionContextServicePOA)m_ecs.get(i)));
                
            } catch (Exception e) {
                rtcout.println(Logbuf.DEBUG, "Exception: Caught unknown Exception in Manager.shutdownComponents().");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
            }
        }
    }
    
    /**
     * {@.ja RTコンポーネントのコンフィギュレーション処理。}
     * {@.en Configure RT-Component}
     *
     * <p>
     * {@.ja RTコンポーネントの型およびインスタンス毎に
     * 記載されたプロパティファイルの
     * 情報を読み込み、コンポーネントに設定する。
     * また、各コンポーネントの NamingService 登録時の名称を取得し、設定する。}
     * {@.en Read property files described each RT-Component's type and 
     * instance, * and configure it to the component.
     * Also, get each component's registered name when registering to
     * NamingService and configure it.}
     *
     * @param comp 
     *   {@.ja コンフィギュレーション対象RTコンポーネント}
     *   {@.en Target RT-Component for the configuration}
     *
     *
     */
    protected void configureComponent(RTObject_impl comp, 
                                                final Properties prop ) {
        
        String category = comp.getCategory();
        String type_name = comp.getTypeName();
        String inst_name = comp.getInstanceName();
        
        String type_conf = category + "." + type_name + ".config_file";
        String name_conf = category + "." + inst_name + ".config_file";
        
        ArrayList<String> config_fname = new ArrayList<String>();
        Properties type_prop = new Properties();
        Properties name_prop = new Properties();
        
        // Load "category.instance_name.config_file"
        if (!(m_config.getProperty(name_conf) == null
                || m_config.getProperty(name_conf).length() == 0)) {
            
            BufferedReader conff = null;
            try {
                conff = new BufferedReader(
                        new FileReader(m_config.getProperty(name_conf)));
                name_prop.load(conff);
                rtcout.println(Logbuf.INFO, 
                        "Component instance conf file: "
                        + m_config.getProperty(name_conf)
                        + " loaded.");
                String str = new String();
                str = name_prop._dump(str,name_prop,0);
                rtcout.println(Logbuf.DEBUG, str);
                config_fname.add(m_config.getProperty(name_conf));
                conff.close();
            } catch (FileNotFoundException e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught FileNotFoundException"
                    +" in Manager.configureComponent() name_conf.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                
            } catch (IOException e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught IOException"
                    +" in Manager.configureComponent() name_prop.load().");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
            } catch (Exception e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught unknown"
                    +" in Manager.configureComponent() name_conf.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
            }
            if(conff != null){
                try {
                    conff.close();
                }
                catch (Exception e) {
                    rtcout.println(Logbuf.DEBUG, 
                        "Exception: Caught unknown"
                        +" in Manager.configureComponent() clsoe.");
                    rtcout.println(Logbuf.DEBUG, e.getMessage());
                }
            }
        }

        if (m_config.findNode(category + "." + inst_name) != null) {
            Properties temp = m_config.getNode(category + "." + inst_name);
            Vector<String> keys = temp.propertyNames();
            int length = keys.size();
            if (!( length == 1 && keys.lastElement().equals("config_file"))) {
                name_prop.merge(m_config.getNode(category + "." + inst_name));
                rtcout.println(Logbuf.INFO,
                        "Component type conf exists in rtc.conf. Merged.");
                String str = new String();
                str = name_prop._dump(str,name_prop,0);
                rtcout.println(Logbuf.DEBUG,str);
                if (m_config.findNode("config_file") != null) {
                    config_fname.add(m_config.getProperty("config_file"));
                }
            }
        }
    
        if (!(m_config.getProperty(type_conf) == null
                || m_config.getProperty(type_conf).length() == 0)) {
            
            BufferedReader conff = null;
            try {
                conff = new BufferedReader(
                        new FileReader(m_config.getProperty(type_conf)));
                type_prop.load(conff);
                rtcout.println(Logbuf.INFO,
                        "Component type conf file: "
                        + m_config.getProperty(type_conf)
                        + " loaded.");
                String str = new String();
                str = type_prop._dump(str,type_prop,0);
                rtcout.println(Logbuf.DEBUG,str);
                config_fname.add(m_config.getProperty(type_conf));
                conff.close();
                
            } catch (FileNotFoundException e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught FileNotFoundException"
                    +" in Manager.configureComponent() type_conf.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                e.printStackTrace();
                
            } catch (Exception e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught unknown Exception"
                    +" in Manager.configureComponent() type_conf.");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                e.printStackTrace();
            } 
            if(conff != null){
                try {
                    conff.close();
                }
                catch (Exception e) {
                    rtcout.println(Logbuf.DEBUG, 
                        "Exception: Caught unknown Exception"
                        +" in Manager.configureComponent() close error.");
                    rtcout.println(Logbuf.DEBUG, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        if (m_config.findNode(category + "." + type_name) != null) {
            Properties temp = m_config.getNode(category + "." + type_name);
            Vector<String> keys = temp.propertyNames();
            int length = keys.size();
            if (!(length == 1 && keys.lastElement().equals("config_file"))) {
                type_prop.merge(m_config.getNode(category + "." + type_name));
                rtcout.println(Logbuf.INFO,
                        "Component type conf exists in rtc.conf. Merged.");
                String str = new String();
                str = type_prop._dump(str,type_prop,0);
                rtcout.println(Logbuf.DEBUG,str);
                if (m_config.findNode("config_file") != null) {
                    config_fname.add(m_config.getProperty("config_file"));
                }
            }
        }


        // Merge Properties. type_prop is merged properties
        comp.setProperties(prop);
        type_prop.merge(name_prop);
        String fname  = StringUtil.flatten(StringUtil.unique_sv(config_fname));
        type_prop.setProperty("config_file",fname);
        comp.setProperties(type_prop);

        // ------------------------------------------------------------
        // Format component's name for NameService
        StringBuffer naming_formats = new StringBuffer();
        Properties comp_prop = comp.getProperties();

        naming_formats.append(m_config.getProperty("naming.formats"));
        naming_formats.append(", " + comp_prop.getProperty("naming.formats"));
        String naming_formats_result = StringUtil.flatten(
                StringUtil.unique_sv(naming_formats.toString().split(",")));

        comp.getProperties().setProperty("naming.formats", 
                                            naming_formats.toString());
        String naming_names = this.formatString(naming_formats_result, 
                                                    comp.getProperties());
        comp.getProperties().setProperty("naming.names", naming_names);
    }
    
    /**
     * {@.ja ExecutionContextManager の初期化。}
     * {@.en ExecutionContextManager initialization}
     *
     * <p>
     * {@.ja 使用する各 ExecutionContext の初期化処理を実行し、
     * 各 ExecutionContext 
     * 生成用 Factory を ExecutionContextManager に登録する。}
     * {@.en Initialize each ExecutionContext that is used, and register each 
     * ExecutionContext creation Factory to ExecutionContextManager.}
     *
     * @return 
     *   {@.ja 正常に初期化できた場合はtrueを、さもなくばfalseを返す。}
     *   {@.en ExecutionContextManager initialization result
     *          (Successful:true, Failed:false)}
     */
    protected boolean initExecContext() {
        
        rtcout.println(Logbuf.TRACE, "Manager.initExecContext()");
        
        PeriodicExecutionContext.PeriodicExecutionContextInit(this);
        ExtTrigExecutionContext.ExtTrigExecutionContextInit(this);
        OpenHRPExecutionContext.OpenHRPExecutionContextInit(this);
        initCpuAffinity();
        return true;
    }
    /**
     * {@.ja スレッドの CPU affinity マスクを設定・取得する}
     * {@.en et  and get a thread's CPU affinity mask}
     *
     */
    protected void initCpuAffinity() {
        rtcout.println(Logbuf.TRACE, "initCpuAffinity()");
        Properties node = m_config.findNode("manager.cpu_affinity");
        if (node == null) {
            return;
        }
        String affinity_str = m_config.getProperty("manager.cpu_affinity");

        rtcout.println(Logbuf.DEBUG, "CPU affinity property: "+ affinity_str);

        String[] tmp = affinity_str.split(",");
        String osname = System.getProperty("os.name").toLowerCase();
        rtcout.println(Logbuf.DEBUG, "os.name: "+ osname);
        ICPUAffinity CPUAffinity;
        if(osname.startsWith("windows")){
            CPUAffinity = new CPUAffinityWindows();
        }
        else{
            CPUAffinity = new CPUAffinityLinux();
        }

        ArrayList<Integer> cpu_num = new ArrayList<Integer>();
        BitSet cpu_set = new BitSet();
        cpu_set.clear();

        rtcout.println(Logbuf.DEBUG, "cpu_num: ");
        for(int ic=0;ic<tmp.length;++ic){
            int num = Integer.parseInt(tmp[ic]);
            rtcout.println(Logbuf.DEBUG, "    " + num);
            cpu_set.set(num);
        }
        CPUAffinity.setProcessAffinity(cpu_set);
    }
    
    /**
     * {@.ja PeriodicECSharedComposite の初期化。}
     * {@.en PeriodicECSharedComposite initialization}
     *
     * @return 
     *   {@.ja PeriodicECSharedComposite 初期化処理実行結果}
     *         (初期化成功:true、初期化失敗:false)}
     *   {@.en PeriodicECSharedComposite initialization result
     *          (Successful:true, Failed:false)}
     */
    protected boolean initComposite() {
        rtcout.println(Logbuf.TRACE, "Manager.initComposite()");
        PeriodicECSharedComposite.PeriodicECSharedCompositeInit(this);

        return true;
    }

    /**
     * {@.ja ファクトリの初期化。}
     * {@.en Factories initialization}
     *
     * <p>
     * {@.ja バッファ、スレッド、パブリッシャ、プロバイダ、コンシューマの
     * ファクトリを初期化する。}
     * {@.en Initialize buffer factories, thread factories, 
     * publisher factories, 
     * provider factories, and consumer factories.}
     *
     * @return 
     *   {@.ja ファクトリ初期化処理実行結果
     *         (初期化成功:true、初期化失敗:false)}
     *   {@.en PeriodicECSharedComposite initialization result
     *          (Successful:true, Failed:false)}
     */
    protected boolean initFactories() {
        rtcout.println(Logbuf.TRACE, "Manager.initFactories()");
        FactoryInit.init();
        return true;
    }

    /**
     * {@.ja Timer の初期化。}
     * {@.en  Timer initialization}
     *
     * <p>
     * {@.ja 使用する各 Timer の初期化処理を実行する。
     * (現状の実装では何もしない)}
     * {@.en Initialize each Timer that is used.
     * (In current implementation, nothing is done.)}
     *
     * @return 
     *   {@.ja Timer 初期化処理実行結果(初期化成功:true、初期化失敗:false)}
     *   {@.en Timer Initialization result (Successful:true, Failed:false)}
     */
    protected boolean initTimer() {
        return true;
    }
    
    /**
     * {@.ja ManagerServant の初期化}
     * {@.en ManagerServant initialization}
     *
     * <p>
     * @return 
     *     {@.ja 初期化処理実行結果(初期化成功:true、初期化失敗:false)}
     *     {@.en Initialization result (Successful:true, Failed:false)}
     *
     */
    protected boolean initManagerServant() {
        rtcout.println(Logbuf.TRACE, "initManagerServant()");
        if (!StringUtil.toBool(m_config.getProperty("manager.corba_servant"), 
                                                    "YES", "NO", true)) {
            return true;
        }
        m_mgrservant = new ManagerServant();
        if (m_config.getProperty("corba.endpoints_ipv4").equals("")) {
            setEndpointProperty(m_mgrservant.getObjRef());
        }
        Properties prop = m_config.getNode("manager");
        String[] names = prop.getProperty("naming_formats").split(",");
    
        if(StringUtil.toBool(prop.getProperty("is_master"), 
                                                    "YES", "NO", true)) {
            for(int ic=0;ic<names.length;++ic){
                String mgr_name = formatString(names[ic], prop);
                m_namingManager.bindObject(mgr_name, m_mgrservant);
            }
        }
        if(StringUtil.toBool(
                m_config.getProperty("corba.update_master_manager.enable"), 
                "YES", "NO", true) && 
           !StringUtil.toBool(
                m_config.getProperty("manager.is_master"), "YES", "NO", false)
        ) {
            TimeValue tm = new TimeValue(10, 0);
            if(m_config.findNode("corba.update_master_manager.interval")!=null){
                String interval = m_config.getProperty(
                                      "corba.update_master_manager.interval");
                try{
                    double duration = Double.parseDouble(interval);
                    tm.convert(duration);
                }
                catch(Exception ex){
                }
            }
            if(m_timer != null){
                m_updateMasterManager 
                            = new updateMasterManager(m_mgrservant);
                m_timer.registerListenerObj(m_updateMasterManager,tm);
            }
        }
        return true;
    }
    /**
     * {@.ja タイマー処理用リスナー}
     * {@.en Listener for timer processing}
     */
    updateMasterManager m_updateMasterManager;
    /**
     * {@.ja Manager の更新のためのリスナークラス}
     * {@.en Listener Class for update of Manager}
     */
    class updateMasterManager implements CallbackFunction {
        private ManagerServant m_mgrser;
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         */
        public updateMasterManager(ManagerServant mgrser) {
            m_mgrser = mgrser;
        }
        /**
         * {@.ja コールバックメソッド}
         * {@.en Callback method}
         */
        public void doOperate() {
            m_mgrser.update_master_manager();
        }
    }

    /**
     * {@.ja ManagerServantをバインドする。}
     * {@.en Binds ManagerServant.}
     *
     * @return 
     *   {@.ja 実行結果(初期化成功:true、初期化失敗:false)}
     *   {@.en Result (Successful:true, Failed:false)}
     *
     */
    protected boolean bindManagerServant() {

        if (!StringUtil.toBool(m_config.getProperty("manager.corba_servant"), 
                                                    "YES", "NO", true)) {
            return true;
        }
        if( m_mgrservant == null) {
            rtcout.println(Logbuf.ERROR, "ManagerServant is not created.");
            return false;
        }


        Properties prop = (m_config.getNode("manager"));
        String[] names=prop.getProperty("naming_formats").split(",");

        if (StringUtil.toBool(prop.getProperty("is_master"), 
                                                    "YES", "NO", true)) {
            for (int i=0; i < names.length; ++i) {
                String mgr_name = formatString(names[i], prop);
                m_namingManager.bindObject(mgr_name, m_mgrservant);
            }
        }

        return true;
    }

    /**
     * {@.ja ManagerServant オブジェクト}
     * {@.en The object to the ManagerServant}
     */
    ManagerServant m_mgrservant;

    /**
     * {@.ja プロパティ情報のマージ。}
     * {@.en Merge property information}
     *
     * <p>
     * {@.ja 指定されたファイル内に設定されているプロパティ情報をロードし、
     * 既存の設定済みプロパティとマージする。}
     * {@.en Load property information that is configured in the specified file,
     * and merge existing properties that has been configured.}
     *
     * @param properties 
     *   {@.ja 設定対象のPropertiesオブジェクト}
     *   {@.en Target properties for the merge}
     * @param fileName 
     *   {@.ja プロパティファイル名}
     *   {@.en File name that property information is described}
     * @return 
     *   {@.ja 正常に設定できた場合はtrueを、さもなくばfalseを返す。}
     *   {@.en Merge result (Successful:true, Failed:false)}
     *
     */
    protected boolean mergeProperty(Properties properties, final String fileName) {
        
        if (fileName == null) {
            rtcout.println(Logbuf.ERROR, "Invalid configuration file name.");

            return false;
        }
        
        if (! (fileName.length() == 0)) {

            BufferedReader conff = null;
            try {
                conff = new BufferedReader(new FileReader(fileName));
                properties.load(conff);
                conff.close();
                
                return true;

            } catch (FileNotFoundException e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught FileNotFoundException"
                    +" in Manager.mergeProperty().");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                e.printStackTrace();

            } catch (IOException e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught IOException"
                    +" in Manager.mergeProperty() properties.load().");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
            } catch (Exception e) {
                rtcout.println(Logbuf.DEBUG, 
                    "Exception: Caught unknown Exception"
                    +" in Manager.mergeProperty().");
                rtcout.println(Logbuf.DEBUG, e.getMessage());
                e.printStackTrace();
            }
            if(conff != null){
                try {
                    conff.close();
                }
                catch (IOException e) {
                    rtcout.println(Logbuf.DEBUG, 
                        "Exception: Caught IOException"
                        +" in Manager.mergeProperty() close().");
                    rtcout.println(Logbuf.DEBUG, e.getMessage());
                }
            }
        }
        
        return false;
    }
    
    /**
     * {@.ja NamingServer に登録する際の登録情報を組み立てる。}
     * {@.en Construct registration information when registering to 
     *        Naming server}
     *
     * <p>
     * {@.ja 指定された書式とプロパティ情報を基に NameServer に登録する際の
     * 情報を組み立てる。
     * 各書式指定用文字の意味は以下のとおり<ul>
     * <li> % : コンテキストの区切り
     * <li> n : インスタンス名称
     * <li> t : 型名
     * <li> m : 型名
     * <li> v : バージョン
     * <li> V : ベンダー
     * <li> c : カテゴリ
     * <li> h : ホスト名
     * <li> M : マネージャ名
     * <li> p : プロセスID</il></ul>}
     * {@.en Construct information when registering to NameServer 
     * based on specified
     * format and property information.
     * Each format specification character means as follows:<ul>
     * <li> % : Break of Context
     * <li> n : Instance's name
     * <li> t : Type name
     * <li> m : Type name
     * <li> v : Version
     * <li> V : Vender
     * <li> c : Category
     * <li> h : Host name
     * <li> M : Manager name
     * <li> p : Process ID </li></ul>}
     *
     * @param namingFormat 
     *   {@.ja 書式指定}
     *   {@.en Format specification for NamingService registration}
     * @param properties 
     *   {@.ja 出力対象となるPropertiesオブジェクト}
     *   {@.en Property information that is used}
     * @return 
     *   {@.ja Propertiesオブジェクトの内容を文字列出力したもの}
     *   {@.en Specification format conversion result}
     */
    protected String formatString(final String namingFormat, Properties properties) {
        
        StringBuffer str = new StringBuffer();
        int count = 0;

        for (int i = 0; i < namingFormat.length(); ++i) {
            char c = namingFormat.charAt(i);
            if (c == '%') {
                ++count;
                if ((count % 2) == 0) {
                    str.append(c);
                }
            } else if (c == '$') {
                count = 0;
                ++i;
                if (namingFormat.charAt(i) == '{' || namingFormat.charAt(i) == '(') {
                    ++i;
                    String env = "";
                    for ( ; i < namingFormat.length() && namingFormat.charAt(i) != '}' && namingFormat.charAt(i) != ')'; ++i) {
                        env += namingFormat.charAt(i);
                    }
                    String envval = System.getenv(env);
                    if (envval != null) str.append(envval);
                } else {
                    str.append(c);
                }
            } else {
                if (count > 0 && (count % 2) != 0) {
                    count = 0;
                    if (c == 'n') {
                        str.append(properties.getProperty("instance_name"));
                    }
                    else if (c == 't') {
                        str.append(properties.getProperty("type_name"));
                    }
                    else if (c == 'm') {
                        str.append(properties.getProperty("type_name"));
                    }
                    else if (c == 'v') {
                        str.append(properties.getProperty("version"));
                    }
                    else if (c == 'V') {
                        str.append(properties.getProperty("vendor"));
                    }
                    else if (c == 'c') {
                        str.append(properties.getProperty("category"));
                    }
                    else if (c == 'i') {
                        str.append(properties.getProperty("implementation_id"));
                    }
                    else if (c == 'N') {
                        String id = properties.getProperty("implementation_id");
                        String iname = properties.getProperty("instance_name");
                        str.append(iname.substring(id.length()));
                    }
                    else if (c == 'h') {
                        str.append(m_config.getProperty("os.hostname"));
                    }
                    else if (c == 'M') {
                        str.append(m_config.getProperty("manager.name"));
                    }
                    else if (c == 'p') {
                        str.append(m_config.getProperty("manager.pid"));
                    }
                    else {
                        str.append(c);
                    }
                } else {
                    count = 0;
                    str.append(c);
                }
            }
        }
        
        return str.toString();
    }
    
    /**
     * {@.ja 唯一のManagerインスタンス}
     * {@.en This field is the only Manager instance.}
     */
    protected static Manager manager;
    /**
     * {@.ja Manager用ミューテックス変数}
     * {@.en This field is a mutex variable for Manager.}
     */
    protected static String manager_mutex = new String();
    /**
     * {@.ja ORB}
     * {@.en ORB}
     */
    protected ORB m_pORB;
    /**
     * {@.ja POA}
     * {@.en POA}
     */
    protected POA m_pPOA;
    /**
     * {@.ja POAManager}
     * {@.en POAManager}
     */
    protected POAManager m_pPOAManager;
    
    /**
     * {@.ja ユーザコンポーネント初期化プロシジャオブジェクト}
     * {@.en User component initialization procedure object}
     */
    protected ModuleInitProc m_initProc;
    /**
     * {@.ja Managerコンフィギュレーション}
     * {@.en Managaer's configuration Properties}
     */
    protected Properties m_config = new Properties();
    /**
     * {@.ja Module Manager}
     * {@.en Module Manager}
     */
    protected ModuleManager m_module;
    /**
     * {@.ja Naming Manager}
     * {@.en Naming Manager}
     */
    protected NamingManager m_namingManager;
    /**
     * {@.ja CORBA Object Manager}
     * {@.en CORBA Object Manager}
     */
    //protected CorbaObjectManager m_objManager;
    /**
     * {@.ja Timer Object}
     * {@.en Timer Object}
     */
    protected Timer m_timer;
    /**
     * {@.ja ロガーストリーム}
     * {@.en Logger stream}
     */
    protected Logbuf rtcout;
    
    /**
     * {@.ja Object検索用ヘルパークラス}
     * {@.en Helper class to find Object}
     */
    protected class InstanceName implements equalFunctor {
        
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         * @param comp
         *   {@.ja Object}
         *   {@.en Object}
         */
        public InstanceName(RTObject_impl comp) {
            m_name = comp.getInstanceName();
        }
        
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         * @param name 
         *   {@.ja インスタンス名}
         *   {@.en Instance name}
         *
         */
        public InstanceName(final String name) {
            m_name = name;
        }
        
        /**
         * {@.ja Object検索。}
         * {@.en Finds Object}
         *
         * <p>
         * {@.ja 指定されたObjectを検索する}
         * {@.en The specified object is found.}
         *
         * @param comp 
         *   {@.ja Object}
         *   {@.en Object}
         *
         * @return 
         *   {@.ja 存在する場合はtrue}
         *   {@.en Returns true when existing.}
         *
         */
        public boolean equalof(java.lang.Object comp) {
            return m_name.equals(((RTObject_impl)comp).getInstanceName());
        }
        
        /**
         * {@.ja インスタンス名}
         * {@.en Instance name}
         */
        public String m_name;
    }
    
    /**
     * {@.ja Component Manager}
     * {@.en Component Manager}
     */
    protected ObjectManager<String, RTObject_impl> m_compManager = new ObjectManager<String, RTObject_impl>();
    
    /**
     * {@.ja Factory検索用ヘルパークラス}
     * {@.en Helper class to find Factory}
     */
    protected class FactoryPredicate implements equalFunctor {
        
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         * @param name 
         *   {@.ja 実装 ID}
         *   {@.en implementation id}
         */
        public FactoryPredicate(final String name) {
            m_name = name;
        }
        
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         * @param factory 
         *   {@.ja ファクトリオブジェクト}
         *   {@.en Faactory Object}
         */
        public FactoryPredicate(FactoryBase factory) {
            m_name = factory.profile().getProperty("implementation_id");
        }
        
        /**
         * {@.ja Factory検索。}
         * {@.en Finds Factory}
         *
         * <p>
         * {@.ja 指定されたFactoryを検索する}
         * {@.en The specified factory is found.}
         *
         * @param factory 
         *   {@.ja ファクトリオブジェクト}
         *   {@.en Faactory Object}
         *
         * @return 
         *   {@.ja 存在する場合はtrue}
         *   {@.en Returns true when existing.}
         *
         */
        public boolean equalof(java.lang.Object factory) {
            return m_name.equals(((FactoryBase)factory).profile().getProperty("implementation_id"));
        }
        /** 
         * {@.ja 実装 ID}
         * {@.en implementation id}
         */
        public String m_name;
    }
    
    /**
     * {@.ja Component Factory Manager}
     * {@.en Component Factory Manager}
     */
    protected ObjectManager<String, FactoryBase> m_factory = new ObjectManager<String, FactoryBase>();
    
    /**
     * {@.ja ECFactory検索用ヘルパークラス}
     * {@.en Helper class to find ECFactory}
     */
    class ECFactoryPredicate implements equalFunctor {
        
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         *
         * @param name 
         *   {@.ja ExecutionContextクラス名}
         *   {@.en Class name of ExecutionContext}
         */
        public ECFactoryPredicate(final String name) {
            m_name = name;
        }
        
        /**
         * {@.ja コンストラクタ}
         * {@.en Constructor}
         * @param factory 
         *   {@.ja ECファクトリオブジェクト}
         *   {@.en ECFaactory Object}
         *
         */
        public ECFactoryPredicate(ECFactoryBase factory) {
            m_name = factory.name();
        }
        
        /**
         * {@.ja Factory検索。}
         * {@.en Finds Factory}
         *
         * <p>
         * {@.ja 指定されたECFactoryを検索する}
         * {@.en The specified ECFactory is found.}
         *
         * @param factory 
         *   {@.ja ECファクトリオブジェクト}
         *   {@.en ECFaactory Object}
         *
         * @return 
         *   {@.ja 存在する場合はtrue}
         *   {@.en Returns true when existing.}
         *
         */
        public boolean equalof(java.lang.Object factory) {
            return m_name.equals(((ECFactoryBase)factory).name());
        }
        
        /**
         * {@.ja ExecutionContextクラス名}
         * {@.en Class name of ExecutionContext}
         */
        public String m_name;
    }
    
    /**
     * {@.ja ExecutionContext Factory}
     * {@.en ExecutionContext Factory}
     */
    protected ObjectManager<String, java.lang.Object> m_ecfactory = new ObjectManager<String, java.lang.Object>();
    /**
     * {@.ja ExecutionContext}
     * {@.en ExecutionContext}
     */
    protected Vector<ExecutionContextBase> m_ecs = new Vector<ExecutionContextBase>();
    /**
     * {@.ja ORB実行用ヘルパークラス}
     * {@.en ORB exrcution helper class}
     */
    protected class OrbRunner implements Runnable {

      /**
       * {@.ja コンストラクタ}
       * {@.en Constructor}
       * @param orb
       *   {@.ja ORB}
       *   {@.en ORB}
       */
        public OrbRunner(ORB orb) {
            m_pORB = orb;
//            this.open("");
        }

      /**
       * {@.ja ORB 活性化処理}
       * {@.en ORB activation processing}
       *
       * @param args 
       *   {@.ja 活性化時引数}
       *   {@.en ORB activation processing}
       *
       * @return 
       *   {@.ja 活性化結果}
       *   {@.en Activation result}
       */
        public int open(String args) {
            // activate();
            Thread t = new Thread(this);
            t.start();
            return 0;
        }

      /**
       * {@.ja ORB 開始処理}
       * {@.en ORB start processing}
       *
       * @return 
       *   {@.ja 開始処理結果}
       *   {@.en Starting result}
       */
        public int svc() {
            if(m_pORB!=null) {
                m_pORB.run();
            }
//            Manager.instance().shutdown();
            return 0;
        }

      /**
       * {@.ja ORB 終了処理}
       * {@.en ORB close processing}
       *
       * @param flags 
       *   {@.ja 終了処理フラグ}
       *   {@.en Flag of close processing}
       *
       * @return 
       *   {@.ja 終了処理結果}
       *   {@.en Close result}
       */
        public int close(long flags) {
            return 0;
        }

      /**
       * {@.ja スレッド実行}
       * {@.en Thread execution}
       */
        public void run() {
            this.svc();
        }

        private ORB m_pORB;
    }
    /**
     * {@.ja ORB Runner}
     * {@.en ORB Runner}
     */
    protected OrbRunner m_runner;
    
    /**
     * {@.ja 終了処理用ヘルパークラス}
     * {@.en ORB termination helper class.}
     */
    protected class Terminator implements Runnable {

      /**
       * {@.ja コンストラクタ}
       * {@.en Constructor}
       *
       * @param manager 
       *   {@.ja マネージャ・オブジェクト}
       *   {@.en Manager object}
       */
        public Terminator(Manager manager) {
            m_manager = manager;
        }
        
      /**
       * {@.ja 終了処理。}
       * {@.en Termination processing}
       *
       * <p>
       * {@.ja ORB，マネージャ終了処理を開始する。}
       * {@.en Start ORB and manager's termination processing.}
       *
       */
        public void terminate(double tm) {
            try {
                Thread.sleep((int)tm*1000);
            } catch (InterruptedException e) {
               
            }
            this.open("");
        }
        
      /**
       * {@.ja 終了処理活性化処理}
       * {@.en Termination processing activation}
       *
       * @param args 
       *   {@.ja 活性化時引数}
       *   {@.en Activation argument}
       *
       * @return 
       *   {@.ja 活性化結果}
       *   {@.en Activation result}
       */
        public int open(String args) {
//            activate();
            Thread t = new Thread(this);
            t.start();
            return 0;
        }
        
      /**
       * {@.ja ORB，マネージャ終了処理}
       * {@.en ORB and manager's termination processing}
       *
       * @return 
       *   {@.ja 終了処理結果}
       *   {@.en Termination result}
       */
        public int svc() {
            Manager.instance().shutdown();
            return 0;
        }
        
      /**
       * {@.ja スレッド実行}
       * {@.en Thread execution}
       */
        public void run() {
            this.svc();
        }
        
      /**
       * {@.ja マネージャ・オブジェクト}
       * {@.en Manager object}
       */
        public Manager m_manager;
    }

    /**
     * {@.ja Terminator}
     * {@.en Terminator}
     */
    protected Terminator m_terminator;
    /**
     * {@.ja Terminator用カウンタ}
     * {@.en Counter for Terminator}
     * */
    protected int m_terminate_waiting;
    /**
     *
     * {@.ja プロパティ検索用ヘルパークラス}
     * {@.en Helper class to find Properties}
     *
     */
    private class find_conf {
        private Properties m_prop;
        /**
         * {@.ja コンストラクタ}
         * {@.en constructor}
         *
         * @param porp 
         *   {@.ja プロパティ}
         *   {@.en Properties}
         *
         */
        public find_conf(final Properties prop) {
            m_prop = prop;
        }
        /**
         * {@.ja プロパティ検索。}
         * {@.en Finds Properties}
         *
         * <p>
         * {@.ja 指定されたプロパティを検索する}
         * {@.en The specified Properties is found.}
         *
         * @param porp 
         *   {@.ja プロパティ}
         *   {@.en Properties}
         *
         * @return 
         *   {@.ja 存在する場合はtrue}
         *   {@.en Returns true when existing.}
         *
         */
        public boolean equalof(Properties prop) {
            String str = m_prop.getProperty("implementation_id");
            if (!str.equals(prop.getProperty("implementation_id"))) {
                return false;
            }
            str = m_prop.getProperty("vendor");
            if (str != null && !str.equals("") &&
                !str.equals(prop.getProperty("vendor"))){ 
                return false; 
            }
            str = m_prop.getProperty("category");
            if (str != null && !str.equals("") &&
                !str.equals(prop.getProperty("category"))) { 
                return false; 
            }
            str = m_prop.getProperty("language");
            if (str!=null &&  !str.equals("") &&
                !str.equals(prop.getProperty("language"))) {
                return false; 
            }
            str = m_prop.getProperty("version");
            if (str!=null &&  !str.equals("") &&
                !str.equals(prop.getProperty("version"))) {
                return false; 
            }
            return true;
        }
    }
    /**
     * {@.ja LocalService の初期化}
     * {@.en LocalService initialization}
     * @return Timer 
     *   {@.ja 初期化処理実行結果(初期化成功:true、初期化失敗:false)}
     *   {@.en Initialization result (Successful:true, Failed:false)}
     */
    protected boolean initLocalService(){
        rtcout.println(Logbuf.TRACE,"Manager::initLocalService()");

        LocalServiceAdmin admin = LocalServiceAdmin.instance();
        Properties prop = m_config.getNode("manager.local_service");
        admin.init(prop);
        rtcout.println(Logbuf.DEBUG,"LocalServiceAdmin's properties:");
        String str = new String();
        str = prop._dump(str,prop,0);
        rtcout.println(Logbuf.TRACE, str);

        LocalServiceProfile[] svclist = admin.getServiceProfiles();
        for (int ic=0; ic < svclist.length; ++ic) {
            rtcout.println(Logbuf.INFO,"Available local service: "+svclist[ic].name+" "+svclist[ic].uuid);
          }
        return true;
    }


    /**
     * {@.ja corba.endpoints にエンドポイント情報を設定する}
     * {@.en Setting endpoint info from corba.endpoints}
     */
    protected void setEndpointProperty(Object objref) {
        rtcout.println(Logbuf.TRACE,"setEndpointProperty()");
        if(objref==null){
            rtcout.println(Logbuf.WARN,"Object reference is nil.");
            return;
        }

        
        DataRef<Boolean> ipv4 = new DataRef<Boolean>(Boolean.FALSE);
        DataRef<Boolean> ipv6 = new DataRef<Boolean>(Boolean.FALSE);
        
        ArrayList<Integer> ipv4_list = new ArrayList<Integer>();
        ArrayList<Integer> ipv6_list = new ArrayList<Integer>();

        endpointPropertySwitch("ipv4", ipv4, ipv4_list);
        endpointPropertySwitch("ipv6", ipv6, ipv6_list);

        String iorstr = m_pORB.object_to_string(objref);
        IOR ior = new IOR();
        DataRef<IOR> iorholder = new DataRef<IOR>(ior);
        toIOR(iorstr, iorholder);
        ArrayList<Address> endpoints = getEndpoints(iorholder.v); 

        ArrayList<String> epstr = new ArrayList<String>();
        ArrayList<String> epstr_ipv4 = new ArrayList<String>();
        ArrayList<String> epstr_ipv6 = new ArrayList<String>();
        int  ipv4_count=0;
        int  ipv6_count=0;
        for (int ic=0; ic < endpoints.size(); ++ic) {
            String addr = endpoints.get(ic).host;
            if (ipv4.v.booleanValue() && StringUtil.isIPv4(addr)) {
                String tmp_port = String.valueOf(endpoints.get(ic).port);
                String tmp = addr + ":" + tmp_port;
                if (ipv4_list.size() == 0 || ipv4_list.contains(ipv4_count)){
                    epstr.add(tmp);
                    epstr_ipv4.add(tmp);
                }
                ipv4_count += 1;
            }
            if (ipv6.v.booleanValue() && StringUtil.isIPv6(addr)) {
                String tmp_port = String.valueOf(endpoints.get(ic).port);
                String tmp = "[" + addr + "]:" + tmp_port;
                if (ipv6_list.size() == 0 ||ipv6_list.contains(ipv6_count)){
                    epstr.add(tmp);
                    epstr_ipv6.add(tmp);
                }
                ipv6_count += 1;
            }
        }
        String str = new String();
        str = StringUtil.flatten(epstr);
        m_config.setProperty("corba.endpoints", str);
        str = StringUtil.flatten(epstr_ipv4);
        m_config.setProperty("corba.endpoints_ipv4", str);
        str = StringUtil.flatten(epstr_ipv6);
        m_config.setProperty("corba.endpoints_ipv6", str);
   }
    /**
     * {@.ja corba.endpoint_property からオプション情報を取得する}
     * {@.en Getting option info from corba.endpoint_property}
     */
    protected void endpointPropertySwitch(final String ipver,
                           DataRef<Boolean> ip, ArrayList<Integer> ip_list) {
        ip.v = Boolean.FALSE; 
        ip_list.clear();

        String ep_prop = m_config.getProperty("corba.endpoint_property", "ipv4");
        ep_prop = ep_prop.toLowerCase();

        int pos = ep_prop.indexOf(ipver);
        if (pos < 0) { 
            return; 
        }

        ip.v = Boolean.TRUE;
        pos += ipver.length();
 
        if (pos >= ep_prop.length() || ep_prop.charAt(pos) != '(') { 
            return; 
        }
  
        int par_begin, par_end;
        par_begin = pos;
        ++pos;
        par_begin = pos;
        int num = ep_prop.indexOf(")",pos);
        if(num < 0){
            num = ep_prop.length()-1;
        }
        par_end = num;

        String list_num = ep_prop.substring(par_begin, par_end);
        String[] nums = list_num.split(",");
        for (int ic=0; ic < nums.length; ++ic) {
            try {
                int ival = Integer.parseInt(nums[ic],10);
                ip_list.add(ival);
            }
            catch(Exception ex){

            }
        }
    }

    /**
     * {@.ja }
     * {@.en }
     * 
     * <p>    
     * {@.ja }
     * {@.en }
     *
     */
    public boolean toIOR(final String iorstr, DataRef<IOR> iorholder) {
        if (iorstr.isEmpty()) { 
            return false; 
        }
        int size = iorstr.length();

        if (size < 4) {
            return false;
        }

        String ior = iorstr;
        if(!ior.startsWith("IOR:")) {
            return false;
        }
        

        // IOR:xxyyzz......
        // "IOR:" occupies 4 digits.
        // two digits express one byte, and all byte sequence express IOR profile
        ior = ior.substring(4);

        ByteArrayOutputStream bastream = new ByteArrayOutputStream();
        org.omg.CORBA.Any any = ORBUtil.getOrb().create_any(); 
        OutputStream buf = any.create_output_stream();

        int step = 0;
        for (int ic=0; ic < ior.length(); ic += 2) {
            String temp  = ior.substring(ic,ic+2);
            try {
                int ival = Integer.parseInt(temp,16);
                bastream.write(ival);
            }
            catch(Exception ex){
                return false;
            }
        }
        byte value[] = bastream.toByteArray();
        buf.write_long (value.length);
        buf.write_octet_array (value, 0, value.length);
        InputStream inp = buf.create_input_stream();
        boolean swap = inp.read_boolean();  //Byte Order (0) Big Endian
        byte[] padding = new byte[4];
        inp.read_octet_array(padding,0,3);  //padding
        int typeIdLength;
        typeIdLength = inp.read_long();     //TypeId length 
        iorholder.v = IORHelper.read(inp); 

        return true;
    }
    /**
     * {@.ja }
     * {@.en }
     * 
     * <p>    
     * {@.ja }
     * {@.en }
     *
     */
    public ArrayList<Address> getEndpoints(IOR ior) {
        ArrayList<Address> addr = new ArrayList<Address>();
        if (ior.profiles.length == 0 && ior.type_id.length() == 0) {
            System.err.println("IOR is a nil object reference.");
            return addr;
        }

        for (int ic=0; ic < ior.profiles.length; ++ic) {
            if (ior.profiles[ic].tag == TAG_INTERNET_IOP.value) {
                org.omg.CORBA.Any any = ORBUtil.getOrb().create_any(); 
                OutputStream buf = any.create_output_stream();
                TaggedProfileHelper.write(buf,ior.profiles[ic]);
                InputStream inp = buf.create_input_stream();
                //Profile Tag 
                int tag = inp.read_long();    
                //Profile length
                int profileLength = inp.read_long(); 
                //Byte Order
                byte byteOrder = inp.read_octet();
                //Version
                byte[] version = new byte[2];
                inp.read_octet_array(version,0,2);
                //Host string
                String host = inp.read_string();
                //Port
                int port = inp.read_short() & 0x0000ffff;
                addr.add(new Address(host,port));
                //extractAddrs(pBody.components, addr);
            }
            else if (ior.profiles[ic].tag == TAG_MULTIPLE_COMPONENTS.value) {
                org.omg.CORBA.Any any = ORBUtil.getOrb().create_any(); 
                OutputStream buf = any.create_output_stream();
                TaggedProfileHelper.write(buf,ior.profiles[ic]);
                InputStream inp = buf.create_input_stream();
                int tag = inp.read_long();    
                //Profile length
                int profileLength = inp.read_long(); 
                //Byte Order
                byte byteOrder = inp.read_octet();
                //Number of tagged components
                int componentsLength = inp.read_long(); 
                for(int icc=0;icc<componentsLength;++icc){
                    int comptag  = inp.read_long();
                    int compLength  = inp.read_long();
                    byte cByteOrder = inp.read_octet();
                    int vender  = inp.read_long();
                }
//                extractAddrs(pBody.components, addr);
            }
            else {
                System.err.printf("Unrecognised profile tag: 0x%x",ior.profiles[ic].tag);
            }
        }
        return addr;
    }


    class Address {
        public Address(String host, int port ){
            this.host = host;
            this. port= port;
        }
        public String host;
        public int port;
    }


    /**
     * {@.ja コンポーネント削除用クラス}
     * {@.en Class}
     * 
     */
    protected class Finalized {
        String  mutex = new String();
        Vector<RTObject_impl> comps = new Vector<RTObject_impl>();
    };
    /**
     * {@.ja コンポーネント削除用リスト}
     * {@.en List for component deletion}
     * 
     */
    Finalized m_finalized = new Finalized();

    ManagerActionListeners m_listeners = new ManagerActionListeners();

    //private static final String SERVER_HOST = "com.sun.CORBA.ORBServerHost";
    //private static final String SERVER_PORT = "com.sun.CORBA.ORBServerPort";
    //private static final String LISTENER_PORT = "com.sun.CORBA.POA.ORBPersistentServerPort";
    //private static final String SERVER_HOST = "OAIAddr";
    //private static final String SERVER_PORT = "OAPort";
    //private static final String LISTENER_PORT = "OAPort";
}
