#!/bin/sh
#
# The shell script to get the profile of RTC.

#######################################
# Gets CLASSPATH for OpenRTM
# Arguments:
#   None
# Returns:
#   CLASSPATH
#######################################
get_classpath()
{
  local file1
  file1=$(ls ${RTM_JAVA_ROOT}/jar/OpenRTM*)
  local file2
  file2=$(ls ${RTM_JAVA_ROOT}/jar/commons-cli*)
  local file3
  file3=$(ls ${RTM_JAVA_ROOT}/jar/jna-?.?.?.jar)
  local file4
  file4=$(ls ${RTM_JAVA_ROOT}/jar/jna-platform-*.jar)
  local class_path
  class_path=.:${file1}:${file2}:${file3}:${file4}:${RTM_JAVA_ROOT}/bin:$(dirname $0)/bin
  echo ${class_path}
}

rtm_jar_path=`find /usr/lib -name OpenRTM-aist*.jar 2>/dev/null`
RTM_JAVA_ROOT=`echo ${rtm_jar_path%/jar/*}`

if test "x${RTM_JAVA_ROOT}" = "x" ; then
    echo "Environment variable RTM_JAVA_ROOT is not set."
    echo "Please specify the OpenRTM-aist installation directory."
    echo "Abort."
    exit 1
fi

export CLASSPATH=$(get_classpath)
java -cp ${CLASSPATH}:${RTM_JAVA_ROOT}/jar/rtcprof.jar rtcprof.rtcprof ${1+"$@"}

