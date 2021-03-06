package jp.go.aist.rtm.RTC.port;

import _SDOPackage.NVListHolder;
//import jp.go.aist.rtm.RTC.port.InPortProviderImpl;
import jp.go.aist.rtm.RTC.util.NVListHolderFactory;
import jp.go.aist.rtm.RTC.util.NVUtil;
import junit.framework.TestCase;

/**
 * <p>InPortProviderImplクラスのためのテストケースです。</p>
 */
public class InPortProviderImplTest extends TestCase {

    //private class InPortProviderImplMock extends InPortProviderImpl {
    private class InPortProviderImplMock extends InPortCorbaCdrProvider {
        
        public InPortProviderImplMock(
                String dataType, String interfaceType,
                String dataFlowType, String subscriptionType) {
            super();
            
            setDataType(dataType);
            setInterfaceType(interfaceType);
            setDataFlowType(dataFlowType);
            setSubscriptionType(subscriptionType);
            
            NVUtil.appendStringValue(m_properties, "PROPERTY_NAME1", "PROPERTY_VALUE1");
            NVUtil.appendStringValue(m_properties, "PROPERTY_NAME2", "PROPERTY_VALUE2");
        }
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * <p>publishInterfaceProfile()メソッドをテストします。</p>
     * <p>取得した情報が期待値と一致するかどうかを確認します。確認する情報は次の4つです。
     * <ul>
     * <li>dataport.data_type</li>
     * <li>dataport.interface_type</li>
     * <li>dataport.dataflow_type</li>
     * <li>dataport.subscription_type</li>
     * </ul>
     * </p>
     */
    public void test_publishInterfaceProfile() throws Exception {

        InPortProvider provider = new InPortProviderImplMock(
                "DATA_TYPE", "INTERFACE_TYPE",
                "DATA_FLOW_TYPE", "SUBSCRIPTION_TYPE");
        
        NVListHolder prop = NVListHolderFactory.create();
        provider.publishInterfaceProfile(prop);

        assertEquals("DATA_TYPE",
                NVUtil.find(prop, "dataport.data_type").extract_wstring());
        assertEquals("INTERFACE_TYPE",
                NVUtil.find(prop, "dataport.interface_type").extract_wstring());
        assertEquals("DATA_FLOW_TYPE",
                NVUtil.find(prop, "dataport.dataflow_type").extract_wstring());
        assertEquals("SUBSCRIPTION_TYPE",
                NVUtil.find(prop, "dataport.subscription_type").extract_wstring());
    }

    /**
     * <p>publishInterface()メソッドのテストです。</p>
     * <p>次の点をテストします。
     * <ol>
     * <li>引数で渡したNameValueオブジェクトのインタフェースタイプが、ポートのそれと一致しない場合：<br />
     * Interface情報が取得されないことを確認する。</li>
     * <li>引数で渡したNameValueオブジェクトのインタフェースタイプが、ポートのそれと一致する場合：<br />
     * Interface情報を取得でき、それが期待値と一致することを確認する。</li>
     * </ol>
     * </p>
     */
    public void testPublishInterface() {

        InPortProvider provider = new InPortProviderImplMock(
                "DATA_TYPE", "INTERFACE_TYPE",
                "DATA_FLOW_TYPE", "SUBSCRIPTION_TYPE");
        
        NVListHolder prop = NVListHolderFactory.create();
        provider.publishInterfaceProfile(prop);

        // (1) インタフェースタイプ不一致の場合：
        NVListHolder prop_dummy = NVListHolderFactory.clone(prop);
        for (int i = 0; i < prop_dummy.value.length; i++) {
            if (prop_dummy.value[i].name.equals("dataport.interface_type")) {
                // インタフェースタイプが一致しないように、書き換える
                prop_dummy.value[i].value.insert_string("DUMMY");
            }
        }
        
        provider.publishInterface(prop_dummy);
        
        // インタフェース情報が取得されないことを確認する
        assertEquals(-1, NVUtil.find_index(prop_dummy, "PROPERTY_NAME1"));
        assertEquals(-1, NVUtil.find_index(prop_dummy, "PROPERTY_NAME2"));

        
        // (2) インタフェースタイプ一致の場合：
        provider.publishInterface(prop);
        
        // インタフェース情報が取得されることを確認する
        assertFalse(-1 == NVUtil.find_index(prop, "PROPERTY_NAME1"));
        assertFalse(-1 == NVUtil.find_index(prop, "PROPERTY_NAME2"));
    }
    
}
