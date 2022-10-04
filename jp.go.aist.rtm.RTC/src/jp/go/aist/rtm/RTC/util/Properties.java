package jp.go.aist.rtm.RTC.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * {@.ja キーと値のセットからなるプロパティセットを表す。}
 * {@.en Property set that consists of key and value}
 * <p>
 * {@.ja ストリームへ保管したり、ストリームからロードしたりできる。
 * 各プロパティのキー、およびそれに対応する値は文字列。
 * プロパティセットには、デフォルトを指定することができ、
 * 元のプロパティセットで指定されたキーが見つからない場合には、
 * この２番目のプロパティセットが検索される。}
 * {@.en Can keep in the stream, and load it from the stream. 
 * The key to each property and the value corresponding to it are character.
 * When default can be specified for the property set, and the key specified by
 * former property set is not found, this second property set is retrieved.} 
 * 
 */
public class Properties {

    private String name;
    private String value;
    private String default_value;
    private boolean set_value = false;
    private Properties root;
    private Vector<Properties> leaf = new Vector<Properties>();
    private static final String EMPTY = "";
	
    /**
     * {@.ja デフォルトコンストラクタ}
     * {@.en Default constructor}
     */
    public Properties() {
        
        this("", "");
    }
    
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたキーを持つプロパティが初期設定される。
     * 指定されたキーに対応する値は空文字列となる。}
     * {@.en The property with the specified key is initialized. 
     * The value corresponding to the specified key becomes a null character
     * string.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     */
    public Properties(String key) {
        
        this(key, "");
    }
    
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたキーおよび値を持つプロパティが初期設定される。}
     * {@.en The property with specified key and value is initialized.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @param value 
     *   {@.ja 値}
     *   {@.en value}
     */
    public Properties(final String key, final String value, final boolean in_set_value) {
		
	this.name = key;
	this.value = value;
	this.default_value = "";
	this.root = null;
	this.set_value = this.value.isEmpty() ? in_set_value : true;
		
	this.leaf.clear();
    }
    
    public Properties(final String key, final String value) {
        
        this.name = key;
        this.value = value;
        this.default_value = "";
        this.root = null;
        this.set_value = this.value.isEmpty() ? false : true;
            
        this.leaf.clear();
    }
        
	
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたデータでデフォルトが初期設定される}
     * {@.en Default is initialized by the specified data.}
     * 
     * @param defaults 
     *   {@.ja デフォルトとなるキーと値を持つMapオブジェクト}
     *   {@.en Map object with key and value of default}
     */
    public Properties(Map<String, String> defaults) {
		
	this.name = "";
	this.value = "";
	this.default_value = "";
	this.root = null;
		
	this.leaf.clear();
		
	Iterator<Map.Entry<String, String>> it = defaults.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry<String, String> entry = it.next();
	    setDefault(entry.getKey(), entry.getValue());
	}
    }
	
    /**
     * {@.ja コンストラクタ}
     * {@.en Constructor}
     * <p>
     * {@.ja 指定されたデータでデフォルトが初期設定される。}
     * {@.en Default is initialized by the specified data.}
     * 
     * @param defaults 
     *   {@.ja デフォルト値を、キー・値の順に交互に並べたもの}
     *   {@.en Default value alternately arranged in order of key and value}
     */
    public Properties(final String[] defaults) {
		
	this.name = "";
	this.value = "";
	this.default_value = "";
	this.root = null;
		
	this.leaf.clear();
		
	setDefaults(defaults);
    }

    /**
     * {@.ja コピーコンストラクタ}
     * {@.en Cpoy constructor}
     *
     * <p>
     * {@.ja コピー元となるPropertiesオブジェクトと同内容を持つ
     * 別のPropertiesオブジェクトを新たに作成する。}
     * {@.en copies the Properties object.}
     * 
     * @param prop 
     *   {@.ja コピー元となるPropertiesオブジェクト}
     *   {@.en Properties object that becomes copy origin}
     */
    public Properties(final Properties prop) {
		
	this.name = prop.name;
	this.value = prop.value;
	this.default_value = prop.default_value;
	this.root = null;
	this.set_value = prop.set_value;
		
	Vector<String> keys = prop.propertyNames();
	for (int i = 0; i < keys.size(); ++i) {
	    Properties node;
	    if ((node = prop.findNode(keys.get(i))) != null) {
		setDefault(keys.get(i), node.default_value);
		if(node.set_value)
		{
		    setProperty(keys.get(i), node.value);
		}
	    }
	}
    }

    /**
     * {@.ja 指定されたPropertiesオブジェクトの内容を、
     * 当該Propertiesオブジェクトに設定する。}
     * {@.en copies the Properties object.}
     *
     * @param prop 
     *   {@.ja コピー元となるPropertiesオブジェクト}
     *   {@.en Properties object that becomes copy origin}
     */
    public void substitute(final Properties prop) {
		
	clear();
        this.name = prop.name;
        this.value = prop.value;
        this.default_value = prop.default_value;

        Vector<String> keys = prop.propertyNames();
        for (int i = 0; i < keys.size(); ++i) {
            Properties node;
            if ((node = prop.findNode(keys.get(i))) != null) {
                setDefault(keys.get(i), node.default_value);
                setProperty(keys.get(i), node.value);
            }
        }
    }
	
    /**
     * {@.ja 削除処理を行う。}
     * {@.en Destructor processing}
     * <p>
     * {@.ja 当該Propertiesオブジェクトの内容をクリアして、
     * 親ノードから切り離す。
     * また、すべての子ノードについても削除処理を行う。}
     * {@.en It separates from the parent node clearing the content of the
     * Properties object. 
     * Moreover, the destructor of all the child nodes is processed. }
     */
    public void destruct() {
		
	// Delete children
	clear();
		
	// delete myself from parent
	if (this.root != null) {
	    this.root.removeNode(this.name);
	}
    }
	
    protected void finalize() throws Throwable {
	try {
	    destruct();
            
	} finally {
	    super.finalize();
	}
    }
	
    /**
     * {@.ja 当該Propertiesオブジェクトのキーを取得する。}
     * {@.en Gets the key to an object concerned Properties.}
     * 
     * @return 
     *   {@.ja キー}
     *   {@.en key}
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@.ja 当該Propertiesオブジェクトの値を取得する。}
     * {@.en Gets the value to an object concerned Properties.}
     * 
     * @return 
     *   {@.ja 値}
     *   {@.en value}
     */
    public String getValue() {
        return this.value;
    }

    /**
     * {@.ja 当該Propertiesオブジェクトのデフォルト値を取得する。}
     * {@.en Gets the default value to an object concerned Properties.}
     * 
     * @return 
     *   {@.ja デフォルト値}
     *   {@.en default value}
     */
    public String getDefaultValue() {
        return this.default_value;
    }

    /**
     * {@.ja 当該Propertiesオブジェクトの子ノード群を取得する。}
     * {@.en Gets the leaf to an object concerned Properties.}
     * 
     * @return 
     *   {@.ja 子ノードPropertiesオブジェクトリスト}
     *   {@.en Child node Properties object list}
     */
    public Vector<Properties> getLeaf() {
        return this.leaf;
    }

    /**
     * {@.ja 当該Propertiesオブジェクトの親ノードを取得する。}
     * {@.en Gets the root to an object concerned Properties.}
     * 
     * @return 
     *   {@.ja 親ノードPropertiesオブジェクト}
     *   {@.en root Properties object list}
     */
    public Properties getRoot() {
        return this.root;
    }
    
    /**
     * {@.ja 指定されたキーに対応する値を取得する。}
     * {@.en acquires the value corresponding to the specified key.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @return 
     *   {@.ja 指定されたキーに対応する値を返す。
     *   指定されたキーが存在するが値が設定されていない場合は、
     *   デフォルト値を返す。
     *   また、指定されたキーが存在しない場合は空文字列を返す。}
     */
    public String getProperty(final String key) {
        
        Vector<String> keys = new Vector<String>();
        split(key, '.', keys);
        Properties node;
        if ((node = _getNode(keys, 0, this)) != null) {
            return (node.set_value) ? node.value : node.default_value;
        }
        
        return Properties.EMPTY;
    }
    
    /**
     * {@.ja 指定されたキーに対応する値を取得する。}
     * {@.en Gets the value corresponding to the specified key.}
     * <p>
     * {@.ja ただし、指定されたキーが存在しない場合や、
     * キーに対応する値が空文字列である場合は、指定された代替値が取得される。}
     * {@.en When the specified key doesn't exist or the value corresponding to
     * the key is a null character string, the specified alternative value is
     * gotten.} 
     * 
     * @param key i
     *   {@.ja キー}
     *   {@.en key}
     * @param alternative 
     *   {@.ja 代替値}
     *   {@.en Alternative value}
     * @return 
     *   {@.ja 指定されたキーに対応する値を返す。
     *         指定されたキーが存在しない場合や、
     *         キーに対応する値が空文字列である場合は、代替値を返す。}
     *   {@.en Returns the value corresponding to the specified key.
     *         When the specified key doesn't exist, and the value
     *         corresponding to the key is a null character string, an
     *         alternative value is returned. }
     */
    public String getProperty(final String key, final String alternative) {
        
        String value = getProperty(key);
        
        return (value.length() == 0) ? alternative : value;
    }
    
    /**
     * {@.ja 指定されたキーに対応するデフォルト値を取得する。}
     * {@.en Gets the default value corresponding to the specified key.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @return 
     *   {@.ja 指定されたキーが存在する場合は、
     *   それに対応するデフォルト値を返す。
     *   指定されたキーが存在しない場合は、空文字列を返す。}
     *   {@.en When the specified key exists, the default value corresponding
     *   to it is returned.  When the specified key doesn't exist, the null
     *   character string is returned.}
     */
    public String getDefault(final String key) {
        
        Vector<String> keys = new Vector<String>();
        split(key, '.', keys);
        Properties node;
        if ((node = _getNode(keys, 0, this)) != null) {
            return node.default_value;
        }
        
        return Properties.EMPTY;
    }
    
    /**
     * {@.ja 指定されたキーに対応する値を登録する。}
     * {@.en Registers the value corresponding to the specified key.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @param value 
     *   {@.ja キーに対応する値}
     *   {@.en value}
     * @return 
     *   {@.ja 以前の設定値を返す。
     *   以前の設定値が存在しない場合は、デフォルト値を返す。}
     *   {@.en A set value is returned. When a set value doesn't exist, the
     *   default value is returned.}
     */
    public String setProperty(final String key, final String value) {
        
        Vector<String> keys = new Vector<String>();
        split(key, '.', keys);
        
        Properties curr = this;
        for (int i = 0; i < keys.size(); ++i) {
            Properties next = curr.hasKey(keys.get(i));
            if (next == null) {
                next = new Properties(keys.get(i));
                next.root = curr;
                curr.leaf.add(next);
            }
            curr = next;
        }

        String oldValue = (curr.value.length() > 0) ? curr.value : curr.default_value;
        curr.value = value;
        curr.set_value = true;
        
        return oldValue;
    }
    
    /**
     * {@.ja 指定されたキーに対応するデフォルト値を設定する。}
     * {@.en sets the default value corresponding to the specified key.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @param defaultValue 
     *   {@.ja キーに対応する新たなデフォルト値}
     *   {@.en Default value}
     */
    public String setDefault(final String key, final String defaultValue) {
        
        Vector<String> keys = new Vector<String>();
        split(key, '.', keys);
        
        Properties curr = this;
        for (int i = 0; i < keys.size(); ++i) {
            Properties next = curr.hasKey(keys.get(i));
            if (next == null) {
                next = new Properties(keys.get(i));
                next.root = curr;
                curr.leaf.add(next);
            }
            curr = next;
        }
        curr.default_value = defaultValue;
        
        return defaultValue;
    }
    
    /**
     * {@.ja 指定されたキーに対応するデフォルト値を設定する。}
     * {@.en Sets the default value corresponding to the specified key.}
     * 
     * @param defaults 
     *   {@.ja デフォルト値を、キー・値の順に交互に並べたもの}
     *   {@.en Default value in which key and value are alternately displayed}
     */
    public void setDefaults(final String[] defaults) {
		
	for (int i = 0; i + 1 < defaults.length; i += 2) {
	    String key = defaults[i];
	    String value = defaults[i + 1];
			
	    key = key.trim();
	    value = value.trim();
			
	    setDefault(key, value);
	}
    }

    /**
     * {@.ja 指定された出力ストリームに、
     * 当該Propertiesオブジェクトの内容を出力する。}
     * {@.en outputs the content of a Properties object to the specified output
     * stream. }
     * 
     * @param out 
     *   {@.ja 出力先ストリーム}
     *   {@.en output stream}
     */
    public void list(OutputStream out) {
        _store(out, "", this);
    }
    
    /**
     * {@.ja 指定されたリーダーから、
     * キーと値が対となったプロパティセットを読み込む。}
     * {@.en Reads the property set from the specified buffer.}
     * 
     * @param reader 
     *   {@.ja 読み込み元となるリーダー。}
     *   {@.en buffer}
     */
    public void load(BufferedReader reader) throws IOException {
        
        String pline = ""; 
        
        String tmp;
        while ((tmp = reader.readLine()) != null) {
            tmp = tmp.trim();
            
            // Skip comments or empty lines
            if (tmp.length() == 0 || tmp.charAt(0) == '#' || tmp.charAt(0) == '!') continue;
            
            // line-end '\' continues entry
            if (tmp.charAt(tmp.length() - 1) == '\\' && !StringUtil.isEscaped(tmp, tmp.length() - 1))
		{
		    tmp = tmp.substring(0, tmp.length() - 1);
		    pline += tmp;
		    continue;
		}
            pline += tmp;
            
            // Skip empty line (made of only ' ' or '\t')
            if (pline.length() == 0) continue;
    
            Pair<String, String> pair = splitKeyValue(pline);
            String key = pair.getKey();
            String value = pair.getValue();
            
            key = StringUtil.unescape(key);
            key = key.trim();

            value = StringUtil.unescape(value);
            value = value.trim();
    
            setProperty(key, value);
            pline = "";
        }
    }
    
    /**
     * {@.ja 当該Propertiesオブジェクトの内容を、
     * 指定されたストリームに出力する。}
     * {@.en Outputs the content of a Properties to the specified stream.}
     * 
     * @param out 
     *   {@.ja 出力先ストリーム}
     *   {@.en output stream}
     * @param header 
     *   {@.ja 出力内容の先頭に付加されるヘッダコメント文字列}
     *   {@.en String added to head of content of output}
     */
    public void save(OutputStream out, final String header) {
        store(out, header);
    }
    
    /**
     * {@.ja 当該Propertiesオブジェクトの内容を、
     * 指定されたストリームに出力する。}
     * {@.en Outputs the content of a Properties to the specified stream.}
     * 
     * @param out 
     *   {@.ja 出力先ストリーム}
     *   {@.en output stream}
     * @param header 
     *   {@.ja 出力内容の先頭に付加されるヘッダコメント文字列}
     *   {@.en String added to head of content of output}
     */
    public void store(OutputStream out, final String header) {

        PrintWriter writer = new PrintWriter(out);
        writer.write("# ");
        writer.write(header);
        writer.println();
        writer.flush();
        
        _store(out, "", this);
    }
    
    /**
     * {@.ja 当該Propertiesオブジェクトおよび子ノード内に存在するキーの
     * リストを取得する。}
     * {@.en acquires the list of the key that exists among the Properties
     * object and the child nodes.}
     * 
     * @return
     *   {@.ja キーのリスト}
     *   {@.en list of key}
     */
    public final Vector<String> propertyNames() {
        
        Vector<String> names = new Vector<String>();
        for (int i = 0; i < leaf.size(); ++i) {
            _propertyNames(names, leaf.get(i).name, leaf.get(i));
        }
        
        return names;
    }
    
    /**
     * {@.ja 当該Propertiesオブジェクトおよび子ノード内に存在するプロパティの
     * 数を取得する。}
     * {@.en acquires the number of properties that exist among the Properties
     * object and the child nodes.} 
     * 
     * @return 
     *   {@.ja プロパティ数}
     *   {@.en the number of properties}
     */
    public final int size() {
        return propertyNames().size();
    }
    
    /**
     * {@.ja ノードを検索する}
     * {@.en Finds node.}
     *
     * @param key
     *   {@.ja キー}
     *   {@.en key}
     *
     * @return
     *   {@.ja Properties}
     *   {@.en Properties}
     */
    public final Properties findNode(final String key) {
	if (key.length() == 0) { return null; }
	Vector<String> keys = new Vector<String>();
	split(key, '.', keys);
	return _getNode(keys, 0, this);
    }

    /**
     * {@.ja 指定されたキーに対応する値を直接保持しているProperties
     * オブジェクトを取得する}
     * {@.en acquires the Properties object that maintains the value
     * corresponding to the specified key.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @return 
     *   {@.ja 指定されたキーに対応する値を直接保持しているProperties
     *   オブジェクト}
     *   {@.en Properties object that maintains value corresponding to
     *   specified key}
     */
    public final Properties getNode(final String key) {
	if (key.length() == 0) { return this; }
	Properties leaf = findNode(key);
	if (leaf != null) {
	    return leaf;
	}
	this.createNode(key);
        return findNode(key);
    }
    
    /**
     * {@.ja 指定されたキーのPropertiesオブジェクトを、
     * 当該Propertiesオブジェクト内に子ノードとして作成する。}
     * {@.en makes the child node the Properties object of the specified key.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @return 
     *   {@.ja 指定されたキーがすでに存在していた場合はfalseを、
     *   さもなくばtrueを返す。}
     *   {@.en When the specified key has already existed, false is returned.}
     */
    public boolean createNode(final String key) {
	if (key.length() == 0) { return false; }

	if (findNode(key) != null) {
	    return false;
	}
        this.setProperty(key, "");
        
        return true;
    }
    
    /**
     * {@.ja 指定されたキーの子ノードPropertiesオブジェクトを、
     * 当該Propertiesオブジェクトから切り離す。}
     * {@.en Separates child node Properties of the specified key from
     * concerned Properties.}
     * 
     * @param name 
     *   {@.ja 切り離したい子ノードのキー}
     *   {@.en Key to child node that separates}
     * @return 
     *   {@.ja 切り離された子ノードPropertiesオブジェクトを返す。
     *   指定されたキーに対応する子ノードが存在しない場合は、nullを返す。}
     *   {@.en The separated child node Properties object is returned.}
     */
    public Properties removeNode(String name) {
        
        Properties prop;
        for (int i = 0; i < this.leaf.size(); ++i) {
            if (this.leaf.get(i).name.equals(name)) {
                prop = this.leaf.get(i);
                this.leaf.remove(i);
                return prop;
            }
        }
        
        return null;
    }
    
    /**
     * {@.ja 当該Propertiesオブジェクトの
     * 直接の子ノード（つまり、孫ノード以下は含まない）の中に、
     * 指定されたキーを持つものが存在するかどうか調べる。存在する場合には、
     * そのPropertiesオブジェクトを取得する。}
     * {@.en examines whether something with the specified key exists in the
     * child node of the Properties object.}
     * 
     * @param key 
     *   {@.ja キー}
     *   {@.en key}
     * @return 
     *   {@.ja 指定されたキーに対応するPropertiesオブジェクトを返す。
     *   指定されたキーに対応するPropertiesオブジェクトが存在しない場合は、
     *   nullを返す。}
     *   {@.en The Properties object corresponding to the specified key is
     *   returned.}
     */
    public final Properties hasKey(final String key) {
        
        for (int i = 0; i < this.leaf.size(); ++i) {
            if (this.leaf.get(i).name.equals(key)) {
                return this.leaf.get(i);
            }
        }
        
        return null;
    }
    
    /**
     * {@.ja 子ノードを全て削除する。}
     * {@.en Deletes all his child nodes.}
     */
    public void clear() {
        
        while (true) {
            if(this.leaf.size() <= 0) {
                break;
            }
            Properties tail = this.leaf.lastElement();
            if (tail != null) {
                tail.destruct();
            }
        }
    }
    
    /**
     * {@.ja 指定されたPropertiesオブジェクト内のプロパティセットを、
     * 当該Propertiesオブジェクト内にマージする。}
     * {@.en merges the specified Properties object with an object concerned
     * Properties.}
     * <p>
     * {@.ja ただし、デフォルト値はマージされない。}
     * 
     * @param prop 
     *   {@.ja マージ元となるPropertiesオブジェクト}
     *   {@.en Properties object in merging origin}
     */
    public void merge(final Properties prop) {
        
        for (Iterator<String> it = prop.propertyNames().iterator(); it.hasNext(); ) {
            String key = it.next();
            String value = prop.getProperty(key);
            this.setProperty(key, value);
        }
    }
    
    /**
     * {@.ja 指定された文字列を、
     * 所定のデリミタによってキーと値のペアに分離する。}
     * 
     * @param str 
     *   {@.ja 分離対象となる文字列。
     *   通常は、キーと値がデリミタで区切られている文字列。}
     * @return 
     *   {@.ja キーと値を格納するPairオブジェクト。}
     */
    protected Pair<String, String> splitKeyValue(final String str) {
        
        for (int i = 0; i < str.length(); ++i) {
            if ((str.charAt(i) == ':' || str.charAt(i) == '=')
		&& !StringUtil.isEscaped(str, i)) {
                return new Pair<String, String>(str.substring(0, i).trim(), str.substring(i + 1).trim());
            }
        }
        
        for (int i = 0; i < str.length(); ++i) {
            if ((str.charAt(i) == ' ') && StringUtil.isEscaped(str, i)) {
                return new Pair<String, String>(str.substring(0, i).trim(), str.substring(i + 1).trim());
            }
        }

        return new Pair<String, String>(str, "");
    }
    
    /**
     * {@.ja 指定された文字列を指定したデリミタで分離した結果を取得する。}
     * 
     * @param str 
     *   {@.ja 分離対象となる文字列}
     * @param delim 
     *   {@.ja デリミタ}
     * @return 
     *   {@.ja 分離された文字列のリスト}
     */
    protected boolean split(final String str, final char delim, Vector<String> values) {
        
        if (str.length() == 0) {
            return false;
        }
        
        int begin_idx = 0;
        int end_idx = 0;
        
        while (end_idx < str.length()) {
            if ((str.charAt(end_idx) == delim) && !StringUtil.isEscaped(str, end_idx)) {
                values.add(str.substring(begin_idx, end_idx));
                begin_idx = end_idx + 1;
            }
            end_idx++;
        }
        values.add(str.substring(begin_idx));
        
        return true;
    }
    
    protected String indent(int index) {
        StringBuffer spaces = new StringBuffer();
        for (int i = 0; i < index - 1; ++i) {
            spaces.append("  ");
        }
        
        return spaces.toString();
    }
    
    protected Properties _getNode(Vector<String> keys, int index, final Properties curr) {
        Properties next = curr.hasKey(keys.get(index));
        
        if (next == null) {
            return null;
        }
        
        if (index < keys.size() - 1) {
            return next._getNode(keys, ++index, next);
        }
        
        return next;
    }
    
    protected void _propertyNames(Vector<String> names, String curr_name, final Properties curr) {
        
        if (!curr.leaf.isEmpty()) {
            for (int i = 0; i < curr.leaf.size(); ++i) {
                String next_name = curr_name + '.' + curr.leaf.get(i).name;
                _propertyNames(names, next_name, curr.leaf.get(i));
            }
        } else {
            names.add(curr_name);
        }
    }
    
    protected void _store(OutputStream out, String curr_name, Properties curr) {
        
        if (!curr.leaf.isEmpty()) {
            for (int i = 0; i < curr.leaf.size(); ++i) {
                String next_name;
                if (curr_name.length() == 0) {
                    next_name = curr.leaf.get(i).name;
                } else {
                    next_name = curr_name + '.' + curr.leaf.get(i).name;
                }
                _store(out, next_name, curr.leaf.get(i));
            }
        }
        
        if (curr.root != null) {
            if (curr.set_value && (curr.value != null)) {
                PrintWriter writer = new PrintWriter(out);
                writer.write(curr_name);
                writer.write(" = ");
                writer.write(curr.value);
                writer.println();
                writer.flush();
            }
        }
    }
    
    protected OutputStream _dump(OutputStream out, final Properties curr, int index) {
        
        PrintWriter writer = new PrintWriter(out);
        
        if (index != 0) {
            writer.write(indent(index));
            writer.write("- ");
            writer.write(curr.name);
        }
        
        if (!curr.set_value) {
            if (curr.value.length() == 0) {
                writer.write(": ");
                writer.write(curr.default_value);
                writer.println();
            } else {
                writer.write(": ");
                writer.write(curr.value);
                writer.println();
            }
            return out;
        }

        if (index != 0) {
            writer.println();
        }
        
        for (int i = 0; i < curr.leaf.size(); ++i) {
            _dump(out, curr.leaf.get(i), index + 1);
        }
        
        return out;
    }
    /**
     * {@.ja Propertiesの内容を文字列にする。}
     * {@.en makes the content of Properties a character string.}
     * 
     * @param out 
     *   {@.ja 出力用文字列}
     *   {@.en String for output}
     * @param curr 
     *   {@.ja Properties}
     *   {@.en Properties}
     * @param index
     *   {@.ja 階層}
     *   {@.en Hierarchy}
     */
    public String _dump(String out, final Properties curr, int index) {
        
        String crlf = System.getProperty("line.separator");
        if (index != 0) {
            out = out +indent(index) + "- " + curr.name;
        }
        
        if (!curr.set_value) {
            if (curr.value.length() == 0) {
                out = out + ": " + curr.default_value + crlf;
            } else {
                out = out + ": " + curr.value + crlf;
            }
            return out;
        }

        if (index != 0) {
            out = out + crlf;
        }
        
        for (int i = 0; i < curr.leaf.size(); ++i) {
            out = _dump(out, curr.leaf.get(i), index + 1);
        }
        
        return out;
    }
}
