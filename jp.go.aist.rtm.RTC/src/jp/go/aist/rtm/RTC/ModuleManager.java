package jp.go.aist.rtm.RTC;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import jp.go.aist.rtm.RTC.util.Properties;
import jp.go.aist.rtm.RTC.util.StringHolder;
import jp.go.aist.rtm.RTC.util.StringUtil;

import jp.go.aist.rtm.RTC.log.Logbuf;


  /**
   * {@.ja モジュールマネージャクラス}
   * {@.en ModuleManager class}
   *
   * <p>
   * {@.ja モジュールのロード、アンロードなどを管理するクラス}
   * {@.en This is a class to manage for loading and unloading modules.}
   *
   */
public class ModuleManager {
    
    private final String CONFIG_EXT = "manager.modules.config_ext";
    private final String CONFIG_PATH = "manager.modules.config_path";
    private final String DETECT_MOD = "manager.modules.detect_loadable";
    private final String MOD_LOADPTH = "manager.modules.load_path";
    private final String INITFUNC_SFX = "manager.modules.init_func_suffix";
    private final String INITFUNC_PFX = "manager.modules.init_func_prefix";
    private final String ALLOW_ABSPATH = "manager.modules.abs_path_allowed";
    private final String ALLOW_URL = "manager.modules.download_allowed";
    private final String MOD_DWNDIR = "manager.modules.download_dir";
    private final String MOD_DELMOD = "manager.modules.download_cleanup";
    private final String MOD_PRELOAD = "manager.modules.preload";

    /**
     * {@.ja コンストラクタ。}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたPropertiesオブジェクト内の情報に基づいて
     * 初期化を行います。}
     * {@.en Initialize based on information in the set Property object.}
     *
     * @param properties
     *   {@.ja 初期化情報を持つPropertiesオブジェクト}
     *   {@.en Properties for initialization}
     */
    public ModuleManager(Properties properties) {
        rtcout = new Logbuf("ModuleManager");
        
        m_properties = properties;
        
        m_configPath = new Vector<String>();
        String[] configPath = properties.getProperty(CONFIG_PATH).split(",");
        for (int i = 0; i < configPath.length; ++i) {
            m_configPath.add(configPath[i].trim());
        }
        
        m_loadPath = new Vector<String>();
        String[] loadPath = properties.getProperty(MOD_LOADPTH).split(",");
        /*
        for(int ic=0;ic<loadPath.length;++ic) {
            try{
                loadPath[ic] = new File(loadPath[ic]).getCanonicalPath();
            }
            catch(Exception e){
            }
        }
        */
        String separator =  System.getProperty("file.separator");
        for (int i = 0; i < loadPath.length; ++i) {
            loadPath[i] = loadPath[i].trim();
            if(loadPath[i].substring(0,2).equals("."+separator)){
                loadPath[i] = loadPath[i].substring(2);
            }
            m_loadPath.add(loadPath[i]);
        }
        m_loadPath = recursiveDirectory(m_loadPath);
        
        m_absoluteAllowed = StringUtil.toBool(
                properties.getProperty(ALLOW_ABSPATH), "yes", "no", false);
        m_downloadAllowed = StringUtil.toBool(
                properties.getProperty(ALLOW_URL), "yes", "no", false);
        
        m_initFuncSuffix = properties.getProperty(INITFUNC_SFX);
        m_initFuncPrefix = properties.getProperty(INITFUNC_PFX);
    }
    
    /**
     * {@.ja デストラクタ。}
     * {@.en destructer}
     * 
     * <p>
     * {@.ja ロード済みモジュールのアンロードなど、
     * リソースの解放処理を行います。
     * 当該ModuleManagerオブジェクトの使用を終えた際に、
     * 明示的に呼び出してください。}
     */
    public void destruct() {
        unloadAll();
    }
    
    /**
     * {@.ja ファイナライザ。}
     * {@.en finalize}
     */
    protected void finalize() throws Throwable {
        
        try {
            destruct();
            
        } finally {
            super.finalize();
        }
    }

