package main

import (
	"context"
	"fmt"
	"os"

	"github.com/charlie-hayes/perf-sdk/cluster"
	"github.com/charlie-hayes/perf-sdk/perf"
	"github.com/charlie-hayes/perf-sdk/protocol"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type SdkService struct {
	connCntr         int
	defaultConn      *cluster.Connection
	conns            map[string]*cluster.Connection
	logger           *logrus.Logger
	performerVersion string
}

func (sdk *SdkService) getConn(connID string) *cluster.Connection {
	//TODO figure out what to do with null connection string
	if connID == "" {
		return sdk.defaultConn
	}

	if sdk.conns == nil {
		return nil
	}

	if conn, isValid := sdk.conns[connID]; isValid {
		return conn
	}

	return nil
}

func (sdk *SdkService) CreateConnection(ctx context.Context, in *protocol.CreateConnectionRequest) (*protocol.CreateConnectionResponse, error) {
	sdk.logger.Log(logrus.InfoLevel, "CreateConnection called")

	sdk.logger.Log(logrus.InfoLevel, "Creating new connection")
	conn, err := cluster.Connect(in.ClusterHostname, in.ClusterUsername, in.ClusterPassword, sdk.logger)
	if err != nil {
		sdk.logger.Logf(logrus.ErrorLevel, "Failed to connect cluster %v", err)
		return nil, status.Errorf(codes.Aborted, "connection creation failed: %v", err)
	}

	sdk.connCntr++
	connID := fmt.Sprintf("cluster-%d", sdk.connCntr)

	if sdk.conns == nil {
		sdk.conns = make(map[string]*cluster.Connection)
	}
	sdk.conns[connID] = conn

	return &protocol.CreateConnectionResponse{
		ProtocolVersion:     "2.0",
		ClusterConnectionId: connID,
	}, nil
}

func (sdk *SdkService) PerfRun(in *protocol.PerfRunRequest, stream protocol.PerformerSdkService_PerfRunServer) error {
	sdk.logger.Log(logrus.InfoLevel, "PerfRun called")

	connection := sdk.getConn(in.ClusterConnectionId)
	sdk.logger.Log(logrus.InfoLevel, connection)

	perf.PerfMarshaller(connection, in, stream, sdk.logger)

	return nil
}

func (sdk *SdkService) Exit(in *protocol.ExitRequest, exit protocol.PerformerSdkService_ExitServer) error {
	sdk.logger.Logf(logrus.InfoLevel, "Been told to exit for reason '%s' with code %d", in.GetReason(), in.GetExitCode())
	os.Exit(int(in.GetExitCode()))
	return nil
}
