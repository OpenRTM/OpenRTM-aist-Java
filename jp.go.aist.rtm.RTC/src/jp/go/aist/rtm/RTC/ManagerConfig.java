package jp.go.aist.rtm.RTC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import jp.go.aist.rtm.Constants;
import jp.go.aist.rtm.RTC.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.HelpFormatter;

/**
 * {@.ja Managerのコンフィグレーションを表現するクラスです。}
 * {@.en Modify Manager's configuration.}
 *
 * <p>
 * {@.ja コマンドライン引数や環境変数、設定ファイルを読み込み・解析して
 * コンフィグレーション情報を生成します。
 * 各設定の優先度は次の通りです。
 * <ul>
 * <li> UNIX/Linux 
 * <ol>
 * <li>コマンドラインオプション -f</li>
 * <li>環境変数 RTC_MANAGER_CONFIG</li>
 * <li>デフォルト設定ファイル ./rtc.conf</li>
 * <li>デフォルト設定ファイル /etc/rtc.conf</li>
 * <li>デフォルト設定ファイル /etc/rtc/rtc.conf</li>
 * <li>デフォルト設定ファイル /usr/local/etc/rtc.conf</li>
 * <li>デフォルト設定ファイル /usr/local/etc/rtc/rtc.conf</li>
 * <li>埋め込みコンフィギュレーション値</li>
 * </ol>
 * <li> Windows
 * <ol>
 * <li>コマンドラインオプション -f</li>
 * <li>環境変数 RTC_MANAGER_CONFIG</li>
 * <li>デフォルト設定ファイル ./rtc.conf</li>
 * <li>デフォルト設定ファイル %RTM_ROOT%/%RTM_VC_VERSION%/rtc.conf</li>
 * </ol></ul>}
 *
 * {@.en This class receives the command line arguments and will be 
 * instantiated.
 * Set property information of Manager with the configuration file specified
 * by the command line argument or the environment variable etc.
 *
 * The priorities of each configuration are as follows:
 * <ul>
 * <li> UNIX/Linux 
 * <OL>
 * <LI>Command option "-f"
 * <LI>Environment variable "RTC_MANAGER_CONFIG"
 * <LI>Default configuration file "./rtc.conf"
 * <LI>Default configuration file "/etc/rtc.conf"
 * <LI>Default configuration file "/etc/rtc/rtc.conf"
 * <LI>Default configuration file "/usr/local/etc/rtc.conf"
 * <LI>Default configuration file "/usr/local/etc/rtc/rtc.conf"
 * <LI>Embedded configuration value
 * </OL>
 * <li> Windows
 * <OL>
 * <LI>Command option "-f"
 * <LI>Environment variable "RTC_MANAGER_CONFIG"
 * <LI>Default configuration file "./rtc.conf"
 * <LI>Default configuration file "%RTM_ROOT%/%RTM_VC_VERSION%/rtc.conf"
 * </OL></ul>}
 *
 */
class ManagerConfig {

    /**
     * {@.ja Managerのデフォルト・コンフィグレーションのファイル・パス}
     * {@.en The default configuration file path for manager}
     */
/*
    public static final String[] CONFIG_FILE_PATH = {
        "./rtc.conf",
        "/etc/rtc.conf",
        "/etc/rtc/rtc.conf",
        "/usr/local/etc/rtc.conf",
        "/usr/local/etc/rtc/rtc.conf",
        null
    };
*/    
    /**
     * {@.ja デフォルト・コンフィグレーションのファイル・パスを識別する
     * 環境変数です。}
     * {@.en The environment variable to distinguish the default configuration
     * file path}
     */
    public static final String CONFIG_FILE_ENV = "RTC_MANAGER_CONFIG";

