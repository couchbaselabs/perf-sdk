# Couchbase SDK Performance
For performance testing of the Couchbase SDKs.

A configuration file is supplied, detailing the full test matrix - what versions of what SDKs to test, against which servers, on which platforms, running which workloads.
[Jenkins-SDK](https://github.com/couchbaselabs/jenkins-sdk) includes code that processes this config file and calculates the hundreds of performance runs that need to be performed.
It compares this against the database to see what's already been run.

For each run that needs running, jenkins-sdk (running on CI, or locally) will create a per-run config file, and then drive aspects of this project.

It will fire up the appropriate 'performer'.  There is one of these for each SDK.

It will then fire up the 'driver', and pass it the per-run config file.

The driver interprets this config file, prepares (creating any documents etc.), and tells the performer what to do, over GRPC.
This may look something like "run 1m removes, across 10 threads".

The performer performs the workload.
Every operation (e.g. each remove) streams back a result over GRPC to the driver.

The driver bucketises the data into one-second buckets, and writes it to the database.

# Technologies
We use timescaledb, a time-scale version of Postgres, as we're working with timeseries database.

The hosted UI (a separate project) is in Vue and Node.

The driver is in modern Java, and the protocol is GRPC.

jenkins-sdk is in Groovy because it was originally intended to be a Jenkins shared library (which need to be in Groovy).
But there were too many requirements and restrictions for that to work well (this could be revisited), and it worked well enough to just compile to code and run it from a Jenkins job instead.
But Groovy is a useful, semi-scripted language that leaves the door open to one day becoming a shared library, so Groovy it has stayed.

# Running on localhost
The project is designed to run both on CI (Jenkins), where it writes to a hosted database.

And locally, so that developers can reproduce and investigate.

It is possible to manually build and spin up the driver and performer - but it's much easier to let the jenkins-sdk project drive everything.

Clone:
```
git clone https://github.com/couchbaselabs/perf-sdk
git clone https://github.com/couchbaselabs/jenkins-sdk
```

Setup Docker network:

```
docker network create perf

# This next step only needed if you also run Couchbase in Docker. It assumes it was run with --name couchbase.
docker network connect perf couchbase
```

Then this will spin up timescaledb with Docker, on the "perf" Docker network, with password "password", and create all tables:
```
cd perf-sdk
make dbPerf
cd ..
```

Edit jenkins-sdk/config/job-config.yaml appropriately.  
Recall that this declaratively states all the performance runs we want to exist, and it will take ages to populate that into your local database unless edited down.
You'll want to change these at least:
* `database.hostname` to "localhost".
* Setup `matrix` according to what you want to test - which is likely to be a small subset of the runs.
  * Almost certainly reduce `matrix.implementations` to just a handful of versions.
  * Almost certainly reduce `matrix.clusters` to one cluster.
  * Almost certainly reduce `matrix.workloads` to a handful.


And kick off jenkins-sdk with:
```
cd jenkins-sdk
./gradlew shadowJar
java -jar .\build\libs\jenkins2-1.0-SNAPSHOT-all.jar
```

Or can replace the last step by running jenkins-sdk in an IDE.  Run `src/com/couchbase/perf/shared/main/main.groovy`.

jenkins-sdk will do everything:

* Look at config/job-config.yaml and calculate what runs need to exist.
* Look at the database to see what runs it needs to run.
* For each run, it outputs a per-run config file, with a UUID.
* Brings up and down clusters as needed.
* Build the driver Docker image.
* Build the relevant performer Docker image(s).
* Run the performer(s).
* Run the driver.

## Running Driver-Performer in Docker
### Create Docker Network
```
docker network create perf

# This next step only needed if you also run Couchbase in Docker. It assumes it was run with --name couchbase.
docker network connect perf couchbase
```

### Build + Run Database
This will spin up timescaledb with Docker, on the "perf" Docker network, with password "password", and create all tables.
`make dbPerf`

### Build Docker Images
Performer:

`docker build -f performers/java/Dockerfile -t performer/java .`

`docker build -f performers/go/Dockerfile -t performer/go .`

`docker build -f performers/python/Dockerfile -t performer/python .`

Driver:

`docker build -f sdk-driver/Dockerfile -t driver .`

### Run Docker Images
Performer:

`docker run --rm -d --network perf -p 8060:8060 --name javaPerformer performer/java`

`docker run --rm -d --network perf -p 8060:8060 --name goPerformer performer/go`

Driver:

`docker run --rm -d --network perf -v /Path/To/testSuite.yaml:/testSuite.yaml driver /testSuite.yaml`

## Generating Stubs
For use if you want to run things locally
### Java Stubs
To generate java stubs:
`cd performers/java/lib/gRPC`
`mvn install generate-sources`

### Golang Stubs
`cd performers/go`

`make proto`

### Python Stubs
`cd performers/python`

`make proto`

Please Note: Unfortunately I could not get the generated files to work in a seperate directory so it just dumps them in
performers/python. If you know how to fix this please do.

## Notes
If you want to test multiple types of commands in the same test run, the REMOVE command has to go last.
This can be done by putting it at the bottom of the operations section of the input `.yaml` file.

Please also note that only one instance of each command is permitted per mixed workload.

