package perf

import (
	"fmt"
	"time"

	"github.com/charlie-hayes/perf-sdk/cluster"
	"github.com/charlie-hayes/perf-sdk/protocol"
	"github.com/couchbase/gocb/v2"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"golang.org/x/sync/errgroup"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func PerfMarshaller(conn *cluster.Connection, perfReq *protocol.PerfRunRequest, stream protocol.PerformerSdkService_PerfRunServer, logger *logrus.Logger) error {
	g := new(errgroup.Group)
	for i := 0; i < len(perfReq.GetHorizontalScaling()); i++ {
		perGoRoutine := perfReq.GetHorizontalScaling()[i]
		logger.Logf(logrus.InfoLevel, "Starting goroutine No.", i)
		g.Go(func() error {
			now := time.Now()
			until := now.Add(time.Second * time.Duration(perfReq.GetRunForSeconds()))
			isDone := false

			for {
				if !isDone {
					for r := 0; r < len(perGoRoutine.GetSdkCommand()); r++ {
						opResult := protocol.PerfSingleSdkOpResult{
							Initiated: timestamppb.Now(),
						}
						result, err := beginOperations(conn, perGoRoutine.GetSdkCommand()[r], logger)
						if err != nil {
							return err
						}
						opResult.Finished = timestamppb.Now()
						opResult.Results = result
						opResult.VersionId = "go:2.4.1"

						if err := stream.Send(&opResult); err != nil {
							return err
						}

						if time.Now().After(until) {
							isDone = true
							return nil
						}
					}
				}
			}
		})
	}
	if err := g.Wait(); err != nil {
		return err
	}
	return nil
}

func beginOperations(conn *cluster.Connection, req *protocol.SdkCreateRequest, logger *logrus.Logger) (*protocol.SdkCommandResult, error) {
	for _, command := range req.GetCommands() {
		err := performOperation(conn, command, logger)
		if err != nil {
			return nil, err
		}
	}
	//TODO return errors and logs
	return &protocol.SdkCommandResult{}, nil
}

func performOperation(conn *cluster.Connection, op *protocol.SdkCommand, logger *logrus.Logger) error {
	if op.GetInsert() != nil {
		request := op.GetInsert()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Insert(uuid.NewString(), request.ContentJson, &gocb.InsertOptions{})
		if err != nil {
			return err
		}
		return nil
	} else {
		return fmt.Errorf("internal performer failure: Unknown operation")
	}
}