    /**
     * {@.ja コンストラクタ。}
     * {@.en Constructor}
     */
    public ManagerConfig() {
        m_isMaster = false;
    }

    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     *
     * <p>
     * {@.ja 与えられた引数によりコンフィギュレーション情報の初期化を行う。}
     * {@.en Initialize configuration information by given arguments.}
     *
     * @param args 
     *   {@.ja コマンドライン引数}
     *   {@.en The command line arguments}
     */
    public ManagerConfig(String[] args) throws Exception {
        m_isMaster = false;
        init(args);
    }

    /**
     * {@.ja 初期化を行います。}
     * {@.en Initialization}
     * 
     * <p>
     * {@.ja ココマンドライン引数を受け取り、
     * コンフィグレーション情報を構成します。
     * マンドラインオプションには、以下のものを使用できます。
     * <ul>
     * <li> -a              : マネージャサービスOFF </li>
     * <li> -f &lt;file name&gt;  : 設定ファイルの指定 </li>
     * <li> -o &lt;options&gt;    : オプション指定 </li>
     * <li> -p &lt;port number&gt;: ポート番号指定 </li>
     * <li> -d              : マスターマネージャ指定 </li>
     * </ul>}
     *
     * {@.en Initialize with command line options. The following command options
     * are available.
     * <ul>
     * <li> -a              : Disable manager service</li>
     * <li> -f &lt;file name&gt;  : Specify a configuration file</li>
     * <li> -o &lt;option&gt;     : Specify options</li>
     * <li> -p &lt;port number&gt;: Specify a port number</li>
     * <li> -d              : Run as the master manager</li>
     * </ul>}
     *
     * @param args 
     *   {@.ja コマンドライン引数}
     *   {@.en The command line arguments}
     * 
     */
    public void init(String[] args) throws Exception {
        parseArgs(args);
    }
    
    /**
     * {@.ja Configuration 情報を Property に設定する}
     * {@.en Specify the configuration information to the Property}
     * 
     * <p>
     * {@.ja Manager のConfiguration 情報を指定された Property に設定する。}
     * {@.en Configure to the properties specified by Manager's configuration}
     * </p>
     *
     * @param properties 
     *   {@.ja コンフィグレーション情報を受け取って格納する
     *          Propertiesオブジェクト}
     *   {@.en The target properties to configure}
     * 
     * @throws IOException 
     *   {@.ja コンフィグレーションファイル読み取りエラーの場合にスローされる}
     */
    public void configure(Properties properties) 
                                throws FileNotFoundException, IOException {
        
        properties.setDefaults(DefaultConfiguration.default_config);
        
        if (findConfigFile()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(this.m_configFile));
                properties.load(reader);
                
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        
        setSystemInformation(properties);
        if (m_isMaster) { 
            properties.setProperty("manager.is_master","YES"); 
        }

        // Properties from arguments are marged finally
        properties.merge(m_argprop);
        properties.setProperty("config_file", m_configFile);
    }
    
