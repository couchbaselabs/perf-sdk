from ast import Pass
from couchbase.cluster import Cluster
from couchbase.options import ClusterOptions
from couchbase.auth import PasswordAuthenticator

from datetime import timedelta

class ClusterConnection():
    def __init__(self, request, logger):
        try:
            logger.info(request.clusterHostname + " " + request.clusterUsername + " " + request.clusterPassword + " " + request.bucketName)
            opts = ClusterOptions(authenticator=PasswordAuthenticator(request.clusterUsername, request.clusterPassword))
            self.cluster = Cluster.connect(("couchbase://" + request.clusterHostname), opts)
            self.cluster.wait_until_ready(timedelta(seconds=30))
            self.bucket = self.cluster.bucket(request.bucketName)
        except Exception as e:
            logger.exception(e)
            raise Exception(f'Failed to connect to cluster.  Error: {e}')