    /**
     * {@.ja モジュールのロード。}
     * {@.en Load the module}
     *
     * <p>
     * {@.ja file_name をDLL もしくは共有ライブラリとしてロードする。
     * file_name は既定のロードパス (manager.modules.load_path) に対する
     * 相対パスで指定する。
     *
     * Property manager.modules.abs_path_allowed が yes の場合、
     * ロードするモジュールを絶対パスで指定することができる。<br>
     * Property manager.modules.download_allowed が yes の場合、
     * ロードするモジュールをURLで指定することができる。
     *
     * file_name は絶対パスで指定することができる。
     * manager.modules.abs_path_allowed が no の場合、
     * 既定のモジュールロードパスから、file_name のモジュールを探しロードする。}
     * {@.en Load file_name as DLL or a shared liblary.
     * The file_name is specified by the relative path to default load
     * path (manager.modules.load_path).
     *
     * If Property manager.modules.abs_path_allowed is yes,
     * the load module can be specified by the absolute path.<br>
     * If Property manager.modules.download_allowed is yes,
     * the load module can be specified with URL.
     *
     * The file_name can be specified by the absolute path.
     * If manager.modules.abs_path_allowed is no, module of file_name
     * will be searched from the default module load path and loaded.}
     * 
     * @param moduleName 
     *   {@.ja ロード対象モジュール名}
     *   {@.en The target module name for the loading}
     *
     * @return 
     *   {@.ja 指定したロード対象モジュール名}
     *   {@.en Name of module for the specified load}
     *
     *
     */
    public String load(final String moduleName) throws Exception {
        rtcout.println(Logbuf.TRACE, "load(fname = " + moduleName +")");
        String module_path = null;

        if(moduleName==null || moduleName.length()==0) {
            throw new IllegalArgumentException("moduleName is empty.:load()");
        }
        try {
            new URL(moduleName);
            if (! m_downloadAllowed) {
                throw new IllegalArgumentException(
                                    "Downloading module is not allowed.");
            } else {
                throw new ClassNotFoundException(
                                            "Not implemented." + moduleName);
            }
        } catch (MalformedURLException moduleName_is_not_URL) {
            // do nothing
        }

        // Find local file from load path or absolute directory
        String separator =  System.getProperty("file.separator");
        Class target = null;

        File file = new File(moduleName);
        rtcout.println(Logbuf.PARANOID, "Is moduleName AbsolutePath ? " 
                                        + file.exists());
        if(file.exists()){ // When moduleName is AbsolutePath.
            if(!m_absoluteAllowed) {
                throw new IllegalArgumentException(
                                            "Absolute path is not allowed");
            }
            else {
                URLClassLoader url = createURLClassLoader(file.getParent());
                if(url!=null){
                    String name = file.getName();
                    name = getModuleName(name);

                    StringHolder packageModuleName = new StringHolder();
                    target = getClassFromName(url,name,packageModuleName);
                    module_path = packageModuleName.value;
                }
            }
        } else {
            if( m_loadPath.size()==0 ) {
                throw new ClassNotFoundException();
            }
            for (int i = 0; i < m_loadPath.size(); ++i) {
                String fullClassName ;
                if(m_loadPath.elementAt(i).equals("")
                        ||m_loadPath.elementAt(i).length()==0){
                    fullClassName = moduleName;
                }
                else {
                    String packageName = m_loadPath.elementAt(i);
                    fullClassName = packageName + separator + moduleName;
                }
                rtcout.println(Logbuf.PARANOID, "fullClassName = " + fullClassName);
                file = new File(fullClassName);
                rtcout.println(Logbuf.PARANOID, 
                               "getParent = " + file.getParent());
                if(file.isAbsolute()){
                    URLClassLoader url = createURLClassLoader(file.getParent());
                    rtcout.println(Logbuf.PARANOID, "url =" + url);
                    if(url!=null){
                        String name = file.getName();
                        name = getModuleName(name);

                        rtcout.println(Logbuf.PARANOID, "name =" + name);
                        StringHolder packageModuleName = new StringHolder();
                        target = getClassFromName(url,name,packageModuleName);
                        module_path = packageModuleName.value;
                        if(target!=null){
                            break;
                        }
                    }
                }
                else{
                    try {
                        fullClassName = getModuleName(fullClassName);
                        rtcout.println(Logbuf.PARANOID, 
                                       "fullClassName =" + fullClassName);
                        fullClassName 
                                = fullClassName.replace(separator,".");
                        fullClassName = fullClassName.replace("..",".");
                        target = Class.forName(fullClassName);
                        module_path = fullClassName;                    
                        if(target!=null){
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        // do nothing
                    }
                }
            }
        }
        rtcout.println(Logbuf.PARANOID, "target:"+ target);
        if( target==null ) {
            throw new ClassNotFoundException(
                                    "Not implemented." + moduleName);
        }
        rtcout.println(Logbuf.PARANOID, "module_path:"+ module_path);
        if(module_path==null || module_path.length()==0) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        DLLEntity dll_entity = new DLLEntity();
        dll_entity.properties = new Properties();
        dll_entity.properties.setProperty("file_path",module_path);
        dll_entity.dll = target;

        m_modules.put(module_path, dll_entity);
        return module_path;
    }

    /**
     *
     */
    private Class getClassFromName(URLClassLoader url, 
                                    String name, 
                                    StringHolder holder){
        String separator =  System.getProperty("file.separator");
        Class target = null;

        try {
            target = url.loadClass(name);
            holder.value = name;
        } catch (java.lang.NoClassDefFoundError e) {
            String messagetString = e.getMessage();
            String key = "wrong name: ";
            int index = messagetString.indexOf(key);
            String packageName 
                = messagetString.substring(index+key.length(),
                                               messagetString.length()-1);
            URL[] urls = url.getURLs();
            java.util.ArrayList al 
                    = new java.util.ArrayList(java.util.Arrays.asList(urls));
            for(int ic=0;ic<urls.length;++ic){
                String stringPath = new String();
                String stringUrl = new String();
                try{
                    stringUrl = urls[ic].toURI().getPath();
                }
                catch(Exception ex){
                    continue;
                } 
                int pointer = packageName.lastIndexOf(name);
                String stringPackageName = packageName.substring(0, pointer);
                if(stringUrl.endsWith(stringPackageName)){
                    int point = stringUrl.lastIndexOf(stringPackageName);
                    stringPath = stringUrl.substring(0, point);
                    File path = new File(stringPath);
                    try{
                        URI uri = path.toURI();
                        al.add(uri.toURL());
                    }
                    catch(java.net.MalformedURLException ex){
                        System.err.println(
                 "java.net.MalformedURLException: toURL() threw Exception."+ex);
                    }
                }
            }
            URL[] addUrls = (URL[])al.toArray(new URL[]{});
            url = url.newInstance(addUrls, url);

            packageName = packageName.replace("/",".");
            packageName = packageName.trim();

            target = getClassFromName(url,packageName,holder);
        } catch (Exception e) {
            //
        }
        return target;
    }

    /**
     * {@.ja モジュール名作成する。}
     * <p>
     * {@.ja 拡張子を削除する。拡張子jarの場合はモジュール名を付加する}
     */
    private String getModuleName(String name){
        String extensions[] = {".class", ".jar"};
        for(int ic=0;ic<extensions.length;++ic){
            if(name.endsWith(extensions[ic])){
                int point = name.lastIndexOf(extensions[ic]);
                name =  name.substring(0, point);
                if(extensions[ic].equals(".jar")){
                    name =  name+"."+name;
                }
                break;
            }
        }
        return name;
    }

    /**
     *
     */
    private URLClassLoader createURLClassLoader(String parent){
        File path = new File(parent);
        URL[] urls = new URL[1];
        try{
            URI uri = path.toURI();
            urls[0] = uri.toURL();
        } catch(java.net.MalformedURLException ex){
            rtcout.println(Logbuf.WARN, 
                "java.net.MalformedURLException: toURL() threw Exception."+ex);
            return null;
        }
        URLClassLoader url = new URLClassLoader(urls);
        return url;
    }

    /**
     * {@.ja モジュールのアンロード。}
     * {@.en Load and intialize the module}
     *
     * <p>
     * {@.ja 指定されたモジュールをロードします。初期化メソッドを指定した場合
     * には、 * ロード時にそのメソッドが呼び出されます。
     * これにより、モジュール初期化を行えます。>
     * 
     * コンストラクタで指定した初期化情報の 'manager.modules.abs_path_allowed'
     * が 'yes' の場合は、className引数は、ロードモジュールのフルクラス名
     * として解釈されます。<br />
     * 'no' が指定されている場合は、className引数はロードモジュールの
     * シンプルクラス名として解釈され、
     * 規定のモジュールロードパス以下からモジュールが検索されます。
     * 
     * コンストラクタで指定した初期化情報の 'manager.modules.download_allowed'
     * が 'yes' の場合は、
     * className引数は、ロードモジュールのURLとして解釈されます。（未実装）}
     * {@.en Load the specified file as DLL or a shared library, and execute 
     * operation for specified initialization.}
     * 
     * @param moduleName 
     *   {@.ja モジュール名}
     *   {@.en A module name}
     * @param methodName 
     *   {@.ja 初期実行メソッド名}
     *   {@.en a initial method name }
     * @return 
     *   {@.ja moduleName引数で指定したモジュール名がそのまま返されます。}
     *   {@.en The module name specified by the argument is returned. }
     * @throws IllegalArgumentException 
     *   {@.ja 引数が正しく指定されていない場合にスローされます。}
     *   {@.en When the argument is not correctly specified, it is thrown out. }
     */
    public String load(final String moduleName, final String methodName)
            throws Exception {
        rtcout.println(Logbuf.TRACE, 
                "load(fname = "+moduleName+"   init_func = "+methodName+")");
        
        if (moduleName == null || moduleName.length() == 0) {
            throw new IllegalArgumentException("moduleName is empty.:load()");
        }
        if (methodName == null || methodName.length() == 0) {
            throw new IllegalArgumentException("methodName is empty.:load()");
        }

        String module_path = load(moduleName);

        Method initMethod = symbol(module_path,methodName);


        DLLEntity dll_entity = m_modules.get(module_path);
        Class target = dll_entity.dll;
        if(target == null){
            throw new ClassNotFoundException("Not implemented." + moduleName); 
        }


        try {
            initMethod.invoke(target.newInstance());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw e;
        } catch (InstantiationException e) {
            throw e;
        }
        
        return module_path;
    }
    
    /**
     * {@.ja モジュールのアンロード}
     * {@.en Unload the module}
     *
     * <p>
     * {@.ja 指定したロード済みモジュールをクローズし、アンロードする。}
     * {@.en Close and unload the specified module that has been loaded.}
     *
     * @param moduleName 
     *   {@.ja アンロード対象モジュール名}
     *   {@.en Name of module for the unloading}
     */
    public void unload(String moduleName) throws Exception {
        if( !m_modules.containsKey(moduleName) ) 
            throw new IllegalArgumentException("Not Found:" + moduleName);
        m_modules.remove(moduleName);
    }
    
    /**
     * {@.ja 全モジュールのアンロード}
     * {@.en Unload all modules}
     *
     * <p>
     * {@.ja 全てのロード済みモジュールをアンロードする。}
     * {@.en Unload all modules that have been loaded.}
     *
     */
    public void unloadAll() {
        m_modules = new HashMap<String, DLLEntity>();
    }
    
    /**
     * {@.ja モジュールのメソッドの参照。}
     * {@.en Refer to the symbol of the method}
     *
     * @param class_name 
     *   {@.ja クラスの名前}
     *   {@.en Name of class}
     * @param method_name
     *   {@.ja メソッドの名前}
     *   {@.en Name of method}
     *
     * @return
     *   {@.ja メソッド}
     *   {@.en method}
     */
    public Method symbol(String class_name, String method_name) 
        throws Exception {
        Class target = m_modules.get(class_name).dll;
        if( target==null ) {
            throw new IllegalArgumentException(
                                    "Not Found(symbol):" + class_name);
        }
        //
        Method initMethod;
        try {
            initMethod = target.getMethod(method_name, null);
        } catch (SecurityException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            throw e;
        }
        
        return initMethod;
    }
    /**
     * <p>初期化関数シンボルを生成する</p>
     */
    /**
     * {@.ja 初期化関数シンボルを生成する}
     * {@.en Create initialization function symbol}
     *
     * <p>
     * {@.ja 初期化関数の名称を組み立てる。}
     * {@.en Assemble names of the initialization functions.}
     *
     * @param class_path 
     *   {@.ja 初期化対象モジュール名称}
     *   {@.en Name of module for initialization}
     *
     * @return 
     *   {@.ja 初期化関数名称組み立て結果}
     *   {@.en Assembly result of initialization function name}
     */
    public String getInitFuncName(String class_path) {
        if( class_path==null || class_path.length()==0 ) return null;
        String base_names[] = class_path.split("\\.");

        return m_initFuncPrefix + base_names[base_names.length-1] + m_initFuncSuffix;
    }

    /**
     * {@.ja モジュールロードパスを指定する。}
     * {@.en Set the module load path}
     * 
     * <p>
     * {@.ja モジュールロード時に対象モジュールを検索するパスを指定する。}
     * {@.en Specify searching path to find the target module when loading 
     * module.}
     *
     * @param loadPath 
     *   {@.ja 規定ロードパス}
     *   {@.en List of module search path}
     */
    public void setLoadpath(final Vector<String> loadPath) {
        m_loadPath = new Vector<String>(loadPath);
        m_loadPath = recursiveDirectory(m_loadPath);
    }
    
    /**
     * {@.ja モジュールロードパスを取得する。}
     * {@.en Get the module load path}
     * 
     * <p>
     * {@.ja 設定されているモジュールを検索対象パスリストを取得する。}
     * {@.en Get the search path of the set module.}
     * 
     * @return
     *   {@.ja 規定モジュールロードパス}
     *   {@.en List of module search path}
     *
     */
    public Vector<String> getLoadPath() {
        return new Vector<String>(m_loadPath);
    }

    /**
     * {@.ja モジュールロードパスを追加する。}
     * {@.en Add the module load path}
     * 
     * <p>
     * {@.ja 指定されたパスリストを検索対象パスリストに追加する。}
     * {@.en Add specified path list to search path list.}
     * 
     * @param loadPath
     *   {@.ja 追加する規定ロードパス}
     *   {@.en List of additional module search path}
     */
    public void addLoadPath(final Vector<String> loadPath) {
        Vector<String>loadpath = recursiveDirectory(loadPath);
        m_loadPath.addAll(loadpath);
    }
    
    /**
     * {@.ja ロード済みのモジュールリストを取得する}
     * {@.en Get the module list that has been loaded}
     *
     * <p>
     * {@.ja 既にロード済みのモジュールリストを取得する。}
     * {@.en Get the module list that has been loaded.}
     *
     * @return 
     *   {@.ja ロード済みモジュールリスト}
     *   {@.en List of module that has been loaded}
     */
    public Vector<Properties> getLoadedModules() {
        Vector<Properties> props = new Vector<Properties>();
        Set<String> str_set = m_modules.keySet();
        for(Iterator it=str_set.iterator();it.hasNext();){
           props.add(m_modules.get(it.next()).properties); 
        }
        return props;
    }
        
    /**
     * {@.ja ロード可能モジュールリストを取得する}
     * {@.en Get the loadable module list}
     *
     * <p>
     * {@.ja ロード可能なモジュールのリストを取得する。}
     * {@.en Get the loadable module list (not implemented).}
     *
     * @return
     *   {@.ja ロード可能モジュールリスト}
     *   {@.en Loadable module list}
     *
     */
    public Vector<Properties> getLoadableModules() {
        rtcout.println(Logbuf.TRACE, "getLoadableModules()");
        //# getting loadable module file path list.

        String[] langs = 
            m_properties.getProperty("manager.supported_languages").split(",");
        rtcout.println(Logbuf.DEBUG, 
                   "manager.supported_languages:"
                   +m_properties.getProperty("manager.supported_languages"));

        for(int ic=0;ic<langs.length;++ic) {
            String lang = langs[ic].trim();

            ArrayList<String> modules = new ArrayList<String>();
            getModuleList(lang, modules);
            rtcout.println(Logbuf.TRACE, lang + ":" +modules.toString());

            ArrayList<Properties> tmpprops = new ArrayList<Properties>();
            getModuleProfiles(lang, modules, tmpprops);
            rtcout.println(Logbuf.TRACE, 
                           "Modile profile size: "
                           +tmpprops.size()
                           +" (newly founded modules)");
            m_modprofs.addAll(tmpprops);
        }

        rtcout.println(Logbuf.DEBUG, 
                       "Modile profile size: "
                       + m_modprofs.size());
                       
        removeInvalidModules();
        rtcout.println(Logbuf.DEBUG, 
                       "Modile profile size: "
                       + m_modprofs.size()
                       + " (invalid mod-profiles deleted)");
        
        return new Vector<Properties>(m_modprofs);
    }
    /**
     * {@.ja 無効なモジュールプロファイルを削除する}
     * {@.en Removing incalid module profile}
     */
    protected void removeInvalidModules(){

        Iterator it = m_modprofs.iterator();
        while (it.hasNext()) {
            Properties prop = (Properties)it.next();
            File file = new File(prop.getProperty("module_file_path"));
            if(!file.exists()){
                it.remove();
            }
        }

    }
    /**
     * {@.ja 指定言語におけるロードパス上のローダブルなファイルリストを返す}
     * {@.en loadable file list on the loadpath for given language}
     *
     * void getModuleList(const std::string& lang, coil::vstring& modules);
     */
    protected void getModuleList(final String lang, ArrayList<String> modules) {
        rtcout.println(Logbuf.PARANOID,
                       "getModuleList("+lang+","+modules.toString()+")");
        String l = "manager.modules." + lang;
        Properties lprop = Manager.instance().getConfig().getNode(l);

        //load path: manager.modules.<lang>.load_path 
        //           + manager.modules.load_path
        rtcout.println(Logbuf.PARANOID,
                               "load_paths :"+lprop.getProperty("load_paths"));
        String[] vstr = lprop.getProperty("load_paths").split(",");
        /*
        for(int ic=0;ic<vstr.length;++ic) {
            try{
                vstr[ic] = new File(vstr[ic]).getCanonicalPath();
            }
            catch(Exception e){
            }
        }
        */
        ArrayList<String> paths = new ArrayList(Arrays.asList(vstr));

        rtcout.println(Logbuf.PARANOID,
                               "m_loadPath :"+m_loadPath.toString());

        paths.addAll(m_loadPath);
        paths = deleteSamePath(paths);
        rtcout.println(Logbuf.PARANOID,
                               "paths :"+paths.toString());

        String[] vstr2 = lprop.getProperty("suffixes").split(",");
        ArrayList<String> suffixes = new ArrayList(Arrays.asList(vstr2));
        rtcout.println(Logbuf.DEBUG, 
                       "suffixes: "
                       + suffixes.toString());

        // for each load path list
        for (String path : paths) {
            if (path.isEmpty()) {
                rtcout.println(Logbuf.WARN,"Given load path is empty");
                continue;
            }
            rtcout.println(Logbuf.DEBUG,"Module load path: "+path);

            // get file list for each suffixes
            ArrayList<String> flist = new ArrayList<String>();
            File dir = new File(path);
            rtcout.println(Logbuf.PARANOID,"dir:"+dir);
            if(!dir.exists()){
                continue;
            }
            for (String suffix: suffixes) {
                String  glob = ".*\\."; 
                glob += suffix.trim();
                rtcout.println(Logbuf.PARANOID,"glob: "+glob);
                String[] files = dir.list(new FilePathFilter(glob));
                if(files == null) {
                    continue;
                }
                rtcout.println(Logbuf.PARANOID,"files.length:"+files.length);
                ArrayList<String> tmp 
                    = new ArrayList<String>(Arrays.asList(files));
                rtcout.println(Logbuf.DEBUG,
                                   "File list (path:"
                                   +path
                                   +", ext:"
                                   +suffix
                                   +"): "
                                   +tmp.toString());
                flist.addAll(tmp);
            }
        
            // reformat file path and remove cached files
            String separator =  System.getProperty("file.separator");
            for (String file : flist) {
                if (!path.endsWith("/")) { 
                    path += "/"; 
                }
                //if (!path.endsWith(separator)) { 
                //    path += separator; 
                //}
                String fpath = path + file;
                addNewFile(fpath, modules);
            }
        }
    }
    /**
     * {@.ja 同じパスを削除}
     * {@.en Deletes the same path.}
     * <p>
     * {@.ja 指定されたリスト内を走査して重複しているパス名を削除する。}
     * {@.en Deletes the path a list overlaps.}
     */
    private ArrayList<String> deleteSamePath(ArrayList<String> paths){
        rtcout.println(Logbuf.PARANOID,
                        "deleteSamePath("+paths.toString()+")");

        ArrayList<String> tmp_paths = new ArrayList<String>();
        for(String path_str : paths){
            rtcout.println(Logbuf.PARANOID,
                        "path_str:"+path_str);
            if(!path_str.isEmpty()){
                Path path = Paths.get(path_str);
                path = path.normalize();
                Path absolutePath = path.toAbsolutePath();
                rtcout.println(Logbuf.PARANOID,
                        "absolutePath:"+absolutePath);
                
                ArrayList<Path> abs_tmp_paths = new ArrayList<Path>();
                for(String tmp_path : tmp_paths){
                    Path abs_tmp_path = Paths.get(tmp_path);
                    abs_tmp_path = abs_tmp_path.normalize();
                    abs_tmp_paths.add(abs_tmp_path);
                }
                if(abs_tmp_paths.indexOf(absolutePath) == -1){
                    tmp_paths.add(path_str);
                }
            }
        }
        rtcout.println(Logbuf.PARANOID,
                        "tmp_paths:"+tmp_paths.toString());

        return tmp_paths;
    }
    /**
     * {@.ja キャッシュに無いパスだけmodulesに追加する}
     * {@.en Adding file path not existing cache}
     */
    protected void addNewFile(final String fpath,
                                  ArrayList<String> modules) {
        rtcout.println(Logbuf.PARANOID, 
                               "addNewFile("+fpath+")");
        boolean exists = false;
        for (Properties modprof : m_modprofs) {
            rtcout.println(Logbuf.PARANOID, 
                               "module_file_path:"
                               +modprof.getProperty("module_file_path"));
            if (modprof.getProperty("module_file_path").equals(fpath)) {
                exists = true;
                rtcout.println(Logbuf.DEBUG, 
                               "Module "+fpath+" already exists in cache.");
                break;
            }
        }
        if (!exists) {
            rtcout.println(Logbuf.DEBUG, "New module: "+fpath);
            modules.add(fpath);
        }
    }

    /**
     * {@.ja ファイルフィルタ用関数}
     * {@.en The function for file filters}
     * <p>
     * {@.ja ファイル名にフィルタをかけるために使用される。}
     * {@.en This class is used to filter directory listings 
     * in the list method of class File.}
     */
    private class FilePathFilter implements FilenameFilter{
        private String m_regex = new String();
        public FilePathFilter(String str) {
            m_regex = str;
        }
        @Override
        public boolean accept(File dir, String name) {
            if(m_regex.isEmpty()){
                 return false;
            }
         
            if(name.matches(m_regex)){
                return true;
            }
            return false;
        }
    }  


    /**
     * {@.ja 指定言語、ファイルリストからモジュールのプロパティを返す}
     * {@.en Getting module properties from given language and file list}
     *
     */
    protected void getModuleProfiles(final String lang,
                                     final ArrayList<String> modules,
                                     final ArrayList<Properties> modprops) {
        rtcout.println(Logbuf.PARANOID, 
                       "getModuleProfiles("+lang+","+modules.toString()+")");

        String  l = "manager.modules." + lang;
        Properties lprop = Manager.instance().getConfig().getNode(l);

        String[] paths = lprop.getProperty("load_paths").split(",");
        /*
        for(int ic=0;ic<paths.length;++ic) {
            try{
                paths[ic] = new File(paths[ic]).getCanonicalPath();
            }
            catch(Exception e){
            }
        }
        */
        Properties prop = new Properties();

        for (String module : modules) {
            if(lang.equals("Java")){
                prop = getRtcProfile(module);
                if(prop!=null){
                    prop.setProperty("module_file_name",
                                     new File(module).getName());
                    prop.setProperty("module_file_path", module);
                    prop.setProperty("language", lang);
                    modprops.add(prop);
                }
            }
            else{
                prop = new Properties();
                String cmd = lprop.getProperty("profile_cmd");
                rtcout.println(Logbuf.PARANOID, 
                       "profile_cmd: "+cmd);
                List<String> cmdlist = new ArrayList();
                String osname = System.getProperty("os.name").toLowerCase();
                if(osname.startsWith("windows")){
                    module = module.replace("\\","/");
                    cmdlist.add("cmd");
                    cmdlist.add("/c");
                }
                cmdlist.add(cmd);
                cmdlist.add(module);
                Process process;
                InputStream input;
                try{
                    rtcout.println(Logbuf.PARANOID, 
                                   "cmdlist :"+cmdlist.toString());
                    ProcessBuilder pb = new ProcessBuilder(cmdlist);
                    process = pb.start();
                    input = process.getInputStream();
                    process.waitFor();
                }
                catch(Exception ex){
                    rtcout.println(Logbuf.DEBUG, cmd + ": failed");
                    String crlf = System.getProperty("line.separator");
                    rtcout.println(Logbuf.PARANOID, crlf + ex.toString());
                    continue;
                }
      
                try{
                    BufferedReader br = new BufferedReader( 
                                        new InputStreamReader(input));
 
                    String line;
 
                    rtcout.println(Logbuf.PARANOID, "-----");
                    for (;;) {
                        line = br.readLine();
                        rtcout.println(Logbuf.PARANOID, line);
                        if(line == null){
                            break;
                        }
                        String[] vstr = line.split("[\\s]*:[\\s]*");
                        if(vstr.length != 1){
                            prop.setProperty(vstr[0],vstr[1]);
                        }
                    }
                    rtcout.println(Logbuf.PARANOID, "-----");
 
                    br.close();
                    input.close();
                }
                catch(Exception ex){
                    String crlf = System.getProperty("line.separator");
                    rtcout.println(Logbuf.DEBUG, crlf + ex.toString());
                    continue;
                }
                rtcout.println(Logbuf.PARANOID, "prop.size():"+prop.size());
                if(!prop.getProperty("implementation_id").isEmpty()){
                    prop.setProperty("module_file_name",
                                     new File(module).getName());
                    prop.setProperty("module_file_path", module);
                    prop.setProperty("language", lang);
                    rtcout.println(Logbuf.PARANOID, 
                            "module_file_name:"
                            +prop.getProperty("module_file_name"));
                    rtcout.println(Logbuf.PARANOID, 
                            "module_file_path:"
                            +prop.getProperty("module_file_path"));
                    modprops.add(prop);
                }
            }
        }
        return;
    }

    private Properties getRtcProfile(String loadpath){
        if(loadpath==null || loadpath.equals("")){
            return null;
        }
        Properties prop = new Properties();
        Class target = null;
        File file = new File(loadpath);
        if(file.isAbsolute()) {
            URLClassLoader url = createURLClassLoader(file.getParent());
            if(url!=null){
                String name = file.getName();
                name = getModuleName(name);
                StringHolder packageModuleName = new StringHolder();
                target = getClassFromName(url,name,packageModuleName);
            }
        }
        else{
            String separator =  System.getProperty("file.separator");
            String str[] = loadpath.split("\\.class");
            str[0] = str[0].replace(separator,".");
            str[0] = str[0].replace("..",".");
            try {
                target = Class.forName(str[0],
                                     true,
                                     this.getClass().getClassLoader());
            }
            catch(Exception e){
                return null;
            }
        }
        try {
            if(target == null){
                return null;
            }
            Field field = target.getField("component_conf");
            String[] data = (String[])field.get(null);
            java.util.ArrayList al 
                = new java.util.ArrayList(java.util.Arrays.asList(data));
        
            prop = new Properties((String[])al.toArray(new String[]{}));
            rtcout.println(Logbuf.TRACE, 
                                "loadabe module:"+loadpath);
        } 
        catch(Exception e){
            return null;
        }
     
        return prop;
    }
/*
        ArrayList<String> dlls = new ArrayList<String>();
        String separator =  System.getProperty("file.separator");
        java.io.File dir = new java.io.File(loadpath);

        String[] flist = dir.list(new FileFilter());
        if(flist == null) {
            return;
        }
        for (int ic=0; ic < flist.length; ++ic) {
            dlls.add(loadpath+separator+flist[ic]);
        }  

        ArrayList<Properties> props = new ArrayList<Properties>();
        for (int ic=0; ic < dlls.size(); ++ic) {
            Class target = null;
            File file = new File(dlls.elementAt(ic));
            if(file.isAbsolute()) {
                URLClassLoader url = createURLClassLoader(file.getParent());
                if(url!=null){
                    String name = file.getName();
                    name = getModuleName(name);
                    StringHolder packageModuleName = new StringHolder();
                    target = getClassFromName(url,name,packageModuleName);
                }
            }
            else{
                String str[] = dlls.elementAt(ic).split("\\.class");
                str[0] = str[0].replace(separator,".");
                str[0] = str[0].replace("..",".");
                try {
                    target = Class.forName(str[0],
                                         true,
                                         this.getClass().getClassLoader());
                }
                catch(Exception e){
                    continue;
                }
            }
            try {
                if(target == null){
                    continue;
                }
                Field field = target.getField("component_conf");
                String[] data = (String[])field.get(null);
                java.util.ArrayList al 
                    = new java.util.ArrayList(java.util.Arrays.asList(data));
            
                //al.add(0,"module_file_name");
                //al.add(1,file.getName());
                //al.add(2,"module_file_path");
                //al.add(3,dlls.elementAt(ic));
                props.add(new Properties((String[])al.toArray(new String[]{})));
                rtcout.println(Logbuf.TRACE, 
                                    "loadabe module:"+dlls.elementAt(ic));
            } 
            catch(Exception e){
                continue;
            }
        }  
         
        return props;
*/
    
    /**
     * {@.ja モジュールの絶対パス指定許可。}
     * {@.en Allow absolute path when specify module path}
     *
     * <p>
     * {@.ja ロード対象モジュールの絶対パス指定を許可するように設定する。}
     * {@.en Set to allow the absolute path when specify the module for 
     * the load.}
     */
    public void allowAbsolutePath() {
        m_absoluteAllowed = true;
    }
    
    /**
     * {@.ja モジュールの絶対パス指定禁止}
     * {@.en Disallow absolute path when specify module path}
     *
     * <p>
     * {@.ja ロード対象モジュールの絶対パス指定を禁止するように設定する。}
     * {@.en Set to disallow the absolute path when specify the module for 
     * the load.}
     */
    public void disallowAbsolutePath() {
        m_absoluteAllowed = false;
    }
    
    /**
     * {@.ja モジュールのURL指定許可。}
     * {@.en Allow URL when specify module path}
     *
     * <p>
     * {@.ja ロード対象モジュールのURL指定を許可する。
     * 本設定が許可されている場合、モジュールをダウンロードしてロードすることが
     * 許可される。}
     * {@.en Allow URL when specify module for the load.
     * When this setup is allowed, downloading and loading the module will
     * be allowed.}
     */
    public void allowModuleDownload() {
        m_downloadAllowed = true;
    }
    
    /**
     * {@.ja モジュールのURL指定禁止}
     * {@.en Disallow URL when specify module path}
     *
     * <p>
     * {@.ja ロード対象モジュールのURL指定を禁止する。}
     * {@.en Disallow URL when specify module for the load.}
     */
    public void disallowModuleDownload() {
        m_downloadAllowed = false;
    }
    
    /**
     * {@.ja Module Manager プロパティ}
     * {@.en Module Manager properties}
     */
    protected Properties m_properties;
    /**
     * {@.ja ロード済みモジュール}
     * {@.en Module list that has already loaded}
     */

    protected Map<String, DLLEntity> m_modules 
                                        = new HashMap<String, DLLEntity>();
    private class DLLEntity {
        public Properties properties;
        public Class dll;
    }


    /**
     * {@.ja モジュール・ロード・パス・リスト}
     * {@.en Module load path list}
     */
    protected Vector<String> m_loadPath = new Vector<String>();
    /**
     * {@.ja コンフィギュレーション・パス・リスト}
     * {@.en Configuration path list}
     */
    protected Vector<String> m_configPath = new Vector<String>();
    /**
     * {@.ja モジュールURL指定許可フラグ}
     * {@.en Flag of URL when specify module for the load.}
     */
    protected boolean m_downloadAllowed;
    /**
     * {@.ja モジュール絶対パス指定許可フラグ}
     * {@.en Flag of absolute path when specify module for the load.}
     */
    protected boolean m_absoluteAllowed;
    /**
     * {@.ja 初期実行関数サフィックス}
     * {@.en Initial execution function suffix}
     */
    protected String m_initFuncSuffix = new String();
    /**
     * {@.ja 初期実行関数プリフィックス}
     * {@.en Initial execution function prefix}
     */
    protected String m_initFuncPrefix = new String();

    private class FileFilter implements java.io.FilenameFilter {
        private final String FILTER_KEYWORD = ".class,.jar";

        public boolean accept(java.io.File dir, String name) {
            java.io.File file = new java.io.File(name);

            if(file.isDirectory()){
               return false;
            }
            String[] filter = FILTER_KEYWORD.split(",");
            for(int ic=0;ic<filter.length;++ic){ 
                if(name.endsWith(filter[ic])){
                    return true;
                }
            }
            return false;
        }
    }
    private Logbuf rtcout;
    private ArrayList<Properties> m_modprofs = new ArrayList<Properties>();
    /**
     * {@.ja 指定したパス以下に存在するディレクトリを探索する}
     * {@.en Searches the directory which exists in below of designated paths.}
     */
    private Vector<String> recursiveDirectory(Vector<String> paths){
        Vector<String> result = new Vector<String>();
        for(String path:paths){
            Stack<File> stack = new Stack<>();
            File temp_path = new File(path);
            stack.add(temp_path);
            if(!temp_path.isDirectory()){
                result.add(path);
            }
            while(!stack.isEmpty()){
                File item = stack.pop();
                if (item.isDirectory()) {
                    String str = item.getPath();
                    String osname = System.getProperty("os.name").toLowerCase();
                    if(osname.startsWith("windows")){
                        str = str.replace("\\","/");
                    }
                    if(result.indexOf(str)==-1){
                        result.add(str);
                        if(item.listFiles()!=null){
                            for (File child : item.listFiles()){
                                stack.push(child);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
