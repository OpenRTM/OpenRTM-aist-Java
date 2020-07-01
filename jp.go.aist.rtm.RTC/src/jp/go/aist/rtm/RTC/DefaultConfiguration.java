package jp.go.aist.rtm.RTC;

import jp.go.aist.rtm.Version;

/**
 * {@.ja Managerのデフォルトコンフィグレーションを定義するインタフェース}
 * {@.en Default configuration for Manager}
 */
interface DefaultConfiguration {

    /**
     * {@.ja Managerのデフォルトコンフィグレーションを定義}
     * {@.en The default configurations of Manager are defined.}
     */
    public static final String[] default_config = {
        
        "config.version",         Version.openrtm_version,
        "openrtm.name",           Version.openrtm_name,
        "openrtm.version",        Version.openrtm_version,
        "manager.instance_name",  "manager",
        "manager.name",           "manager",
        "manager.naming_formats", "%h.host_cxt/%n.mgr",
        "manager.pid",            "",
        "manager.refstring_path", "/var/log/rtcmanager.ref",
        "os.name",                "",
        "os.release",             "",
        "os.version",             "",
        "os.arch",                "",
        "os.hostname",            "",
        "logger.enable",          "YES",
        "logger.file_name",       "./rtc%p.log",
        "logger.date_format",     "%b %d %H:%M:%S.%Q",
        "logger.log_level",       "INFO",
        "logger.stream_lock",     "NO",
        "logger.master_logger",   "",
        "module.conf_path",       "",
        "module.load_path",       "",
        "naming.enable",          "YES",
        "naming.type",            "corba",
        "naming.formats",         "%h.host_cxt/%n.rtc",
        "naming.update.enable",   "YES",
        "naming.update.interval", "10.0",
        "timer.enable",           "YES",
        "timer.tick",             "0.1",
        "corba.args",             "",
        "corba.endpoint",         "", // hostname:port_number
        "corba.id",               Version.corba_name,
        "corba.nameservers",      "localhost",
        "corba.master_manager",   "localhost:2810",
        "corba.nameservice.replace_endpoint", "NO",
        "corba.update_master_manager.enable", "YES",
        "corba.update_master_manager.interval", "10.0",
        "exec_cxt.periodic.type", "jp.go.aist.rtm.RTC.executionContext.PeriodicExecutionContext",
        "exec_cxt.periodic.rate", "1000",
        "exec_cxt.sync_transition",              "YES",
        "exec_cxt.transition_timeout",           "0.5",
        //"exec_cxt.evdriven.type", "jp.go.aist.rtm.RTC.executionContext.EventDrivenExecutionContext",
        "manager.modules.load_path",        "./",
        "manager.modules.abs_path_allowed", "YES",
        "manager.is_master",                "NO",
        "manager.corba_servant",            "YES",
        "manager.shutdown_on_nortcs",          "YES",
        "manager.shutdown_auto",            "YES",
        "manager.auto_shutdown_duration",        "10.0",
        "manager.termination_waittime",          "1.0",
        "manager.name",                     "manager",
        "manager.command",                  "rtcd",
        "manager.nameservers",              "default",
        "manager.language",                 "Java",
        "manager.components.naming_policy", "process_unique",
        "manager.modules.C++.manager_cmd", "rtcd",
        "manager.modules.Python.manager_cmd", "rtcd_python",
        "manager.modules.Python3.manager_cmd", "rtcd_python3",
        "manager.modules.Java.manager_cmd", "rtcd_java",
        "sdo.service.provider.enabled_services",  "ALL",
        "sdo.service.consumer.enabled_services",  "ALL",
        "manager.local_service.enabled_services","ALL",
        "manager.modules.C++.load_paths",     "",
        "manager.modules.Python.load_paths",  "",
        "manager.modules.Python3.load_paths",  "",
        "manager.modules.Java.load_paths",    "",
        "manager.modules.search_auto",        "YES",
        "manager.supported_languages",        "C++, Python, Java",
        "manager.modules.C++.suffixes",       "",
        "manager.modules.Python.suffixes",    "py",
        "manager.modules.Python3.suffixes",    "py",
        "manager.modules.Java.suffixes",      "class",
        "manager.modules.C++.profile_cmd",    "rtcprof",
        "manager.modules.Python.profile_cmd", "rtcprof_python",
        "manager.modules.Python3.profile_cmd", "rtcprof_python3",
        "manager.modules.Java.profile_cmd",   "rtcprof_java",
        ""
    };
}
