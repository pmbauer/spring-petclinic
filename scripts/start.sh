#!/bin/bash
OPTIONS=""
if [ "$1" == "ap" ];
then
  OPTIONS="-agentpath:../../ap/build/libasyncProfiler.so=start,event=wall,collapsed,file=petclinic_collapsed.txt"
fi
if [ "$1" == "jfr" ];
then
  OPTIONS="-XX:StartFlightRecording=filename=petclinic.jfr,dumponexit=true"
fi
if [ "$1" == "dd" ];
then
  OPTIONS="-javaagent:../../dd-java-agent.jar -Ddd.profiling.enabled=true -Ddd.profiling.api-key-file=../../profiling-api-key -Ddd.trace.enabled=false"
fi
if [ "${1%%_*}" == "stackdepth" ];
then
  depth="${1#*_}"
  OPTIONS="-javaagent:../../dd-java-agent.jar -Ddd.profiling.enabled=true -Ddd.profiling.api-key-file=../../profiling-api-key -Ddd.trace.enabled=false -XX:FlightRecorderOptions=stackdepth=${depth}"
fi
if [ "$2" == "zulu8" ];
then
  export JAVA_HOME=../../jdk/zulu8.46.0.19-ca-jdk8.0.252-linux_x64
fi
if [ "$2" == "jdk8dev" ];
then
  export JAVA_HOME=../../jdk/jdk8dev
fi
if [ "$2" == "jdk11" ];
then
  export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
fi
export PATH=$PATH:$JAVA_HOME/bin
export DD_TAGS="type:${1}"
$JAVA_HOME/bin/java ${OPTIONS} \
    -DnbThreads=0 \
    -DnbExtraStack=256 \
    -jar ../target/spring-petclinic-2.2.0.BUILD-SNAPSHOT.jar > out_petclinic.txt
