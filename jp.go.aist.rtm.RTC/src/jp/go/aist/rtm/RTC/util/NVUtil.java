package jp.go.aist.rtm.RTC.util;

import java.util.Vector;
import java.lang.reflect.Method;

import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;

import _SDOPackage.NVListHolder;
import _SDOPackage.NameValue;


/**
 * {@.ja NameValue操作用ユーティリティクラス}
 * {@.en Utility for NameValue}
 *
 */
public class NVUtil {

    /**
     * {@.ja 指定されたデータを用いてNameValueオブジェクトを作成する}
     * {@.en Create NameValue}
     * 
     * @param name 
     *   {@.ja オブジェクトに設定する名称}
     *   {@.en Name of NameValue}
     * @param value 
     *   {@.ja オブジェクトに設定する値}
     *   {@.en The value of NameValue}
     * @param klass 
     *   {@.ja オブジェクトに設定する値の型}
     *   {@.en ClassType}
     * 
     * @return 
     *   {@.ja 作成されたNameValueオブジェクト}
     *   {@.en NameValue}
     */
    public static <T> NameValue newNV(final String name, T value, Class<T> klass) {
        
        TypeCast<T> cast = new TypeCast<T>(klass);

        NameValue nv = new NameValue();
        nv.name = name;
        nv.value = cast.castAny(value);
        return nv;
    }
    /**
     * {@.ja 指定されたデータを用いてNameValueオブジェクトを作成する}
     * {@.en Create NameValue typed String}
     * 
     * @param name 
     *   {@.ja オブジェクトに設定する名称}
     *   {@.en Name of NameValue}
     * @param value 
     *   {@.ja オブジェクトに設定する値(String)}
     *   {@.en The value of NameValue}
     * 
     * @return 
     *   {@.ja 作成されたNameValueオブジェクト}
     *   {@.en NameValue}
     */
    public static NameValue newNVString(final String name, String value) {
        NameValue nv = new NameValue();
        nv.name = name;
        Any any = ORBUtil.getOrb().create_any();
        any.insert_string(value);
        nv.value = any;
        return nv;
    }
    
    /**
     * {@.ja 指定されたデータを用いてNameValueオブジェクトを作成する}
     * {@.en Create NameValue}
     * 
     * @param name 
     *   {@.ja オブジェクトに設定する名称}
     *   {@.en Name of NameValue}
     * @param value 
     *   {@.ja オブジェクトに設定するString値}
     *   {@.en The value of NameValue}
     * 
     * @return 
     *   {@.ja 作成されたNameValueオブジェクト}
     *   {@.en NameValue}
     */
    public static NameValue newNV(final String name, String value) {
        
        return newNV(name, value, String.class);
    }
    
    /**
     * {@.ja 指定されたデータを用いてNameValueオブジェクトを作成する}
     * {@.en Create NameValue typed CORBA::Any}
     * 
     * @param name 
     *   {@.ja オブジェクトに設定する名称}
     *   {@.en Name of NameValue}
     * @param value 
     *   {@.ja オブジェクトに設定するAny値}
     *   {@.en The value of NameValue}
     *
     * 
     * @return 
     *   {@.ja 作成されたNameValueオブジェクト}
     *   {@.en NameValue}
     */
    public static NameValue newNVAny(final String name, final Any value) {
        
        NameValue nv = new NameValue();
        nv.name = name;
        nv.value = value;
        return nv;
    }

