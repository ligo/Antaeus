#!/bin/bash
# -*- coding: UTF-8 -*-

# set -x
if [[ $1 == "-start" ]]
then
    echo "start image searcher and indexer "
    source /etc/profile
    cd /home/Antaeus
    java -cp build/libs/Antaeus-1.0.jar com.oceanai.main.AntaeusServer index.json
elif [[ $1 == "-build" ]]
then
    source /etc/profile
    cd /home/Antaeus
    echo "start copy dependencies"
    gradle copyJar
    echo "start build project "
    gradle build
    echo "build successful~"
fi
