from os import times
from webbrowser import get
import doc_pool_counter
from operation import perform_operation
from threading import Thread
from google.protobuf.timestamp_pb2 import Timestamp
import sdk_performer_pb2_grpc as performer_grpc
import sdk_performer_pb2
import logging
import uuid
import psutil

def get_time_now():
    timestamp = Timestamp()
    timestamp.GetCurrentTime()
    return timestamp



def perf_marshaller(connection, perf_request, write_queue, logger):
    get_counter = doc_pool_counter.DocPoolCounter()
    replace_counter = doc_pool_counter.DocPoolCounter()
    remove_counter = doc_pool_counter.DocPoolCounter()
    runners = []
    for request in perf_request.horizontalScaling:
        runners.append(Thread(target = perf_runner, args = (connection, request, get_counter, replace_counter, remove_counter, write_queue, logger)))

    logger.info(f"Starting {len(runners)} thread(s)")

    for per_thread in runners:
        per_thread.start()

    for per_thread in runners:
        per_thread.join()

    logger.info(f"All {len(runners)} threads completed")

    get_counter.reset_counter()
    replace_counter.reset_counter()
    remove_counter.reset_counter()



def perf_runner(connection, request, get_counter, replace_counter, remove_counter, write_queue, logger):
    for command in request.sdkCommand:
        if command.name == "INSERT":
            request = command.command.insert
            collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)

            for _ in range(command.count):
                command_initiated = get_time_now()

                logger.info(f"Performing insert operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
                collection.insert(str(uuid.uuid4()), request.contentJson)
                perf_result = sdk_performer_pb2.SdkCommandResult(log = str(psutil.virtual_memory()))

                command_finished = get_time_now()
                write_queue.put(sdk_performer_pb2.PerfSingleSdkOpResult(results = perf_result, initiated = command_initiated, finished = command_finished))

        elif command.name == "GET":
            request = command.command.get
            collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)

            for _ in range(command.count):
                command_initiated = get_time_now()

                logger.info(f"Performing get operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
                collection.get((request.keyPreface + str(get_counter.get_and_inc())))
                perf_result = sdk_performer_pb2.SdkCommandResult(log = str(psutil.virtual_memory()))

                command_finished = get_time_now()
                write_queue.put(sdk_performer_pb2.PerfSingleSdkOpResult(results = perf_result, initiated = command_initiated, finished = command_finished))

        elif command.name == "REMOVE":
            request = command.command.remove
            collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)

            for _ in range(command.count):
                command_initiated = get_time_now()

                logger.info(f"Performing remove operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
                collection.remove(request.keyPreface + str(remove_counter.get_and_inc()))
                perf_result = sdk_performer_pb2.SdkCommandResult(log = str(psutil.virtual_memory()))

                command_finished = get_time_now()
                write_queue.put(sdk_performer_pb2.PerfSingleSdkOpResult(results = perf_result, initiated = command_initiated, finished = command_finished))

        elif command.name == "REPLACE":
            request = command.command.replace
            collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)

            for _ in range(command.count):
                command_initiated = get_time_now()

                logger.info(f"Performing replace operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
                collection.replace(request.keyPreface + str(replace_counter.get_and_inc()), request.contentJson)
                perf_result = sdk_performer_pb2.SdkCommandResult(log = str(psutil.virtual_memory()))

                command_finished = get_time_now()
                write_queue.put(sdk_performer_pb2.PerfSingleSdkOpResult(results = perf_result, initiated = command_initiated, finished = command_finished))