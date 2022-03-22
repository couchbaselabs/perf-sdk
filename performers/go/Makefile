proto:
	mkdir -p protocol
	protoc --proto_path=../../gRPC --go_opt=paths=source_relative --go_out=plugins=grpc:./protocol ../../gRPC/sdk_basic.proto
	protoc --proto_path=../../gRPC --go_opt=paths=source_relative --go_out=plugins=grpc:./protocol ../../gRPC/sdk_commands.proto
	protoc --proto_path=../../gRPC --go_opt=paths=source_relative --go_out=plugins=grpc:./protocol ../../gRPC/sdk_performer.proto

build: proto
	go build

lint:
	golangci-lint run

.PHONY: proto build
