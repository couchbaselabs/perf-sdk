FROM python:3.9

WORKDIR /app

COPY ./performers/python/requirements.txt requirements.txt
RUN pip3 install -r requirements.txt
RUN python3 -m pip install git+https://github.com/couchbase/couchbase-python-client.git

COPY . .

WORKDIR /app/performers/python/
RUN python3 -m grpc_tools.protoc -I../../gRPC --python_out=. --grpc_python_out=. ../../gRPC/sdk_basic.proto && \
	python3 -m grpc_tools.protoc -I../../gRPC --python_out=. --grpc_python_out=. ../../gRPC/sdk_commands.proto && \
	python3 -m grpc_tools.protoc -I../../gRPC --python_out=. --grpc_python_out=. ../../gRPC/sdk_performer.proto

ENTRYPOINT ["python3", "main.py"]