    /**
     * {@.ja コマンド引数をパースする。}
     * {@.en Parse the command arguments}
     *
     * <p>
     * {@.ja <ul>
     * <li>-a               : マネージャサービスOFF </li>
     * <li>-f &lt;file name&gt;  : 設定ファイルの指定 </li>
     * <li>-o &lt;options&gt;    : オプション指定 </li>
     * <li>-p &lt;port number&gt;: ポート番号指定 </li>
     * <li>-d               : マスターマネージャ指定 </li>
     * </ul>}
     * {@.en <ul> 
     * <li> -a              : Disable manager service</li>
     * <li> -f &lt;file name&gt;  : Specify a configuration file</li>
     * <li> -o &lt;option&gt;     : Specify options</li>
     * <li> -p &lt;port number&gt;: Specify a port number</li>
     * <li> -d              : Run as the master manager</li>
     * </ul>}
     * </p>
     *
     * @param args 
     *   {@.ja コマンドライン引数}
     *   {@.en The command line arguments}
     *
     * @throws IllegalArgumentException 
     *   {@.ja コマンドライン引数を解析できなかった場合にスローされる。}
     *
     */
    protected void parseArgs(String[] args) throws IllegalArgumentException {
        
        Options options = new Options();
        options.addOption("a", false, "Disable manager service");
        options.addOption("f", true, "Specify a configuration file");
        //options.addOption("l", true, "load module");
        //options.addOption("o", true, "other options");
        options.addOption("p", true, "Specify a port number");
        options.addOption("d", false, "Run as the master manager");
        options.addOption("i", false, "Don't Shutdown if a configuration file doesn't exist");
        Option opt_oter = OptionBuilder.
                                withLongOpt("other"). 
                                withDescription("other options").
                                hasArgs(60).
                                isRequired(false).
                                withArgName("other").
                                create('o');
        options.addOption(opt_oter);


	CommandLine commandLine = null;
        try {
            CommandLineParser parser = new BasicParser();
            commandLine = parser.parse(options, args);
            
        } catch (ParseException e) {
	    HelpFormatter help = new HelpFormatter();
	    help.printHelp("UpdateKeyword", options, true);
            throw new IllegalArgumentException("Could not parse arguments.");
        }

        String[] searchArgs = commandLine.getOptionValues("o");
        if(searchArgs!=null){
            for(int ic=0;ic<searchArgs.length;++ic) {
                String optarg = searchArgs[ic].trim();
                int pos = optarg.indexOf(":");
                if (pos >= 0) {
                    m_argprop.setProperty(optarg.substring(0, pos), 
                                                    optarg.substring(pos + 1));
                }
            }
        }
        if (commandLine.hasOption("a")) {
            m_argprop.setProperty("manager.corba_servant","NO");;
        }
        if (commandLine.hasOption("f")) {
            //this.m_configFile = commandLine.getOptionValue("f").trim();
            String str = commandLine.getOptionValue("f");
            if(str != null){
                if(!fileExist(str)){
                    System.err.println("Configuration file: "
                                       + str 
                                       + " not found.");
                    if (!commandLine.hasOption("i")) {
                        System.exit(-1);
                    }
                    
                }
                this.m_configFile = str.trim();
            }
        }
/*
        if (commandLine.hasOption("l")) {
            // do nothing
        }
*/
/*
        if (commandLine.hasOption("o")) {
            String optarg = commandLine.getOptionValue("o").trim();
            int pos = optarg.indexOf(":");
            if (pos >= 0) {
                m_argprop.setProperty(optarg.substring(0, pos), 
                                                    optarg.substring(pos + 1));
            }
        }
*/
        if (commandLine.hasOption("p")) {
        // ORB's port number
            //String str = commandLine.getOptionValue("p").trim();
            String str = commandLine.getOptionValue("p");
            if(str != null){
                str = str.trim();
                int portnum;
                try {
                    portnum = Integer.parseInt(str);
                    String arg = ":"; 
                    arg += str;
                    m_argprop.setProperty("corba.endpoints", arg);
                }
                catch(Exception ex){
                    //do nothing
                }
            }
        }
        if (commandLine.hasOption("d")) {
            m_isMaster = true;
        }
    }
    
