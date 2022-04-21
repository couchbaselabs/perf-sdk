# perf-sdk
A basic gRPC thing, that has a driver and a performer. Pretty cool stuff overall

## Generating Stubs
### Java Stubs
To generate java stubs: 
`cd performers/java/lib/gRPC`
`mvn install generate-sources`

### Golang Stubs
`cd performers/go`

`make proto`

## Running Driver-Performer in Docker
### Create Docker Network
`docker network create perf`

Please ensure that the TimescaleDB and Couchbase Server (if running CBS in docker) instances are also run
in this network.

### Build Docker Images
Performer:

`docker build -f performers/java/Dockerfile -t performer/java .`

`docker build -f performers/go/Dockerfile -t performer/go .`

Driver:

`docker build -f sdk-driver/Dockerfile -t driver .`

### Run Docker Images
Performer:

`docker run --rm -d --network perf -p 8060:8060 --name javaPerformer performer/java`

`docker run --rm -d --network perf -p 8060:8060 --name goPerformer performer/go`

Driver:

`docker run --rm -d --network perf -v /Path/To/testSuite.yaml:/testSuite.yaml driver /testSuite.yaml`

## Notes
If you want to test multiple types of commands in the same test run, the REMOVE command has to go last.
This can be done by putting it at the bottom of the operations section of the input `.yaml` file.

Please also note that only one instance of each command is permitted per mixed workload.