    /**
     * {@.ja 指定されたPropertiesを用いてNameValueオブジェクトリストを作成し、
     * それを指定されたNVListHolder内にコピーする}
     * {@.en Copy the properties to NVList}
     * <p>
     * {@.ja Propertiesのキーおよび値が、
     * 各NameValueの名称および値にそれぞれ割り当てらる}
     * {@.en This operation copies the properties to NVList.
     * All NVList's values are copied as CORBA::string.}
     * 
     * @param nvlist 
     *   {@.ja 作成されたNameValueオブジェクトリストを受け取るためNVListHolder}
     *   {@.en NVList to store properties values}
     * @param prop 
     *   {@.ja 設定元となるPropertiesオブジェクト}
     *   {@.en Properties that is copies from}
     */
    public static void copyFromProperties(NVListHolder nvlist, 
                                              final Properties prop) {
        
        Vector keys = prop.propertyNames();
        int len = keys.size();
        nvlist.value = new NameValue[len];
        for (int intIdx = 0; intIdx < len; ++intIdx) {
            if (nvlist.value[intIdx] == null) {
                nvlist.value[intIdx] = new NameValue();
            }
            nvlist.value[intIdx].name = (String) keys.elementAt(intIdx);
            Any anyValue = ORBUtil.getOrb().create_any();
            anyValue.insert_string(prop.getProperty((String) keys.elementAt(intIdx)));
//
//            String half_full = prop.getProperty((String) keys.elementAt(intIdx));
//            if(half_full.getBytes().length == half_full.length()){
//                anyValue.insert_string(half_full);
//            }
//            else{
//                anyValue.insert_wstring(half_full);
//            } 
//
            nvlist.value[intIdx].value = anyValue;
        }
    }
    
