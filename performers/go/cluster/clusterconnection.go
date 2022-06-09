package cluster

import (
	"time"

	gocb "github.com/couchbase/gocb/v2"
	"github.com/sirupsen/logrus"
)

type Connection struct {
	cluster *gocb.Cluster
	bucket  *gocb.Bucket
}

func Connect(hostname, username, password, bucketName string, logger *logrus.Logger) (*Connection, error) {
	gocb.SetLogger(&gocbLogger{logger: logger})
	c, err := gocb.Connect("couchbase://"+hostname, gocb.ClusterOptions{
		Username: username,
		Password: password,
	})
	if err != nil {
		return nil, err
	}

	b := c.Bucket(bucketName)
	logger.Logf(logrus.InfoLevel, "Bucket set: ", b.Name())
	err = b.WaitUntilReady(30*time.Second, nil)
	if err != nil {
		return nil, err
	}

	return &Connection{
		cluster: c,
		bucket:  b,
	}, nil
}

func (c *Connection) DefaultBucket(logger *logrus.Logger) *gocb.Bucket {
	return c.bucket
}

type gocbLogger struct {
	logger *logrus.Logger
}

func (logger *gocbLogger) Log(level gocb.LogLevel, offset int, format string, v ...interface{}) error {
	// We need to do some conversion between gocb and logrus levels as they don't match up.
	var logrusLevel logrus.Level
	switch level {
	case gocb.LogError:
		logrusLevel = logrus.ErrorLevel
	case gocb.LogWarn:
		logrusLevel = logrus.WarnLevel
	case gocb.LogInfo:
		logrusLevel = logrus.InfoLevel
	case gocb.LogDebug:
		logrusLevel = logrus.DebugLevel
	case gocb.LogTrace:
		logrusLevel = logrus.TraceLevel
	case gocb.LogSched:
		logrusLevel = logrus.TraceLevel
	case gocb.LogMaxVerbosity:
		logrusLevel = logrus.TraceLevel
	}

	// Send the data to the logrus Logf function to make sure that it gets formatted correctly.
	logger.logger.Logf(logrusLevel, format, v...)
	return nil
}
