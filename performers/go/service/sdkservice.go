package service

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
	Logger           *logrus.Logger
	PerformerVersion string
}

func (sdk *SdkService) getConn(connID string) *cluster.Connection {
	if connID == "" {
		return sdk.defaultConn
	}

	if sdk.conns == nil {
		return nil
	}

	if conn, isValid := sdk.conns[connID]; isValid {
		return conn
	}
	//TODO error here
	return nil
}

func (sdk *SdkService) CreateConnection(ctx context.Context, in *protocol.CreateConnectionRequest) (*protocol.CreateConnectionResponse, error) {
	sdk.Logger.Log(logrus.InfoLevel, "CreateConnection called")

	sdk.Logger.Log(logrus.InfoLevel, "Creating new connection")
	conn, err := cluster.Connect(in.GetClusterHostname(), in.GetClusterUsername(), in.GetClusterPassword(), in.GetBucketName(), sdk.Logger)
	if err != nil {
		sdk.Logger.Logf(logrus.ErrorLevel, "Failed to connect cluster %v", err)
		return nil, status.Errorf(codes.Aborted, "connection creation failed: %v", err)
	}

	sdk.connCntr++
	connID := fmt.Sprintf("cluster-%d", sdk.connCntr)

	if sdk.conns == nil {
		sdk.conns = make(map[string]*cluster.Connection)
	}
	sdk.conns[connID] = conn

	// defaultConn will always be the most recent connection
	sdk.defaultConn = conn

	return &protocol.CreateConnectionResponse{
		ProtocolVersion:     "2.0",
		ClusterConnectionId: connID,
	}, nil
}

func (sdk *SdkService) PerfRun(in *protocol.PerfRunRequest, stream protocol.PerformerSdkService_PerfRunServer) error {
	sdk.Logger.Log(logrus.InfoLevel, "PerfRun called")

	connection := sdk.getConn(in.ClusterConnectionId)
	sdk.Logger.Log(logrus.InfoLevel, connection)

	if err := perf.PerfMarshaller(connection, in, stream, sdk.Logger); err != nil {
		sdk.Logger.Logf(logrus.ErrorLevel, "error whilst executing perfRun %v", err)
		return status.Errorf(codes.Aborted, "error whilst executing perfRun %v", err)
	}

	return nil
}

func (sdk *SdkService) Exit(in *protocol.ExitRequest, exit protocol.PerformerSdkService_ExitServer) error {
	sdk.Logger.Logf(logrus.InfoLevel, "Been told to exit for reason '%s' with code %d", in.GetReason(), in.GetExitCode())
	os.Exit(int(in.GetExitCode()))
	return nil
}
