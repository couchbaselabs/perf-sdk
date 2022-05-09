# perf-sdk
A basic gRPC thing, that has a driver and a performer. Pretty cool stuff overall

## Jenkins-SDK
This project is designed to be run by [Jenkins-SDK](https://github.com/couchbaselabs/jenkins-sdk). Currently the version that works with this project
lives on  my laptop, so it's exclusive to me only

## Running Driver-Performer in Docker
### Create Docker Network
`docker network create perf`

Please ensure that the TimescaleDB and Couchbase Server (if running CBS in docker) instances are also run
in this network.

### Build + Run Database
`make database`

(This is a very janky thing that just waits 10 seconds for the container to be created)

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