    /**
     * {@.ja Configuration file の検索。}
     * {@.en Find the configuration file}
     *
     * <p>
     * {@.ja 使用すべきコンフィグレーションファイルを検索して特定します。
     * すでに特定済みの場合は、そのファイルの存在有無のみをチェックします。
     * 
     * なお、次の順序でコンフィグレーションファイルを検索します。
     * <ul>
     * <li> UNIX/Linux 
     * <ol>
     * <li>コマンドラインオプション -f</li>
     * <li>環境変数 RTC_MANAGER_CONFIG</li>
     * <li>デフォルト設定ファイル ./rtc.conf</li>
     * <li>デフォルト設定ファイル /etc/rtc.conf</li>
     * <li>デフォルト設定ファイル /etc/rtc/rtc.conf</li>
     * <li>デフォルト設定ファイル /usr/local/etc/rtc.conf</li>
     * <li>デフォルト設定ファイル /usr/local/etc/rtc/rtc.conf</li>
     * </ol>
     * <li> Windows
     * <ol>
     * <li>コマンドラインオプション -f</li>
     * <li>環境変数 RTC_MANAGER_CONFIG</li>
     * <li>デフォルト設定ファイル ./rtc.conf</li>
     * <li>デフォルト設定ファイル %RTM_ROOT%/%RTM_VC_VERSION%/rtc.conf</li>
     * </ol></ul>}
     * {@.en Find the configuration file and configure it.
     * Confirm the file existence when the configuration file has 
     * already configured.
     * The configuration file is retrieved in the following order. 
     * <ul>
     * <li> UNIX/Linux 
     * <ol>
     * <li>The command line option -f</li>
     * <li>The environment variable RTC_MANAGER_CONFIG</li>
     * <li>Default configuration file ./rtc.conf</li>
     * <li>Default configuration file /etc/rtc.conf</li>
     * <li>Default configuration file /etc/rtc/rtc.conf</li>
     * <li>Default configuration file /usr/local/etc/rtc.conf</li>
     * <li>Default configuration file /usr/local/etc/rtc/rtc.conf</li>
     * </ol>
     * <li> Windows
     * <OL>
     * <LI>Command option "-f"
     * <LI>Environment variable "RTC_MANAGER_CONFIG"
     * <LI>Default configuration file "./rtc.conf"
     * <LI>Default configuration file "%RTM_ROOT%/%RTM_VC_VERSION%/rtc.conf"
     * </OL></ul>}
     * 
     * @return 
     *   {@.ja <ul>
     *   <li>コンフィグレーションファイル未特定の場合 : 
     *   使用すべきコンフィグレーションファイルを検索・特定できた場合は
     *   trueを、さもなくばfalseを返します。
     *   <li>コンフィグレーションファイル特定済みの場合 : 
     *   特定済みのコンフィグレーションファイルが存在すればtrueを、
     *   さもなくばfalseを返します。}
     *   {@.en <ul>
     *   <li>When the configuration file is the unspecific: 
     *   True is returned when the configuration file can be retrieved and, 
     *   otherwise, false is returned.
     *   <li>When the configuration file is a specific settlement: 
     *   If the configuration file exists, true is returned and, 
     *   otherwise, false is returned.}
     */
    protected boolean findConfigFile() {
        
        // Check existance of configuration file given command arg
        if (! (m_configFile == null || m_configFile.length() == 0)) {
            if (fileExist(m_configFile)) {
                return true;
            }
            System.err.println("Configuration file: "
                               + m_configFile 
                               + " not found.");
        }

        // Search rtc configuration file from environment variable
        String env = System.getenv(CONFIG_FILE_ENV);
        if (env != null) {
            if (fileExist(env)) {
                this.m_configFile = env;
                return true;
            }
            System.err.println("Configuration file: "
                               + env 
                               + " not found.");
        }

        String osname = System.getProperty("os.name").toLowerCase();
        if(osname.startsWith("windows")){
            ArrayList<String> paths = new ArrayList<String>();
            paths.add("./rtc.conf");
            String def_path = System.getenv("RTM_ROOT")
                          + "bin\\"
                          + System.getenv("RTM_VC_VERSION")
                          + "\\rtc.conf";
            paths.add(def_path);
            for(String path:paths){
                if (fileExist(path)) {
                    this.m_configFile = path;
                    return true;
                }
            }
        }
        else{ 
            // Search rtc configuration file from default search path
            for (int i = 0; Constants.CONFIG_FILE_PATH[i] != null; i++) {
                if (fileExist(Constants.CONFIG_FILE_PATH[i])) {
                    m_configFile = Constants.CONFIG_FILE_PATH[i];
                    return true;
                }
            }
        } 
        return false;
    }
    
