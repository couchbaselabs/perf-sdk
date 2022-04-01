package main

import (
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"strconv"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc"

	"github.com/charlie-hayes/perf-sdk/protocol"
	"github.com/charlie-hayes/perf-sdk/service"
)

func main() {
	// logLevel := envFlagInt("TXNPERFORMERLOGLEVEL", "log-level", 0,
	// 	"The log level to use")
	port := envFlagInt("PERFORMERPORT", "port", 8060,
		"The port to use")
	version := envFlagString("VERSION", "version", "1.0.0",
		"The version to use")
	flag.Parse()

	logger := logrus.New()
	logger.SetFormatter(&logrus.JSONFormatter{
		PrettyPrint: true,
	})
	logger.SetOutput(os.Stdout)
	logger.SetLevel(logrus.InfoLevel)

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatalf("Failed to start listener: %v", err)
	}

	grpcSrv := grpc.NewServer()
	protocol.RegisterPerformerSdkServiceServer(grpcSrv, &service.SdkService{
		Logger:           logger,
		PerformerVersion: "1.0.0",
	})

	logger.Logf(logrus.InfoLevel, "Starting grpc server at %d", *port)
	logger.Logf(logrus.DebugLevel, "Version: %s", version)
	err = grpcSrv.Serve(lis)
	if err != nil {
		log.Fatalf("Failed to start grpc server: %v", err)
	}
}

func envFlagString(envName, name string, value string, usage string) *string {
	envValue := os.Getenv(envName)
	if envValue != "" {
		value = envValue
	}
	return flag.String(name, value, usage)
}

func envFlagInt(envName, name string, value int, usage string) *int {
	envValue := os.Getenv(envName)
	if envValue != "" {
		var err error
		value, err = strconv.Atoi(envValue)
		if err != nil {
			panic("failed to parse string as int")
		}
	}
	return flag.Int(name, value, usage)
}