    /**
     * {@.ja 指定されたNVListHolderに保持されている
     * NameValueオブジェクトリストの内容を、指定されたPropertiesオブジェクトに
     * コピーする}
     * {@.en Copy NVList to the Proeprties}
     * <p>
     * {@.ja 各NameValueの名称および値が、
     * Propertiesのキーおよび値にそれぞれ割り当てられる}
     * {@.en This operation copies NVList to properties.}
     * 
     * @param prop 
     *   {@.ja コピー先のPropertiesオブジェクト}
     *   {@.en Properties to store NVList values}
     * @param nvlist 
     *   {@.ja コピー元のNameValueオブジェクトリストを
     *   内包するNVListHolderオブジェクト}
     *   {@.en NVList of copy source}
     */
    public static void copyToProperties(Properties prop, final NVListHolder nvlist) {

        for (int intIdx = 0; intIdx < nvlist.value.length; ++intIdx) {
            try {
                Any anyVal = nvlist.value[intIdx].value;
                String value = null;
                if( anyVal.type().kind() == TCKind.tk_objref ) {
		    org.omg.CORBA.Object obj = anyVal.extract_Object();
		    if (obj != null) {
			value = obj.toString();
		    }
		    else {
			value = "";
		    }
			
                } else if( anyVal.type().kind() == TCKind.tk_boolean ) {
                    value =  anyVal.extract_boolean() ? "true" : "false";
                } else if( anyVal.type().kind() == TCKind.tk_octet ) {
                    value =  Byte.toString(anyVal.extract_octet());
                } else if( anyVal.type().kind() == TCKind.tk_char ) {
                    value =  Character.toString(anyVal.extract_char());
                } else if( anyVal.type().kind() == TCKind.tk_wchar ) {
                    value =  Character.toString(anyVal.extract_wchar());
                } else if( anyVal.type().kind() == TCKind.tk_string ) {
                    value = anyVal.extract_string();
                } else if( anyVal.type().kind() == TCKind.tk_wstring ) {
                    value = anyVal.extract_wstring();
                } else if( anyVal.type().kind() == TCKind.tk_short ) {
                    value =  Integer.toString(anyVal.extract_short());
                } else if( anyVal.type().kind() == TCKind.tk_ushort ) {
                    value =  Integer.toString(anyVal.extract_ushort());
                } else if( anyVal.type().kind() == TCKind.tk_long ) {
                    value =  Integer.toString(anyVal.extract_long());
                } else if( anyVal.type().kind() == TCKind.tk_ulong ) {
                    value =  Integer.toString(anyVal.extract_ulong());
                } else if( anyVal.type().kind() == TCKind.tk_longlong ) {
                    value =  Long.toString(anyVal.extract_longlong());
                } else if( anyVal.type().kind() == TCKind.tk_ulonglong ) {
                    value =  Long.toString(anyVal.extract_ulonglong());
                } else if( anyVal.type().kind() == TCKind.tk_fixed ) {
                    value = anyVal.extract_fixed().toString();
                } else if( anyVal.type().kind() == TCKind.tk_float ) {
                    value =  Float.toString(anyVal.extract_float());
                } else if( anyVal.type().kind() == TCKind.tk_double ) {
                    value =  Double.toString(anyVal.extract_double());
                } else if( anyVal.type().kind() == TCKind.tk_struct ) {

                    if(anyVal.type().name().equals("TimedBoolean")) {
                        value = RTC.TimedBooleanHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedOctet")) {
                        value = RTC.TimedOctetHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedChar")) {
                        value = RTC.TimedCharHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedWChar")) {
                        value = RTC.TimedCharHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedShort")) {
                        value = RTC.TimedShortHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedUShort")) {
                        value = RTC.TimedShortHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedLong")) {
                        value = RTC.TimedLongHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedULong")) {
                        value = RTC.TimedLongHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedFloat")) {
                        value = RTC.TimedFloatHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedDouble")) {
                        value = RTC.TimedDoubleHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedString")) {
                        value = RTC.TimedStringHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedWString")) {
                        value = RTC.TimedStringHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedBooleanSeq")) {
                        value = RTC.TimedBooleanSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedOctetSeq")) {
                        value = RTC.TimedOctetSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedCharSeq")) {
                        value = RTC.TimedCharSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedWCharSeq")) {
                        value = RTC.TimedCharSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedStringSeq")) {
                        value = RTC.TimedStringSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedWStringSeq")) {
                        value = RTC.TimedWStringSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedShortSeq")) {
                        value = RTC.TimedShortSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedUShortSeq")) {
                        value = RTC.TimedUShortSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedLongSeq")) {
                        value = RTC.TimedLongSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedULongSeq")) {
                        value = RTC.TimedULongSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedFloatSeq")) {
                        value = RTC.TimedFloatSeqHelper.extract(anyVal).toString();
                    }
                    else if(anyVal.type().name().equals("TimedDoubleSeq")) {
                        value = RTC.TimedDoubleSeqHelper.extract(anyVal).toString();
                    }
                    else {
                        try {
                            String className = anyVal.type().name();
                            Class helper = Class.forName(className + "Helper", 
                                           true, 
                                           anyVal.getClass().getClassLoader()); 
                            Method method = helper.getMethod("extract",
                                                   org.omg.CORBA.Any.class);
                            Object targetObject = method.invoke(
                                null, // invoke static method.
                                anyVal);
                            value = targetObject.toString();
                        } catch ( Exception ex) {
                            throw new ClassCastException("Unknown data type.");
                        }
                    }
		} else {
                    Class cl = anyVal.getClass();
                    String str = cl.getName();
		    value = anyVal.extract_Value().toString();
                }
                final String name = nvlist.value[intIdx].name;
                prop.setProperty(name, value);
                
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }
    
    /**
     * {@.ja 指定されたNVListHolderに保持されているNameValueオブジェクトリスト
     * の内容をもとに、Propertiesオブジェクトを作成する}
     * {@.en Transform NVList to the properties}
     * <p>
     * {@.ja 各NameValueの名称および値が、Propertiesのキーおよび値に
     * それぞれ割り当てられる}
     * {@.en This operation transforms NVList to properties}
     * 
     * @param nvlist 
     *   {@.ja 元となるNameValueオブジェクトリストを
     *   内包するNVListHolderオブジェクト}
     *   {@.en NVList of tranformation source}
     *
     * @return 
     *   {@.ja 作成されたPropertiesオブジェクト}
     *   {@.en Transformation result Property}
     */
    public static Properties toProperties(final NVListHolder nvlist) {
        to_prop prop = new to_prop();
        prop = (to_prop) CORBA_SeqUtil.for_each(nvlist, prop);
        if(prop != null) {
            return prop.m_prop;
        }
        return new Properties();
    }

    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * 指定した名称を持つNameValueオブジェクトを検索して、
     * そのオブジェクトが持つ値をAny型で取得する}
     * {@.en Return the value specified by name from NVList}
     * 
     * <p>
     * {@.en This operation returns Any type of value specified by name.
     * When an element of specified name doesn't exist, the exception will 
     * occur.}
     *
     * @param nvlist 
     *   {@.ja 検索対象となるNameValueオブジェクトリストを内包するNVListHolder
     *   オブジェクト}
     *   {@.en The target NVList for the find}
     * @param name 
     *   {@.ja 検索したいNameValueオブジェクトの名称}
     *   {@.en Name for the find}
     * 
     * @return 
     *   {@.ja 指定した名称を持つNameValueオブジェクトのAny値}
     *   {@.en Find result}
     * 
     * @throws Exception 
     *   {@.ja 指定した名称のNameValueオブジェクトが見つからない場合}
     *   {@.en Not found NameValue object}
     */
    public final static Any find(final NVListHolder nvlist, final String name)
            throws Exception {

        int index = CORBA_SeqUtil.find(nvlist, new nv_find(name));
        if (index < 0) {
            throw new Exception("Not found");
        }

        return nvlist.value[index].value;
    }

    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * 指定した名称を持つNameValueオブジェクトを検索して、
     * そのオブジェクトのリスト内でのインデクスを取得する}
     * {@.en Return the index of element specified by name from NVList}
     * <p> 
     * {@.en This operation returns the index at the position where the element
     * specified by name is stored.}
     *
     * @param nvlist 
     *   {@.ja 検索対象となるNameValueオブジェクトリストを
     *   内包するNVListHolderオブジェクト}
     *   {@.en The target NVList for the find}
     * @param name 
     *   {@.ja 検索したいNameValueオブジェクトの名称}
     *   {@.en Name for the find}
     *
     * 
     * @return 
     *   {@.ja 指定した名称を持つNameValueオブジェクトが見つかった場合は、
     *   そのオブジェクトのリスト内でのインデクスを返す。
     *   また、見つからない場合は、-1を返す。}
     *   {@.en Index of target object for the find}
     */
    public final static int find_index(final NVListHolder nvlist, final String name) {
        return CORBA_SeqUtil.find(nvlist, new nv_find(name));
    }
    
    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * 指定した名称を持つNameValueオブジェクトを検索して、
     * そのオブジェクトが持つ値が文字列型かどうかを調べる}
     * {@.en Validate whether value type specified by name is string type}
     * <p>
     * {@.en This operation returns the bool value by checking whether 
     * the type of value specified with name is CORBA::string.}
     * 
     * @param nvlist 
     *   {@.ja 検索対象となるNameValueオブジェクトリストを
     *   内包するNVListHolderオブジェクト}
     *   {@.en @param nv The target NVList for the search}
     * @param name 
     *   {@.ja 検索したいNameValueオブジェクトの名称}
     *   {@.en Name for the search}
     * 
     * @return 
     *   {@.ja 見つかったNameValueオブジェクトの値が文字列型の場合はtrueを、
     *   そうでなければfalseを返す}
     *   {@.en String validation result (String:true, Else:false)}
     */
    public static boolean isString(final NVListHolder nvlist, final String name) {
        try {
            Any value = find(nvlist, name);
            if( value.type().kind() == TCKind.tk_wstring ) {
                value.extract_wstring();
            } else {
                value.extract_string();
            }
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * 指定した名称を持つNameValueオブジェクトを検索して、
     * そのオブジェクトが持つ値が文字列型かどうかを調べる}
     * {@.en Check whether the value of specified name matches the specified
     *        string}
     *
     * <p>
     * {@.ja 文字列型の場合は、さらに指定された文字列値に一致するかどうかを
     * 調べる}
     * {@.en This operation checks whether the value specified with name is 
     * CORBA::string and returns the bool value which matches spcified string.}
     * 
     * @param nvlist 
     *   {@.ja 検索対象となるNameValueオブジェクトリストを
     *   内包するNVListHolderオブジェクト}
     *   {@.en The target NVList for the search}
     * @param name 
     *   {@.ja 検索したいNameValueオブジェクトの名称}
     *   {@.en Name for the search}
     * 
     * @param value 
     *   {@.ja 比較する文字}
     *   {@.en String value to compare}
     *
     * @return 
     *   {@.ja 見つかったNameValueオブジェクトの値が文字列型で、
     *   かつ指定された文字列値に一致する場合はtrueを返す。
     *   また、そうでなければfalseを返す。}
     *   {@.en Check result (Match:true, Unmatch:false)}
     */
    public static boolean isStringValue(final NVListHolder nvlist,
            final String name, final String value) {
        
        if (isString(nvlist, name)) {
            if (toString(nvlist, name).equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * 指定した名称を持つNameValueオブジェクトを検索して、
     * そのオブジェクトが持つ値を文字列型で取得する}
     * {@.en Get NVList of specifid name as string}
     * <p>
     * {@.en This operation returns string value in NVList specified by name.
     * If the value in NVList specified by name is not CORBA::string type
     * this operation returns empty string value.}
     *
     * @param nvlist 
     *   {@.ja 検索対象となるNameValueオブジェクトリストを内包するNVListHolder
     *   オブジェクト}
     *   {@.en The target NVList for the search}
     * 
     * @param name 
     *   {@.ja 検索したいNameValueオブジェクトの名称}
     *   {@.en Name for the search}
     *
     * 
     * @return 
     *   {@.ja 指定した名称を持つNameValueオブジェクトの文字列値を返す。
     *         ただし、指定した名称を持つNameValueオブジェクトが
     *         見つからない場合や、NameValueオブジェクトの値が
     *         文字列型でない場合は、空文字列を返す。}
     *   {@.en String value corresponding to name}
     * 
     */
    public static String toString(final NVListHolder nvlist, final String name) {
        String str_value;
        try {
            Any value = find(nvlist, name);
            if( value.type().kind() == TCKind.tk_wstring ) {
                str_value = value.extract_wstring();
            } else {
                str_value = value.extract_string();
            }
        } catch (Exception e) {
            str_value = "";
        }
        if( str_value == null ) {
            return "";
        }
        return str_value;
    }
    
    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * 指定した名称を持つNameValueオブジェクトを検索して、
     * そのオブジェクトに指定した文字列値を追加する。}
     * {@.en Append the specified string to element of NVList}
     *
     * <p>
     * {@.ja 指定した名称を持つNameValueオブジェクトが存在しない場合には、
     * その名称と指定された値を持つNameValueが新たに作成され、
     * NVListHolder内のNameValueオブジェクトリストに追加される。}
     * {@.en This operation appends the string value specified by 
     * value to the elementspecified by name.
     * Operate nothing when the 'value' value has already been set to the
     * element specified by name.
     * Add the 'value' value each separating by a comma "," when the 'value'
     * value is not set to the element specified by name.
     * Set the specified value.
     * Add a new element at the end of NVList, and set the specified value,
     * when the element specified by name does not exist.}
     * 
     * @param nvlist 
     *   {@.ja 検索対象となるNameValueオブジェクトリストを内包するNVListHolder
     *   オブジェクト}
     *   {@.en The target NVList for the search}
     * @param name 
     *   {@.ja 検索したいNameValueオブジェクトの名称}
     *   {@.en The target element name for the appending}
     * @param value 
     *   {@.ja 見つかったNameValueオブジェクトの値に追加したい文字列値}
     *   {@.en String to append}
     *
     * @return 
     *   {@.ja trueを返す。}
     *   {@.en Append operation result}
     */
    public static boolean appendStringValue(NVListHolder nvlist,
            final String name, final String value) {
        
        int index = find_index(nvlist, name);
        boolean find = false;
        
        if (index >= 0) {
            String tmp_char = null;
            if( nvlist.value[index].value.type().kind() == TCKind.tk_wstring ) {
                tmp_char = nvlist.value[index].value.extract_wstring();
            } else {
                tmp_char = nvlist.value[index].value.extract_string();
            }
            String tmp_str = tmp_char;
            String[] newValues = value.split(",");
            String[] existentValues = tmp_str.split(",");
            for (int intIdxNew = 0; intIdxNew < newValues.length; intIdxNew++) {
                for (int intIdx = 0; intIdx < existentValues.length; intIdx++) {
                    if (existentValues[intIdx].trim().equals(newValues[intIdxNew].trim())) {
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    tmp_str = tmp_str + "," + value;
                    nvlist.value[index].value.insert_string(tmp_str);
                }
            }
        } else {
            CORBA_SeqUtil.push_back(nvlist, newNV(name, value, String.class));
        }
        
        return true;
    }
    
    /**
     * {@.ja 片方のNameValueListオブジェクトが内包するNameValue
     * オブジェクトリストを、もう一方のNameValueListオブジェクト内に追加する。}
     * {@.en Append an element to NVList}
     * <p>
     * {@.en This operation appends elements specified by src to NVList 
     * specified by dest.}
     * 
     * @param destNvList 
     *   {@.ja 追加先となるNVListHolderオブジェクト}
     *   {@.en NVList to be appended}
     * @param srcNvList 
     *   {@.ja 追加元となるNVListHolderオブジェクト}
     *   {@.en NVList to append}
     */
    public static void append(NVListHolder destNvList, final NVListHolder srcNvList) {
        for (int intIdx = 0; intIdx < srcNvList.value.length; ++intIdx) {
            CORBA_SeqUtil.push_back(destNvList, srcNvList.value[intIdx]);
        }
    }
    
    /**
     * {@.ja 指定されたNVListHolderオブジェクトが内包する
     * NameValueオブジェクトリストの内容を、標準出力に出力する。}
     * {@.en Print information configured in NVList as string type}
     * 
     * @param nvlist 
     *   {@.ja 表示対象となるNameValueオブジェクトリストを内包する
     *   NVListHolderオブジェクト}
     *   {@.en The target NVList for the print}
     */
    public static void dump(NVListHolder nvlist) {
        String str_value;
        for (int intIdx = 0; intIdx < nvlist.value.length; ++intIdx) {
            try {
                if( nvlist.value[intIdx].value.type().kind() == TCKind.tk_wstring ) {
                    str_value = nvlist.value[intIdx].value.extract_wstring();
                } else {
                    str_value = nvlist.value[intIdx].value.extract_string();
                }
                System.out.println(nvlist.value[intIdx].name + ":" + str_value);
            } catch (Exception ex) {
                System.out.println(nvlist.value[intIdx].name + ": not a string value");
            }
        }
    }

    /**
     * {@.ja 指定された名称を持つNameValueオブジェクトを
     * 検索するためのヘルパクラス。}
     * {@.en Helper class to retrieve NameValue. }
     */
    static class nv_find implements equalFunctor {

        /**
         * {@.ja コンストラクタ。}
         * {@.en Constructor}
         * <p>
         * {@.ja 検索対象となるNameValueオブジェクトの名称を指定する。}
         * 
         * @param name 
         *   {@.ja 検索対象となるNameValueオブジェクトの名称}
         *   {@.en Name to find}
         */
        public nv_find(final String name) {
            m_name = name;
        }

        /**
         * {@.ja 検索対象となる名称を持つNameValueオブジェクトか否かを判定する}
         * {@.en Judges whether to maintain the name 
         * that NameValue object retrieves.}
         * 
         * @param nv 
         *   {@.ja 判定対象のNameValueオブジェクト}
         *   {@.en NameValue of object}
         * 
         * @throws ClassCastException 
         *   {@.ja 指定されたオブジェクトがNameValueオブジェクトでない場合}
         *   {@.en Throws Exception, when the specified object is not 
         *   NameValue object. }
         */
        public boolean equalof(final Object nv) {
            return m_name.equals(((NameValue) nv).name);
        }

        private String m_name;
    }

    /**
     * {@.ja 指定されたNameValueオブジェクトの内容を元に
     * Propertiesオブジェクトを作成するヘルパクラス}
     * {@.en Helper class that makes Properties object based on content 
     * of specified NameValue object}
     */
    static class to_prop implements operatorFunc {
        
        /**
         * {@.ja 指定されたNameValueオブジェクトの内容を元に
         * Propertiesオブジェクトを作成する}
         * {@.en Makes Properties object based on the content 
         * of the specified NameValue object. }
         * <p>
         * {@.ja 作成されたPropertiesオブジェクトは、
         * m_propメンバとして取得でる。}
         * 
         * @param elem 
         *   {@.ja 作成元となるNameValueオブジェクト}
         *   {@.en NameValue object in making origin}
         * 
         * @throws ClassCastException 
         *   {@.ja 指定されたオブジェクトがNameValueオブジェクトでない場合}
         *   {@.en Throws Exception, when the specified object is not 
         *   NameValue object. }
         */
        public void operator(Object elem) {
            operator((NameValue) elem);
        }
        
        /**
         * {@.ja 指定されたNameValueオブジェクトの内容を元に
         * Propertiesオブジェクトを作成する}
         * {@.en Makes Properties object based on the content 
         * of the specified NameValue object. }
         * <p>
         * {@.ja 作成されたPropertiesオブジェクトは、
         * m_propメンバとして取得でる。}
         * 
         * @param nv 
         *   {@.ja 作成元となるNameValueオブジェクト}
         *   {@.en NameValue object in making origin}
         * 
         */
        public void operator(final NameValue nv) {
            try {
                String value = null;
                if( nv.value.type().kind() == TCKind.tk_wstring || nv.value.type().kind() == TCKind.tk_string ) {
                    if( nv.value.type().kind() == TCKind.tk_wstring ) {
                        value = nv.value.extract_wstring();
                    } else if( nv.value.type().kind() == TCKind.tk_string ) {
                        value = nv.value.extract_string();
                    }
                    m_prop.setProperty(nv.name, value);
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        
        public Properties m_prop = new Properties();
    }

    /**
     * {@.ja NVListHolderが内包するNameValueオブジェクトリストの中から、
     * そのオブジェクトが持つ値を文字列型で取得する}
     * {@.en Acquires the value of the character string of the NameValue object
     *  from NVListHolder. }
     *
     * @param nvlist
     *   {@.ja NVListHolderオブジェクト}
     *   {@.en NVListHolder object}
     * 
     * @return 
     *   {@.ja NameValueオブジェクトの文字列値を返す。}
     *   {@.ja String value of NameValue objcet}
     * 
     * @throws Exception 
     *   {@.ja 指定した名称のNameValueオブジェクトが見つからない場合}
     *   {@.en Throws Exception, when the specified object is not 
     *   NameValue object. }
     */
    public static String toString(final NVListHolder nvlist) {
        String crlf = System.getProperty("line.separator");
        if(nvlist.value==null){
            return "NVListHolder is empty."+crlf;
        }
        String str = new String();
        for (int intIdx = 0; intIdx < nvlist.value.length; ++intIdx) {
            final String name = nvlist.value[intIdx].name;
            try {
                Any anyVal = nvlist.value[intIdx].value;
                String value = null;
                if( anyVal.type().kind() == TCKind.tk_wstring ) {
                    value = anyVal.extract_wstring();
                } else if( anyVal.type().kind() == TCKind.tk_string ) {
                    value = anyVal.extract_string();
                } else if( anyVal.type().kind() == TCKind.tk_value ) {
                    value = anyVal.extract_Value().toString();
                } else {
                    value = " not a string value";
                }
                str = str + name + ":" + value + crlf;
            } catch (Exception ignored) {
                str = str + name + ": not a string value" + crlf;
            }
        }
        return str;
    }
}
