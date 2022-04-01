package perf

import (
	"fmt"
	"strconv"

	"github.com/charlie-hayes/perf-sdk/cluster"
	"github.com/charlie-hayes/perf-sdk/protocol"
	gocb "github.com/couchbase/gocb/v2"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"golang.org/x/sync/errgroup"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func PerfMarshaller(conn *cluster.Connection, perfReq *protocol.PerfRunRequest, stream protocol.PerformerSdkService_PerfRunServer, logger *logrus.Logger) error {
	docPool := map[string]*docPoolCounter{
		"REMOVE":  &docPoolCounter{},
		"REPLACE": &docPoolCounter{},
		"GET":     &docPoolCounter{}}
	g := new(errgroup.Group)
	for i := 0; i < len(perfReq.GetHorizontalScaling()); i++ {
		perGoRoutine := perfReq.GetHorizontalScaling()[i]
		logger.Logf(logrus.InfoLevel, "Starting goroutine No.", i)
		g.Go(func() error {
			//now := time.Now()
			//until := now.Add(time.Second * time.Duration(perfReq.GetRunForSeconds()))
			//isDone := false

			for _, command := range perGoRoutine.GetSdkCommand() {
				for r := 0; r < int(command.GetCount()); r++ {
					opResult := protocol.PerfSingleSdkOpResult{
						Initiated: timestamppb.Now(),
					}
					result, err := performOperation(conn, command.GetCommand(), logger, docPool)
					if err != nil {
						return err
					}
					opResult.Finished = timestamppb.Now()
					opResult.Results = result

					if err := stream.Send(&opResult); err != nil {
						return err
					}
					//if time.Now().After(until) {
					//	isDone = true
					//	return nil
					//}
				}
			}
			return nil
		})
	}
	err := g.Wait()
	for _, count := range docPool {
		count.resetCounter()
	}
	if err != nil {
		return err
	}
	return nil
}

// func beginOperations(conn *cluster.Connection, req *protocol.SdkCreateRequest, logger *logrus.Logger, docPool *docPoolCounter) (*protocol.SdkCommandResult, error) {
// 	//logger.Log(logrus.InfoLevel, "Beginning operations")
// 	for i := 0; i < int(req.GetCount()); i++ {
// 		err := performOperation(conn, req.GetCommand(), logger, docPool)
// 		if err != nil {
// 			return nil, err
// 		}
// 	}
// 	//TODO return errors and logs
// 	return &protocol.SdkCommandResult{}, nil
// }

func performOperation(conn *cluster.Connection, op *protocol.SdkCommand, logger *logrus.Logger, docPool map[string]*docPoolCounter) (*protocol.SdkCommandResult, error) {
	if op.GetInsert() != nil {
		logger.Log(logrus.InfoLevel, "Performing insert")
		request := op.GetInsert()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Insert(uuid.NewString(), request.ContentJson, &gocb.InsertOptions{})
		if err != nil {
			return &protocol.SdkCommandResult{}, err
		}
		return &protocol.SdkCommandResult{}, nil
	} else if op.GetGet() != nil {
		logger.Log(logrus.InfoLevel, "Performing get operation")
		request := op.GetGet()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Get(fmt.Sprintf(request.GetKeyPreface()+strconv.Itoa(docPool["GET"].getAndInc())), &gocb.GetOptions{})
		if err != nil {
			return &protocol.SdkCommandResult{}, err
		}
		return &protocol.SdkCommandResult{}, nil
	} else if op.GetRemove() != nil {
		logger.Log(logrus.InfoLevel, "Performing remove operation")
		request := op.GetRemove()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Remove(fmt.Sprintf(request.GetKeyPreface()+strconv.Itoa(docPool["REMOVE"].getAndInc())), &gocb.RemoveOptions{})
		if err != nil {
			return &protocol.SdkCommandResult{}, err
		}
		return &protocol.SdkCommandResult{}, nil
	} else if op.GetReplace() != nil {
		logger.Log(logrus.InfoLevel, "Performing replace operation")
		request := op.GetReplace()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Replace(fmt.Sprintf(request.GetKeyPreface()+strconv.Itoa(docPool["REPLACE"].getAndInc())), request.ContentJson, &gocb.ReplaceOptions{})
		if err != nil {
			return &protocol.SdkCommandResult{}, err
		}
		return &protocol.SdkCommandResult{}, nil
	} else {
		return &protocol.SdkCommandResult{}, fmt.Errorf("internal performer failure: Unknown operation")
	}
}
