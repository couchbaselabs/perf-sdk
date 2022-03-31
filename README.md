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

## Notes
If you want to test multiple types of commands in the same test run, the REMOVE command has to go last.
This can be done by putting it at the bottom of the operations section of the input `.yaml` file.

Please also note that only one instance of each command is permitted per mixed workload.

