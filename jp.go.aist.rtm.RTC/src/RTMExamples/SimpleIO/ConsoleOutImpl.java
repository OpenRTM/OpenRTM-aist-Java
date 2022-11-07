package RTMExamples.SimpleIO;

import java.util.Observer;
import java.util.Observable;

import RTC.ReturnCode_t;
import RTC.TimedLong;
import RTC.Time;
import jp.go.aist.rtm.RTC.DataFlowComponentBase;
import jp.go.aist.rtm.RTC.Manager;
import jp.go.aist.rtm.RTC.connectorListener.ReturnCode;
import jp.go.aist.rtm.RTC.port.InPort;
import jp.go.aist.rtm.RTC.port.ConnectorBase;
import jp.go.aist.rtm.RTC.port.ConnectorDataListener;
import jp.go.aist.rtm.RTC.port.ConnectorDataListenerT;
import jp.go.aist.rtm.RTC.port.ConnectorDataListenerType;
import jp.go.aist.rtm.RTC.port.ConnectorListenerType;
import jp.go.aist.rtm.RTC.util.DataRef;

import org.omg.CORBA.portable.OutputStream;

public class ConsoleOutImpl  extends DataFlowComponentBase {

    public ConsoleOutImpl(Manager manager) {
        super(manager);
        // <rtc-template block="initializer">
        m_in_val = new TimedLong();
        m_in_val.tm = new Time();
        m_in = new DataRef<TimedLong>(m_in_val);
        m_inIn = new InPort<TimedLong>("in", m_in);
        // </rtc-template>

        // Registration: InPort/OutPort/Service
        // <rtc-template block="registration">
/*
        // Set InPort buffers
        try {
//            registerInPort(TimedLong.class, "in", m_inIn);  //v042
            registerInPort("in", m_inIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
*/
        
        // Set OutPort buffer
        
        // Set service provider to Ports
        
        // Set service consumers to Ports
        
        // Set CORBA Service Ports
        
        // </rtc-template>
    }

    // The initialize action (on CREATED->ALIVE transition)
    // formaer rtc_init_entry() 
//    @Override
    protected ReturnCode_t onInitialize() {
        try {
            addInPort("in", m_inIn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_BUFFER_WRITE,
                            new DataListener("ON_BUFFER_WRITE"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_BUFFER_FULL, 
                            new DataListener("ON_BUFFER_FULL"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_BUFFER_WRITE_TIMEOUT, 
                            new DataListener("ON_BUFFER_WRITE_TIMEOUT"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_BUFFER_OVERWRITE, 
                            new DataListener("ON_BUFFER_OVERWRITE"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_BUFFER_READ, 
                            new DataListener("ON_BUFFER_READ"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_SEND, 
                            new DataListener("ON_SEND"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_RECEIVED,
                            new DataListener("ON_RECEIVED"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_RECEIVER_FULL, 
                            new DataListener("ON_RECEIVER_FULL"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_RECEIVER_TIMEOUT, 
                            new DataListener("ON_RECEIVER_TIMEOUT"));
        m_inIn.addConnectorDataListener(
                            ConnectorDataListenerType.ON_RECEIVER_ERROR,
                            new DataListener("ON_RECEIVER_ERROR"));

        return super.onInitialize();
    }
    // The finalize action (on ALIVE->END transition)
    // formaer rtc_exiting_entry()
//    @Override
//    protected ReturnCode_t onFinalize() {
//        return super.onFinalize();
//    }
    //
    // The startup action when ExecutionContext startup
    // former rtc_starting_entry()
//    @Override
//    protected ReturnCode_t onStartup(int ec_id) {
//        return super.onStartup(ec_id);
//    }
    //
    // The shutdown action when ExecutionContext stop
    // former rtc_stopping_entry()
//    @Override
//    protected ReturnCode_t onShutdown(int ec_id) {
//        return super.onShutdown(ec_id);
//    }
    //
    // The activated action (Active state entry action)
    // former rtc_active_entry()
//    @Override
//    protected ReturnCode_t onActivated(int ec_id) {
//        return super.onActivated(ec_id);
//    }
    //
    // The deactivated action (Active state exit action)
    // former rtc_active_exit()
//    @Override
//    protected ReturnCode_t onDeactivated(int ec_id) {
//        return super.onDeactivated(ec_id);
//    }
    //
    // The execution action that is invoked periodically
    // former rtc_active_do()
    @Override
    protected ReturnCode_t onExecute(int ec_id) {
        if( m_inIn.isNew() ) {
            m_inIn.read();
//        if( m_inIn.read() ) {
            System.out.print( "Received: " + m_in.v.data + " " );
            System.out.print( "TimeStamp: " + m_in.v.tm.sec + "[s] " );
            System.out.println( m_in.v.tm.nsec + "[ns]" );
          }
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return super.onExecute(ec_id);
    }
    //
    // The aborting action when main logic error occurred.
    // former rtc_aborting_entry()
//  @Override
//  public ReturnCode_t onAborting(int ec_id) {
//      return super.onAborting(ec_id);
//  }
    //
    // The error action in ERROR state
    // former rtc_error_do()
//    @Override
//    public ReturnCode_t onError(int ec_id) {
//        return super.onError(ec_id);
//    }
    //
    // The reset action that is invoked resetting
    // This is same but different the former rtc_init_entry()
//    @Override
//    protected ReturnCode_t onReset(int ec_id) {
//        return super.onReset(ec_id);
//    }
//  
    // The state update action that is invoked after onExecute() action
    // no corresponding operation exists in OpenRTm-aist-0.2.0
//    @Override
//    protected ReturnCode_t onStateUpdate(int ec_id) {
//        return super.onStateUpdate(ec_id);
//    }
    //
    // The action that is invoked when execution context's rate is changed
    // no corresponding operation exists in OpenRTm-aist-0.2.0
//    @Override
//    protected ReturnCode_t onRateChanged(int ec_id) {
//        return super.onRateChanged(ec_id);
//    }
//
    // DataInPort declaration
    // <rtc-template block="inport_declare">
    protected TimedLong m_in_val;
    protected DataRef<TimedLong> m_in;
    protected InPort<TimedLong> m_inIn;
    
    // </rtc-template>

    // DataOutPort declaration
    // <rtc-template block="outport_declare">
    
    // </rtc-template>

    // CORBA Port declaration
    // <rtc-template block="corbaport_declare">
    
    // </rtc-template>

    // Service declaration
    // <rtc-template block="service_declare">
    
    // </rtc-template>

    // Consumer declaration
    // <rtc-template block="consumer_declare">
    
    // </rtc-template>

    class DataListener extends ConnectorDataListenerT<TimedLong>{
        public DataListener(final String name){
            super(TimedLong.class);
            m_name = name;
        }

        public ReturnCode operator(ConnectorBase.ConnectorInfo arg,
                               TimedLong data) {
            ConnectorBase.ConnectorInfo info =(ConnectorBase.ConnectorInfo)arg;
            System.out.println("------------------------------");
            System.out.println("Listener:       "+m_name);
            System.out.println("Profile::name:  "+info.name);
            System.out.println("Profile::id:    "+info.id);
//            System.out.println("Profile::properties: ");
//            System.out.println(info.properties);
            System.out.println("Data:           "+data.data);
            System.out.println("------------------------------");
            return ReturnCode.NO_CHANGE;
        }
        public String m_name;
    }


}
