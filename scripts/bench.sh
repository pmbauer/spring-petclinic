#!/bin/bash
INJECT_COUNT=5
OUTPUT_FILE=out_petclinic.txt
THREADS=2

function bench () {
  if [ "$1" == "" ];
  then
    echo "Missing tag"
    exit 1
  fi
  if [ "$2" == "" ];
  then
    echo "Missing jdk"
    exit 1
  fi
  TAG=$1
  JDK=$2
  # checking prequisites
  checks $TAG
  for I in $(seq $INJECT_COUNT);
  do
    if [ -f $OUTPUT_FILE ];
    then
      rm $OUTPUT_FILE
    fi
    echo "$(date +%H:%M:%S) Starting application ${TAG}-${JDK} run $I/$INJECT_COUNT..."
    ./start.sh $TAG $JDK &
    PID=$!
    DEAD=0
    sleep 0.2
    while [ "$(grep -o "Started PetClinicApplication" $OUTPUT_FILE)" != "Started PetClinicApplication" -a "$DEAD" != "1" ];
    do
      kill -0 $PID
      DEAD=$?
      sleep 1
    done
    if [ "$DEAD" == "1" ];
    then
      echo "Application not started correctly!"
      exit 1
    fi

    pidstat -r -C java 1 > mem-${TAG}-${JDK}-${I}.txt &
    echo "$(date +%H:%M:%S) Sending requests..."
    pids=()
    for FORK in $(seq $THREADS);
    do
      ./inject.sh results_${TAG}-${JDK}-${I}_${FORK}.csv &
      pids[$FORK]=$!
    done
    for FORK in $(seq $THREADS);
    do
      pid=${pids[$FORK]}
      wait $pid
    done
    # grab user & sys cpu ticks
    java_pid=$(pgrep java)
    echo "java pid: $java_pid"
    cat /proc/$java_pid/stat | cut -d " " -f 14 > cpu_ticks_${TAG}-${JDK}-${I}.txt
    # merge all request sender threads
    true > results_${TAG}-${JDK}-${I}.csv
    for FORK in $(seq $THREADS);
    do
      cat results_${TAG}-${JDK}-${I}_${FORK}.csv >> results_${TAG}-${JDK}-${I}.csv
    done
    echo "Killing $PID"
    pkill -P $PID
    pkill pidstat
    sleep 1
  done
  python percentiles.py ${TAG}-${JDK}.csv results_${TAG}-${JDK}-?.csv
}

function checks () {
  TAG=$1
  if [ "$TAG" == "ap" ];
  then
    if [ ! -f ../../ap/build/libasyncProfiler.so ];
    then
      echo "Async profiler library is missing."
      exit 1
    fi
    echo 1 | sudo tee /proc/sys/kernel/perf_event_paranoid
    echo 0 | sudo tee /proc/sys/kernel/kptr_restrict
  fi
  if [ "$TAG" == "dd" ];
  then
    if [ ! -f ../../dd-java-agent.jar ];
    then
      echo "DD java agent is missing"
      exit 1
    fi
    if [ ! -f ../../profiling-api-key ];
    then
      echo "DD api-key file is missing"
      exit 1
    fi
  fi
}

bench stackdepth_256 jdk11
bench stackdepth_128 jdk11
bench stackdepth_64  jdk11
