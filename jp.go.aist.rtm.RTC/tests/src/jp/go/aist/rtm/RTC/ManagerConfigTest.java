package jp.go.aist.rtm.RTC;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jp.go.aist.rtm.RTC.util.Properties;
import junit.framework.TestCase;

/**
* マネージャ　コンフィギュレーションクラス　テスト(1)
* 対象クラス：ManagerConfig
*/
public class ManagerConfigTest extends TestCase {

    protected ManagerConfig m_managerConfig;
    
    private class ManagerConfigMock extends ManagerConfig {

        public boolean fileExist(final String filename) {
          return super.fileExist(filename);
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        
        this.m_managerConfig = new ManagerConfig();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * <p>init()メソッドとconfigure()メソッドのテスト
     * <ul>
     * <li>コマンド引数の-fオプションで指定したファイルで正しく初期化できるか？</li>
     * <li>設定されている内容を正しく取得できるか？</li>
     * <li>システム情報のプロパティが、取得内容に含まれているか？</li>
     * </ul>
     * </p>
     */
    public void test_init_and_configure() throws Exception {
        ManagerConfigMock mgrCfg = new ManagerConfigMock();
        assertTrue(mgrCfg.fileExist("tests/fixtures/ManagerConfig/test.conf"));

        // コマンド引数の-fオプションで指定したファイルで正しく初期化できるか？
        String[] args = { "-f", "tests/fixtures/ManagerConfig/test.conf" };
        mgrCfg.init(args);
        
        // 設定されている内容を正しく取得できるか？
        Properties properties = new Properties();
        mgrCfg.configure(properties);

        assertEquals("RTC.COMPONENT.CONF.PATH", properties.getProperty("rtc.component.conf.path"));
        assertEquals("RTC.MANAGER.ARCH", properties.getProperty("rtc.manager.arch"));
        assertEquals("RTC.MANAGER.DEBUG.LEVEL", properties.getProperty("rtc.manager.debug.level"));
        assertEquals("RTC.MANAGER.LANGUAGE", properties.getProperty("rtc.manager.language"));
        assertEquals("RTC.MANAGER.NAMESERVER", properties.getProperty("rtc.manager.nameserver"));
        assertEquals("RTC.MANAGER.OPENING_MESSAGE", properties.getProperty("rtc.manager.opening_message"));
        assertEquals("RTC.MANAGER.ORB", properties.getProperty("rtc.manager.orb"));
        assertEquals("RTC.MANAGER.ORB.OPTIONS", properties.getProperty("rtc.manager.orb.options"));
        assertEquals("RTC.MANAGER.OS", properties.getProperty("rtc.manager.os"));
        assertEquals("RTC.MANAGER.OS.RELEASE", properties.getProperty("rtc.manager.os.release"));
        assertEquals("RTC.MANAGER.SUBSYSTEMS", properties.getProperty("rtc.manager.subsystems"));
        assertEquals("RTC.OPENRTM.AUTHOR", properties.getProperty("rtc.openrtm.author"));
        assertEquals("RTC.OPENRTM.RELEASE", properties.getProperty("rtc.openrtm.release"));
        assertEquals("RTC.OPENRTM.VENDOR", properties.getProperty("rtc.openrtm.vendor"));
        assertEquals("RTC.OPENRTM.VERSION", properties.getProperty("rtc.openrtm.version"));
        
        // システム情報のプロパティが、取得内容に含まれているか？
        // （システム情報は動作環境に依存するので、プロパティが取得できていることだけを確認する）
        assertFalse( properties.getProperty("os.name").equals(""));
        assertFalse( properties.getProperty("os.release").equals(""));
        assertFalse( properties.getProperty("os.version").equals(""));
        assertFalse( properties.getProperty("os.arch").equals(""));
        assertFalse( properties.getProperty("os.hostname").equals(""));
        assertFalse( properties.getProperty("manager.pid").equals(""));

        String value = properties.getProperty("manager.supported_languages");
        String[] langs = value.split(",");
        for(int ic=0;ic<langs.length;++ic) {
            langs[ic] = langs[ic].trim();
        }
        java.util.Properties sysInfo = System.getProperties();
        String osName = sysInfo.getProperty("os.name").toLowerCase();
        if(osName.startsWith("windows")){
            assertEquals("C++", langs[0]);
            assertEquals("Python", langs[1]);
            assertEquals("Java", langs[2]);
        }
        else if(osName.startsWith("linux")){
            assertEquals("C++", langs[0]);
            assertEquals("Python", langs[1]);
            assertEquals("Python3", langs[2]);
            assertEquals("Java", langs[3]);
        }
        else{
            assertEquals("C++", langs[0]);
            assertEquals("Python", langs[1]);
            assertEquals("Python3", langs[2]);
            assertEquals("Java", langs[3]);
        }
    }

    /**
     * <p>init()メソッドのテスト
     * <ul>
     * <li>コンフィグファイルの指定オプションなしで初期化した場合、デフォルトのコンフィグレーションファイルの内容で初期化されるか？</li>
     * </ul>
     * </p>
     */
    public void test_init_default() throws Exception {
        ManagerConfigMock mgrCfg = new ManagerConfigMock();
        
        // オプション指定なしで初期化する
        mgrCfg.init(null);
        
        // 設定されている内容を正しく取得できるか？
        Properties properties = new Properties();
        mgrCfg.configure(properties);
        assertEquals("", properties.getProperty("module.conf_path"));
        assertEquals("INFO", properties.getProperty("logger.log_level"));
        assertEquals("Java", properties.getProperty("manager.language"));
        assertEquals("default", properties.getProperty("manager.nameservers"));
        assertEquals("", properties.getProperty("corba.args"));
        assertEquals("orbd", properties.getProperty("corba.id"));
        
        // システム情報のプロパティが、取得内容に含まれているか？
        // （システム情報は動作環境に依存するので、プロパティが取得できていることだけを確認する）
        assertFalse( properties.getProperty("os.name").equals(""));
        assertFalse( properties.getProperty("os.release").equals(""));
        assertFalse( properties.getProperty("os.version").equals(""));
        assertFalse( properties.getProperty("os.arch").equals(""));
        assertFalse( properties.getProperty("os.hostname").equals(""));
        assertFalse( properties.getProperty("manager.pid").equals(""));
    }
    
    public void test_init_f() throws Exception {
        
        String[] args = { "-f", "tests/fixtures/ManagerConfig/rtc.conf" };
        this.m_managerConfig.init(args);
    }

    /**
     *<pre>
     * プロパティの読み込みチェック
     *　・プロパティとして設定した情報をコンフィギュレーション情報に設定できるか？
     *</pre>
     */
    public void test_configure() throws Exception {
        
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("rtc.component.conf.path", "C:\\\\Program\\\\ Files\\\\OpenRTM-aist");
        expected.put("rtc.manager.arch", "i386");
        expected.put("rtc.manager.debug.level", "PARANOID");
        expected.put("rtc.manager.language", "C++");
        expected.put("rtc.manager.nameserver", "zonu.a02.aist.go.jp");
        expected.put("rtc.manager.opening_message", "\"Hello \" World\"");
        expected.put("rtc.manager.orb", "omniORB");
        expected.put("rtc.manager.orb.options", "IIOPAddrPort, -ORBendPoint, giop:tcp:");
        expected.put("rtc.manager.os", "FreeBSD");
        expected.put("rtc.manager.os.release", "6.1-RELEASE");
        expected.put("rtc.manager.subsystems", "Camera, Manipulator, Force Sensor");
        expected.put("rtc.openrtm.author", "Noriaki Ando");
        expected.put("rtc.openrtm.release", "aist");
        expected.put("rtc.openrtm.vendor", "AIST");
        expected.put("rtc.openrtm.version", "1.0.0");
        
        String[] args = { "-f", "tests/fixtures/ManagerConfig/rtc.conf" };
        this.m_managerConfig.init(args);
        
        Properties prop = new Properties();
        this.m_managerConfig.configure(prop);
        
        for (Iterator<Map.Entry<String, String>> it = expected.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            String expectedValue = entry.getValue();
            String resultValue = prop.getProperty(entry.getKey());
            assertEquals(expectedValue, resultValue);
        }
    }
}
