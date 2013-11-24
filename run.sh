#!/usr/bin/env bash

####### Modify these as required #######
VOLTDB_BIN="/opt/voltdb/bin"
VOLTDB_LIB="/opt/voltdb/lib"
VOLTDB_VOLTDB="/opt/voltdb/voltdb"
MURMUR_LIB="/Users/eabbott/.m2/repository/com/clearspring/analytics/stream/2.4.0/"
########## Done modifications ##########

APPNAME="volthll"

APPCLASSPATH=$CLASSPATH:$({ \
    \ls -1 "$VOLTDB_VOLTDB"/voltdb-*.jar; \
    \ls -1 "$VOLTDB_LIB"/*.jar; \
    \ls -1 "$VOLTDB_LIB"/extension/*.jar; \
    \ls -1 "$MURMUR_LIB"/*.jar; \
} 2> /dev/null | paste -sd ':' - )
VOLTDB="$VOLTDB_BIN/voltdb"
LOG4J="$VOLTDB_VOLTDB/log4j.xml"
LICENSE="$VOLTDB_VOLTDB/license.xml"
HOST="localhost"

# remove build artifacts
function clean() {
    rm -rf obj debugoutput $APPNAME.jar $APPNAME.api.jar voltdbroot voltdbroot
}

# compile the source code for procedures and the client
function srccompile() {
    mkdir -p obj
    echo $APPCLASSPATH
    javac -target 1.6 -source 1.6 -classpath $APPCLASSPATH -d obj \
        src/org/eabbott/volthll/api/*.java \
        src/org/eabbott/volthll/procedures/*.java \
        src/org/eabbott/volthll/example/*.java
    # stop if compilation fails
    if [ $? != 0 ]; then exit; fi

    jar -cf $APPNAME.api.jar -C obj/ org/eabbott/volthll/api/
}

# build an application catalog
function catalog() {
    srccompile
    $VOLTDB compile --classpath obj -o $APPNAME.jar hllddl.sql
    cd obj
    jar uf ../$APPNAME.jar org/eabbott/volthll/api/RegisterSet.class
    cd ..
    # stop if compilation fails
    if [ $? != 0 ]; then exit; fi
}

# run the voltdb server locally
function server() {
    # if a catalog doesn't exist, build one
    if [ ! -f $APPNAME.jar ]; then catalog; fi
    # run the server
    $VOLTDB create catalog $APPNAME.jar deployment deployment.xml \
        license $LICENSE host $HOST
}

# run the voltdb server locally
function server2() {
    # if a catalog doesn't exist, build one
    if [ ! -f $APPNAME.jar ]; then catalog; fi
    # run the server
    $VOLTDB create catalog $APPNAME.jar deployment deployment2.xml \
        license $LICENSE host $HOST port 21270 internalport 21272 replicationport 21273 zkport 21276 adminport 21277
}

function help() {
    echo "Usage: ./run.sh {clean|catalog|server|server2}"
}

# Run the target passed as the first arg on the command line
# If no first arg, run server
if [ $# -gt 1 ]; then help; exit; fi
if [ $# = 1 ]; then $1; else server; fi
