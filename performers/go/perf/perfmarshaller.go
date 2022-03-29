package perf

import (
	"fmt"
	"strconv"
	"sync/atomic"

	"github.com/charlie-hayes/perf-sdk/cluster"
	"github.com/charlie-hayes/perf-sdk/protocol"
	gocb "github.com/couchbase/gocb/v2"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
	"golang.org/x/sync/errgroup"
	"google.golang.org/protobuf/types/known/timestamppb"
)

type docPoolCounter struct {
	//Using int64 as the string conversion function converts an int to that anyway
	docPoolNum int64
}

func (c *docPoolCounter) add(num int64) {
	atomic.AddInt64(&c.docPoolNum, num)
}

func (c *docPoolCounter) read() int64 {
	return atomic.LoadInt64(&c.docPoolNum)
}

func PerfMarshaller(conn *cluster.Connection, perfReq *protocol.PerfRunRequest, stream protocol.PerformerSdkService_PerfRunServer, logger *logrus.Logger) error {
	docPool := &docPoolCounter{1}
	g := new(errgroup.Group)
	for i := 0; i < len(perfReq.GetHorizontalScaling()); i++ {
		perGoRoutine := perfReq.GetHorizontalScaling()[i]
		logger.Logf(logrus.InfoLevel, "Starting goroutine No.", i)
		g.Go(func() error {
			//now := time.Now()
			//until := now.Add(time.Second * time.Duration(perfReq.GetRunForSeconds()))
			//isDone := false
			count := calculateCount(perGoRoutine.GetSdkCommand())

			for {
				if countMax(count) != 0 {
					for _, command := range perGoRoutine.GetSdkCommand() {
						if count[command.GetName()] > 0 {
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

							count[command.GetName()] -= 1
							//if time.Now().After(until) {
							//	isDone = true
							//	return nil
							//}
						}
					}
				} else {
					return nil
				}
			}
		})
	}
	if err := g.Wait(); err != nil {
		return err
	}
	return nil
}

func calculateCount(commandList []*protocol.SdkCreateRequest) map[string]int {
	count := make(map[string]int)
	for _, command := range commandList {
		count[command.GetName()] = int(command.GetCount())
	}
	return count
}

func countMax(count map[string]int) int {
	//commandCount can't be lower than 0
	max := -1
	for _, commandCount := range count {
		if commandCount > max {
			max = commandCount
		}
	}
	return max
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

func performOperation(conn *cluster.Connection, op *protocol.SdkCommand, logger *logrus.Logger, docPool *docPoolCounter) (*protocol.SdkCommandResult, error) {
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
		request := op.GetGet()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Get(request.GetDocId(), &gocb.GetOptions{})
		if err != nil {
			return &protocol.SdkCommandResult{}, err
		}
		return &protocol.SdkCommandResult{}, nil
	} else if op.GetRemove() != nil {
		logger.Logf(logrus.InfoLevel, "Performing remove operation:", docPool.read())
		request := op.GetRemove()
		collection := conn.DefaultBucket(logger).Scope(request.BucketInfo.ScopeName).Collection(request.BucketInfo.CollectionName)
		_, err := collection.Remove(fmt.Sprintf(request.GetKeyPreface()+strconv.FormatInt(docPool.read(), 10)), &gocb.RemoveOptions{})
		if err != nil {
			return &protocol.SdkCommandResult{}, err
		}
		docPool.add(1)
		return &protocol.SdkCommandResult{}, nil
	} else {
		return &protocol.SdkCommandResult{}, fmt.Errorf("internal performer failure: Unknown operation")
	}
}