    /**
     * {@.ja システム情報を設定する。}
     * {@.en Set system information}
     *
     * <p>
     * {@.ja システム情報を取得しプロパティにセットする。
     * 設定されるキーは以下の通り。
     * <ul>
     * <li> os.name    : OS名
     * <li> os.release : OSリリース名
     * <li> os.version : OSバージョン名
     * <li> os.arch    : OSアーキテクチャ
     * <li> os.hostname: ホスト名
     * <li> manager.pid        : プロセスID
     * <li> manager.modules.C++.suffixes: ライブラリの拡張子
     * </ul>}
     * {@.en Get the following system info. and set them to Manager's 
     * properties.
     * <ul>
     * <li> os.name    : OS name
     * <li> os.release : OS release name
     * <li> os.version : OS version
     * <li> os.arch    : OS architecture
     * <li> os.hostname: Hostname
     * <li> manager.pid        : process ID
     * <li> manager.modules.C++.suffixes: The suffix of library
     * </ul>}
     *
     * 
     * @param properties 
     *   {@.ja システム情報を設定したプロパティ}
     *   {@.en Properties to set system information}
     *
     */
    protected void setSystemInformation(Properties properties) {

        String osName = "UNKNOWN";
        String osRelease = "UNKNOWN";
        String osVersion = "UNKNOWN";
        String osArch = "UNKNOWN";
        String hostName = "UNKNOWN";
        String pid = "UNKNOWN";
        String suffix = "UNKNOWN";
        String languages = "C++, Python, Java";
        
        try {
            java.util.Properties sysInfo = System.getProperties();
            
            // OS名
            osName = sysInfo.getProperty("os.name");
            if(osName.toLowerCase().startsWith("windows")){
                suffix = "dll";
                languages = "C++, Python, Java";
            }
            else if(osName.toLowerCase().startsWith("linux")){
                suffix = "so";
                languages = "C++, Python, Python3, Java";
            }
            else{
                suffix = "dylib";
                languages = "C++, Python, Python3, Java";
            }
            
            // OSバージョン
            osVersion = sysInfo.getProperty("os.version");
            
            // OSアーキテクチャ
            osArch = sysInfo.getProperty("os.arch");
            
            //プロセスID
            //pid = System.getProperty("java.version") + new Random().nextInt();
            pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        
        try {
            // ホスト名
            hostName = InetAddress.getLocalHost().getHostName();
            
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        
        properties.setProperty("os.name", osName);
        properties.setProperty("os.release", osRelease);
        properties.setProperty("os.version", osVersion);
        properties.setProperty("os.arch", osArch);
        properties.setProperty("os.hostname", hostName);
        properties.setProperty("manager.pid", pid);
        properties.setProperty("manager.modules.C++.suffixes", suffix);
        properties.setProperty("manager.supported_languages", languages);

    }

    /**
     * {@.ja ファイルの存在確認}
     * {@.en Check the file existence}
     *
     * <p>
     * {@.ja 指定されたファイルが存在するか確認する。}
     * {@.en Confirm whether the specified file exists}
     *
     * @param filePath 
     *   {@.ja ファイルパス}
     *   {@.en The target confirmation file}
     *
     * @return 
     *   {@.ja 対象ファイル確認結果(存在する場合にtrue)}
     *   {@.en file existance confirmation (True if the file exists.)}
     */
    protected boolean fileExist(String filePath) {
        return (new File(filePath)).exists();
    }
    
    /**
     * {@.ja 使用されるコンフィグレーションファイルのパス}
     * {@.en Manager's configuration file path}
     */
    protected String m_configFile = new String();
    /**
     * {@.ja Manager マスタフラグ}
     * {@.en Manager master flag}
     *
     * {@.ja true:マスタ,false:スレーブ}
     * {@.en true:master,false:slave}
     */
    protected boolean m_isMaster;
    /**
     * {@.ja 引数から渡されるプロパティ}
     * {@.en configuration properties from arguments}
     */
    protected Properties m_argprop = new Properties();
    
}
