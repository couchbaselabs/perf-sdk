import sdk_performer_pb2_grpc as performer_grpc
import sdk_performer_pb2 as sdk_performer_pb2
from cluster_connection import ClusterConnection
from exceptions import *
from perf_marshaller import perf_marshaller

from threading import Thread
from concurrent import futures
import logging
import grpc
import sys
import traceback
import queue

class Listener(performer_grpc.PerformerSdkServiceServicer):
    def __init__(self):
        self.logger = logging.getLogger()
        handler = logging.StreamHandler()
        formatter = logging.Formatter('%(asctime)s %(name)-12s %(levelname)-8s %(message)s')
        handler.setFormatter(formatter)
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)
        self.conn_cntr = 0
        self.conns = {}
        self.default_conn = None

    def createConnection(self, request, context):
        self.logger.info("Creating new connection")

        try:
            conn = ClusterConnection(request, self.logger)
        except Exception as e:
            raise Exception(e)

        self.conn_cntr += 1
        conn_id = f"cluster-{self.conn_cntr}"
        self.conns[conn_id] = conn

        # defaultConn will always be the most recent connections
        self.default_conn = conn

        return sdk_performer_pb2.CreateConnectionResponse(protocolVersion = "2.0", clusterConnectionId = conn_id)

    def get_connection(self, conn_id):
        if conn_id == "":
            return self.default_conn
        elif self.conns == {}:
            raise ConnectionDictCreation("The connection dictionary has not been created, has createConnection been called?")
        elif conn_id in self.conns:
            return self.conns[conn_id]
        else:
            return GetConnectionFailed("Unable to get connection")

            

    def perfRun(self, request, context):
        self.logger.info("Starting perf run")
        write_queue = queue.Queue()
        connection = self.get_connection(request.clusterConnectionId)
        self.logger.info(connection)
        perf = Thread(target = perf_marshaller, args = (connection, request, write_queue, self.logger))
        perf.start()
        while not(write_queue.empty() and not(perf.is_alive())):
            yield write_queue.get()
        return None
    
    def Exit(self, request, context):
        None



def serve():
    print("called serve")
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
    print("made serverobject")
    performer_grpc.add_PerformerSdkServiceServicer_to_server(Listener(), server)
    print("added listerner")
    server.add_insecure_port("[::]:8060")
    print("add port")
    server.start()
    print("started server")
    server.wait_for_termination()

if __name__ == "__main__":
    try:
        print("table")
        serve()
    except Exception:
        exc_info = sys.exc_info()
        tb = ''.join(traceback.format_tb(exc_info[2]))
        print(f'Exception info: {exc_info[1]}\nTraceback:\n{tb}')