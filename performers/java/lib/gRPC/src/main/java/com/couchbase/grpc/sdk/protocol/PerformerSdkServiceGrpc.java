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
  private static volatile io.grpc.MethodDescriptor<CreateConnectionRequest,
          CreateConnectionResponse> getCreateConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "createConnection",
      requestType = CreateConnectionRequest.class,
      responseType = CreateConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<CreateConnectionRequest,
          CreateConnectionResponse> getCreateConnectionMethod() {
    io.grpc.MethodDescriptor<CreateConnectionRequest, CreateConnectionResponse> getCreateConnectionMethod;
    if ((getCreateConnectionMethod = PerformerSdkServiceGrpc.getCreateConnectionMethod) == null) {
      synchronized (PerformerSdkServiceGrpc.class) {
        if ((getCreateConnectionMethod = PerformerSdkServiceGrpc.getCreateConnectionMethod) == null) {
          PerformerSdkServiceGrpc.getCreateConnectionMethod = getCreateConnectionMethod =
              io.grpc.MethodDescriptor.<CreateConnectionRequest, CreateConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "createConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  CreateConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  CreateConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PerformerSdkServiceMethodDescriptorSupplier("createConnection"))
              .build();
        }
      }
    }
    return getCreateConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<SdkCreateRequest,
          SdkCommandResult> getSdkCommandMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sdkCommand",
      requestType = SdkCreateRequest.class,
      responseType = SdkCommandResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<SdkCreateRequest,
          SdkCommandResult> getSdkCommandMethod() {
    io.grpc.MethodDescriptor<SdkCreateRequest, SdkCommandResult> getSdkCommandMethod;
    if ((getSdkCommandMethod = PerformerSdkServiceGrpc.getSdkCommandMethod) == null) {
      synchronized (PerformerSdkServiceGrpc.class) {
        if ((getSdkCommandMethod = PerformerSdkServiceGrpc.getSdkCommandMethod) == null) {
          PerformerSdkServiceGrpc.getSdkCommandMethod = getSdkCommandMethod =
              io.grpc.MethodDescriptor.<SdkCreateRequest, SdkCommandResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sdkCommand"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  SdkCreateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  SdkCommandResult.getDefaultInstance()))
              .setSchemaDescriptor(new PerformerSdkServiceMethodDescriptorSupplier("sdkCommand"))
              .build();
        }
      }
    }
    return getSdkCommandMethod;
  }

  private static volatile io.grpc.MethodDescriptor<PerfRunRequest,
          PerfSingleSdkOpResult> getPerfRunMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "perfRun",
      requestType = PerfRunRequest.class,
      responseType = PerfSingleSdkOpResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<PerfRunRequest,
          PerfSingleSdkOpResult> getPerfRunMethod() {
    io.grpc.MethodDescriptor<PerfRunRequest, PerfSingleSdkOpResult> getPerfRunMethod;
    if ((getPerfRunMethod = PerformerSdkServiceGrpc.getPerfRunMethod) == null) {
      synchronized (PerformerSdkServiceGrpc.class) {
        if ((getPerfRunMethod = PerformerSdkServiceGrpc.getPerfRunMethod) == null) {
          PerformerSdkServiceGrpc.getPerfRunMethod = getPerfRunMethod =
              io.grpc.MethodDescriptor.<PerfRunRequest, PerfSingleSdkOpResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "perfRun"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  PerfRunRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  PerfSingleSdkOpResult.getDefaultInstance()))
              .setSchemaDescriptor(new PerformerSdkServiceMethodDescriptorSupplier("perfRun"))
              .build();
        }
      }
    }
    return getPerfRunMethod;
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
    public void createConnection(CreateConnectionRequest request,
                                 io.grpc.stub.StreamObserver<CreateConnectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateConnectionMethod(), responseObserver);
    }

    /**
     */
    public void sdkCommand(SdkCreateRequest request,
                           io.grpc.stub.StreamObserver<SdkCommandResult> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSdkCommandMethod(), responseObserver);
    }

    /**
     */
    public void perfRun(PerfRunRequest request,
                        io.grpc.stub.StreamObserver<PerfSingleSdkOpResult> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPerfRunMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateConnectionMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                      CreateConnectionRequest,
                      CreateConnectionResponse>(
                  this, METHODID_CREATE_CONNECTION)))
          .addMethod(
            getSdkCommandMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                      SdkCreateRequest,
                      SdkCommandResult>(
                  this, METHODID_SDK_COMMAND)))
          .addMethod(
            getPerfRunMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                      PerfRunRequest,
                      PerfSingleSdkOpResult>(
                  this, METHODID_PERF_RUN)))
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
    public void createConnection(CreateConnectionRequest request,
                                 io.grpc.stub.StreamObserver<CreateConnectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sdkCommand(SdkCreateRequest request,
                           io.grpc.stub.StreamObserver<SdkCommandResult> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSdkCommandMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void perfRun(PerfRunRequest request,
                        io.grpc.stub.StreamObserver<PerfSingleSdkOpResult> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getPerfRunMethod(), getCallOptions()), request, responseObserver);
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
    public CreateConnectionResponse createConnection(CreateConnectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateConnectionMethod(), getCallOptions(), request);
    }

    /**
     */
    public SdkCommandResult sdkCommand(SdkCreateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSdkCommandMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<PerfSingleSdkOpResult> perfRun(
        PerfRunRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getPerfRunMethod(), getCallOptions(), request);
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
    public com.google.common.util.concurrent.ListenableFuture<CreateConnectionResponse> createConnection(
        CreateConnectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateConnectionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<SdkCommandResult> sdkCommand(
        SdkCreateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSdkCommandMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_CONNECTION = 0;
  private static final int METHODID_SDK_COMMAND = 1;
  private static final int METHODID_PERF_RUN = 2;

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
          serviceImpl.createConnection((CreateConnectionRequest) request,
              (io.grpc.stub.StreamObserver<CreateConnectionResponse>) responseObserver);
          break;
        case METHODID_SDK_COMMAND:
          serviceImpl.sdkCommand((SdkCreateRequest) request,
              (io.grpc.stub.StreamObserver<SdkCommandResult>) responseObserver);
          break;
        case METHODID_PERF_RUN:
          serviceImpl.perfRun((PerfRunRequest) request,
              (io.grpc.stub.StreamObserver<PerfSingleSdkOpResult>) responseObserver);
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
      return SdkPerformer.getDescriptor();
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
              .addMethod(getSdkCommandMethod())
              .addMethod(getPerfRunMethod())
              .build();
        }
      }
    }
    return result;
  }
}
