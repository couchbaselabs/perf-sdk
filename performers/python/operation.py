import sdk_performer_pb2
import logging
import json
import uuid
import psutil

def perform_operation(connection, command, get_counter, replace_counter, remove_counter, logger):
#     TODO change this to oneof implementation
    if command.name == "INSERT":
        request = command.command.insert
        collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)
        logger.info(f"Performing insert operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
        collection.insert(str(uuid.uuid4()), request.contentJson)
#         log = str(psutil.virtual_memory())
        return sdk_performer_pb2.SdkCommandResult()
    if command.name == "GET":
        request = command.command.get
        collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)
        logger.info(f"Performing get operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
        collection.get((request.keyPreface + str(get_counter.get_and_inc())))
#       log = str(psutil.virtual_memory())
        return sdk_performer_pb2.SdkCommandResult()
    if command.name == "REMOVE":
        request = command.command.remove
        collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)
        logger.info(f"Performing remove operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
        collection.remove(request.keyPreface + str(remove_counter.get_and_inc()))
#         log = str(psutil.virtual_memory())
        return sdk_performer_pb2.SdkCommandResult()
    if command.name == "REPLACE":
        request = command.command.replace
        collection = connection.bucket.scope(request.bucketInfo.scopeName).collection(request.bucketInfo.collectionName)
        logger.info(f"Performing replace operation on bucket {request.bucketInfo.bucketName} on collection {request.bucketInfo.collectionName}")
        collection.replace(request.keyPreface + str(replace_counter.get_and_inc()), request.contentJson)
#         log = str(psutil.virtual_memory())
        return sdk_performer_pb2.SdkCommandResult()