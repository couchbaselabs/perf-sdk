# A Docker image for the Couchbase SDK Performance Framework
# Java Performer

FROM golang:1.17
WORKDIR /app

COPY . .

WORKDIR /app/performers/go

RUN go mod download
RUN apt-get update && apt-get install make protobuf-compiler -y
RUN go get -u github.com/golang/protobuf/protoc-gen-go


RUN mkdir -p "protocol"

RUN protoc --proto_path=../../gRPC --go_opt=paths=source_relative --go_out=plugins=grpc:./protocol ../../gRPC/sdk_basic.proto && \
protoc --proto_path=../../gRPC --go_opt=paths=source_relative --go_out=plugins=grpc:./protocol ../../gRPC/sdk_commands.proto && \
protoc --proto_path=../../gRPC --go_opt=paths=source_relative --go_out=plugins=grpc:./protocol ../../gRPC/sdk_performer.proto


RUN go build -o /go-performer

ENTRYPOINT ["/go-performer"]