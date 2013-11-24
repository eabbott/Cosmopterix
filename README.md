# Cosmopterix
HyperLogLog utilizing voltdb as the backend.
This implementation was based upon the HLL found in https://github.com/addthis/stream-lib v2.4.0.

## Requirements
Needs clearsprings hash methods.

## Migration
Voltdb is an in memory database that is blazing fast but operationally is
a bit tricky to work with. In AWS it's pretty common for an instance to have
a problem and the server on it needs to be migrated. In voltdb, much of the
business logic can be stored as part of the database for performance gains
(ala stored procedures).

The way to implement HyperLogLog most efficiently is happily also idempotent.
Therefore we can easily migrate from one cluster to another just by writing
to two clusters, migrating all data

* Fire up a new voltdb cluster
* Start writing to both clusters
* Migrate the data via the hll.merge() method (idempotent)
* Start reading from new cluster
* Stop writing to old cluster
* Ditch old cluster

In this implementation the migration step is through a method that gets
all keys and values in one shot from the voltdb cluster. This call will
freeze voltdb for the duration of the command. Whatever your pause threshold
is will determine when you have to switch from that approach to something
more traditional such as reading the keys from a snapshot and slowing iterating.

## Usage
Currently volt activity is kicked off through the run.sh script based off the
voltdb examples while the test run and IDE expect to use maven. As such
configuration is a bit wonky.


* Install voltdb into /opt/voltdb or your preferred location
* Install clearsprings stream-lib v2.4.0 as per the pom.xml
* Modify run.sh updating the config lines to point to your voltdb and stream-lib locations
* ./run.sh srccompile
* ./run.sh catalog
* ./run.sh server     # to start the first voltdb server
* ./run.sh server2    # to start the second voltdb server
* Fire up an ide and kick off the Run.main method


## Fun Hll facts
```
log2m =  1, storage =     4b, Hll 2^01 =     2 buckets, accuracy = 73.54%
log2m =  2, storage =     4b, Hll 2^02 =     4 buckets, accuracy = 52.00%
log2m =  3, storage =     8b, Hll 2^03 =     8 buckets, accuracy = 36.77%
log2m =  4, storage =    12b, Hll 2^04 =    16 buckets, accuracy = 26.00%
log2m =  5, storage =    24b, Hll 2^05 =    32 buckets, accuracy = 18.38%
log2m =  6, storage =    44b, Hll 2^06 =    64 buckets, accuracy = 13.00%
log2m =  7, storage =    88b, Hll 2^07 =   128 buckets, accuracy = 9.19%
log2m =  8, storage =   172b, Hll 2^08 =   256 buckets, accuracy = 6.50%
log2m =  9, storage =   344b, Hll 2^09 =   512 buckets, accuracy = 4.60%
log2m = 10, storage =   684b, Hll 2^10 =  1024 buckets, accuracy = 3.25%
log2m = 11, storage =  1368b, Hll 2^11 =  2048 buckets, accuracy = 2.30%
log2m = 12, storage =  2732b, Hll 2^12 =  4096 buckets, accuracy = 1.63%
log2m = 13, storage =  5464b, Hll 2^13 =  8192 buckets, accuracy = 1.15%
log2m = 14, storage = 10924b, Hll 2^14 = 16384 buckets, accuracy = 0.81%
log2m = 15, storage = 21848b, Hll 2^15 = 32768 buckets, accuracy = 0.57%
log2m = 16, storage = 43692b, Hll 2^16 = 65536 buckets, accuracy = 0.41%
```
