package com.couchbase.grpc.sdk.protocol;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.39.0)",
    comments = "Source: sdk_performer.proto")
public final class PerformerSdkServiceGrpc {

  private PerformerSdkServiceGrpc() {}

  public static final String SERVICE_NAME = "protocol.PerformerSdkService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.sdk.protocol.CreateConnectionRequest,
      com.couchbase.grpc.sdk.protocol.CreateConnectionResponse> getCreateConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "createConnection",
      requestType = com.couchbase.grpc.sdk.protocol.CreateConnectionRequest.class,
      responseType = com.couchbase.grpc.sdk.protocol.CreateConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.sdk.protocol.CreateConnectionRequest,
      com.couchbase.grpc.sdk.protocol.CreateConnectionResponse> getCreateConnectionMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.sdk.protocol.CreateConnectionRequest, com.couchbase.grpc.sdk.protocol.CreateConnectionResponse> getCreateConnectionMethod;
    if ((getCreateConnectionMethod = PerformerSdkServiceGrpc.getCreateConnectionMethod) == null) {
      synchronized (PerformerSdkServiceGrpc.class) {
        if ((getCreateConnectionMethod = PerformerSdkServiceGrpc.getCreateConnectionMethod) == null) {
          PerformerSdkServiceGrpc.getCreateConnectionMethod = getCreateConnectionMethod =
              io.grpc.MethodDescriptor.<com.couchbase.grpc.sdk.protocol.CreateConnectionRequest, com.couchbase.grpc.sdk.protocol.CreateConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "createConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.sdk.protocol.CreateConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.sdk.protocol.CreateConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PerformerSdkServiceMethodDescriptorSupplier("createConnection"))
              .build();
        }
      }
    }
    return getCreateConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest,
      com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse> getSdkFactoryCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sdkFactoryCreate",
      requestType = com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest.class,
      responseType = com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest,
      com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse> getSdkFactoryCreateMethod() {
    io.grpc.MethodDescriptor<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest, com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse> getSdkFactoryCreateMethod;
    if ((getSdkFactoryCreateMethod = PerformerSdkServiceGrpc.getSdkFactoryCreateMethod) == null) {
      synchronized (PerformerSdkServiceGrpc.class) {
        if ((getSdkFactoryCreateMethod = PerformerSdkServiceGrpc.getSdkFactoryCreateMethod) == null) {
          PerformerSdkServiceGrpc.getSdkFactoryCreateMethod = getSdkFactoryCreateMethod =
              io.grpc.MethodDescriptor.<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest, com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sdkFactoryCreate"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PerformerSdkServiceMethodDescriptorSupplier("sdkFactoryCreate"))
              .build();
        }
      }
    }
    return getSdkFactoryCreateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PerformerSdkServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PerformerSdkServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PerformerSdkServiceStub>() {
        @java.lang.Override
        public PerformerSdkServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PerformerSdkServiceStub(channel, callOptions);
        }
      };
    return PerformerSdkServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PerformerSdkServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PerformerSdkServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PerformerSdkServiceBlockingStub>() {
        @java.lang.Override
        public PerformerSdkServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PerformerSdkServiceBlockingStub(channel, callOptions);
        }
      };
    return PerformerSdkServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PerformerSdkServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PerformerSdkServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PerformerSdkServiceFutureStub>() {
        @java.lang.Override
        public PerformerSdkServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PerformerSdkServiceFutureStub(channel, callOptions);
        }
      };
    return PerformerSdkServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class PerformerSdkServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Creates a connection between performer and couchbase server
     * </pre>
     */
    public void createConnection(com.couchbase.grpc.sdk.protocol.CreateConnectionRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.sdk.protocol.CreateConnectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public void sdkFactoryCreate(com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSdkFactoryCreateMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateConnectionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.sdk.protocol.CreateConnectionRequest,
                com.couchbase.grpc.sdk.protocol.CreateConnectionResponse>(
                  this, METHODID_CREATE_CONNECTION)))
          .addMethod(
            getSdkFactoryCreateMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest,
                com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse>(
                  this, METHODID_SDK_FACTORY_CREATE)))
          .build();
    }
  }

  /**
   */
  public static final class PerformerSdkServiceStub extends io.grpc.stub.AbstractAsyncStub<PerformerSdkServiceStub> {
    private PerformerSdkServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerformerSdkServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerformerSdkServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a connection between performer and couchbase server
     * </pre>
     */
    public void createConnection(com.couchbase.grpc.sdk.protocol.CreateConnectionRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.sdk.protocol.CreateConnectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public void sdkFactoryCreate(com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest request,
        io.grpc.stub.StreamObserver<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSdkFactoryCreateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PerformerSdkServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<PerformerSdkServiceBlockingStub> {
    private PerformerSdkServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerformerSdkServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerformerSdkServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a connection between performer and couchbase server
     * </pre>
     */
    public com.couchbase.grpc.sdk.protocol.CreateConnectionResponse createConnection(com.couchbase.grpc.sdk.protocol.CreateConnectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse sdkFactoryCreate(com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSdkFactoryCreateMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PerformerSdkServiceFutureStub extends io.grpc.stub.AbstractFutureStub<PerformerSdkServiceFutureStub> {
    private PerformerSdkServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerformerSdkServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerformerSdkServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a connection between performer and couchbase server
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.sdk.protocol.CreateConnectionResponse> createConnection(
        com.couchbase.grpc.sdk.protocol.CreateConnectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a Transactions (e.g. a transactions factory), returning a transactionsFactoryRef
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse> sdkFactoryCreate(
        com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSdkFactoryCreateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_CONNECTION = 0;
  private static final int METHODID_SDK_FACTORY_CREATE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PerformerSdkServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PerformerSdkServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_CONNECTION:
          serviceImpl.createConnection((com.couchbase.grpc.sdk.protocol.CreateConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.sdk.protocol.CreateConnectionResponse>) responseObserver);
          break;
        case METHODID_SDK_FACTORY_CREATE:
          serviceImpl.sdkFactoryCreate((com.couchbase.grpc.sdk.protocol.SdkFactoryCreateRequest) request,
              (io.grpc.stub.StreamObserver<com.couchbase.grpc.sdk.protocol.SdkFactoryCreateResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PerformerSdkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PerformerSdkServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.couchbase.grpc.sdk.protocol.SdkPerformer.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PerformerSdkService");
    }
  }

  private static final class PerformerSdkServiceFileDescriptorSupplier
      extends PerformerSdkServiceBaseDescriptorSupplier {
    PerformerSdkServiceFileDescriptorSupplier() {}
  }

  private static final class PerformerSdkServiceMethodDescriptorSupplier
      extends PerformerSdkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PerformerSdkServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PerformerSdkServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PerformerSdkServiceFileDescriptorSupplier())
              .addMethod(getCreateConnectionMethod())
              .addMethod(getSdkFactoryCreateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
