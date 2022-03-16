// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sdk_performer.proto

package com.couchbase.grpc.sdk.protocol;

/**
 * Protobuf type {@code protocol.PerfSingleSdkOpResult}
 */
public final class PerfSingleSdkOpResult extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:protocol.PerfSingleSdkOpResult)
    PerfSingleSdkOpResultOrBuilder {
private static final long serialVersionUID = 0L;
  // Use PerfSingleSdkOpResult.newBuilder() to construct.
  private PerfSingleSdkOpResult(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PerfSingleSdkOpResult() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new PerfSingleSdkOpResult();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private PerfSingleSdkOpResult(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            SdkCommandResult.Builder subBuilder = null;
            if (results_ != null) {
              subBuilder = results_.toBuilder();
            }
            results_ = input.readMessage(SdkCommandResult.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(results_);
              results_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            com.google.protobuf.Timestamp.Builder subBuilder = null;
            if (initiated_ != null) {
              subBuilder = initiated_.toBuilder();
            }
            initiated_ = input.readMessage(com.google.protobuf.Timestamp.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(initiated_);
              initiated_ = subBuilder.buildPartial();
            }

            break;
          }
          case 26: {
            com.google.protobuf.Timestamp.Builder subBuilder = null;
            if (finished_ != null) {
              subBuilder = finished_.toBuilder();
            }
            finished_ = input.readMessage(com.google.protobuf.Timestamp.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(finished_);
              finished_ = subBuilder.buildPartial();
            }

            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return SdkPerformer.internal_static_protocol_PerfSingleSdkOpResult_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return SdkPerformer.internal_static_protocol_PerfSingleSdkOpResult_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            PerfSingleSdkOpResult.class, PerfSingleSdkOpResult.Builder.class);
  }

  public static final int RESULTS_FIELD_NUMBER = 1;
  private SdkCommandResult results_;
  /**
   * <pre>
   * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
   * </pre>
   *
   * <code>.protocol.SdkCommandResult results = 1;</code>
   * @return Whether the results field is set.
   */
  @java.lang.Override
  public boolean hasResults() {
    return results_ != null;
  }
  /**
   * <pre>
   * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
   * </pre>
   *
   * <code>.protocol.SdkCommandResult results = 1;</code>
   * @return The results.
   */
  @java.lang.Override
  public SdkCommandResult getResults() {
    return results_ == null ? SdkCommandResult.getDefaultInstance() : results_;
  }
  /**
   * <pre>
   * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
   * </pre>
   *
   * <code>.protocol.SdkCommandResult results = 1;</code>
   */
  @java.lang.Override
  public SdkCommandResultOrBuilder getResultsOrBuilder() {
    return getResults();
  }

  public static final int INITIATED_FIELD_NUMBER = 2;
  private com.google.protobuf.Timestamp initiated_;
  /**
   * <code>.google.protobuf.Timestamp initiated = 2;</code>
   * @return Whether the initiated field is set.
   */
  @java.lang.Override
  public boolean hasInitiated() {
    return initiated_ != null;
  }
  /**
   * <code>.google.protobuf.Timestamp initiated = 2;</code>
   * @return The initiated.
   */
  @java.lang.Override
  public com.google.protobuf.Timestamp getInitiated() {
    return initiated_ == null ? com.google.protobuf.Timestamp.getDefaultInstance() : initiated_;
  }
  /**
   * <code>.google.protobuf.Timestamp initiated = 2;</code>
   */
  @java.lang.Override
  public com.google.protobuf.TimestampOrBuilder getInitiatedOrBuilder() {
    return getInitiated();
  }

  public static final int FINISHED_FIELD_NUMBER = 3;
  private com.google.protobuf.Timestamp finished_;
  /**
   * <code>.google.protobuf.Timestamp finished = 3;</code>
   * @return Whether the finished field is set.
   */
  @java.lang.Override
  public boolean hasFinished() {
    return finished_ != null;
  }
  /**
   * <code>.google.protobuf.Timestamp finished = 3;</code>
   * @return The finished.
   */
  @java.lang.Override
  public com.google.protobuf.Timestamp getFinished() {
    return finished_ == null ? com.google.protobuf.Timestamp.getDefaultInstance() : finished_;
  }
  /**
   * <code>.google.protobuf.Timestamp finished = 3;</code>
   */
  @java.lang.Override
  public com.google.protobuf.TimestampOrBuilder getFinishedOrBuilder() {
    return getFinished();
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (results_ != null) {
      output.writeMessage(1, getResults());
    }
    if (initiated_ != null) {
      output.writeMessage(2, getInitiated());
    }
    if (finished_ != null) {
      output.writeMessage(3, getFinished());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (results_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getResults());
    }
    if (initiated_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(2, getInitiated());
    }
    if (finished_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(3, getFinished());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof PerfSingleSdkOpResult)) {
      return super.equals(obj);
    }
    PerfSingleSdkOpResult other = (PerfSingleSdkOpResult) obj;

    if (hasResults() != other.hasResults()) return false;
    if (hasResults()) {
      if (!getResults()
          .equals(other.getResults())) return false;
    }
    if (hasInitiated() != other.hasInitiated()) return false;
    if (hasInitiated()) {
      if (!getInitiated()
          .equals(other.getInitiated())) return false;
    }
    if (hasFinished() != other.hasFinished()) return false;
    if (hasFinished()) {
      if (!getFinished()
          .equals(other.getFinished())) return false;
    }
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasResults()) {
      hash = (37 * hash) + RESULTS_FIELD_NUMBER;
      hash = (53 * hash) + getResults().hashCode();
    }
    if (hasInitiated()) {
      hash = (37 * hash) + INITIATED_FIELD_NUMBER;
      hash = (53 * hash) + getInitiated().hashCode();
    }
    if (hasFinished()) {
      hash = (37 * hash) + FINISHED_FIELD_NUMBER;
      hash = (53 * hash) + getFinished().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static PerfSingleSdkOpResult parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static PerfSingleSdkOpResult parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static PerfSingleSdkOpResult parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static PerfSingleSdkOpResult parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static PerfSingleSdkOpResult parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static PerfSingleSdkOpResult parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static PerfSingleSdkOpResult parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static PerfSingleSdkOpResult parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static PerfSingleSdkOpResult parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static PerfSingleSdkOpResult parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static PerfSingleSdkOpResult parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static PerfSingleSdkOpResult parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(PerfSingleSdkOpResult prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code protocol.PerfSingleSdkOpResult}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:protocol.PerfSingleSdkOpResult)
          PerfSingleSdkOpResultOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return SdkPerformer.internal_static_protocol_PerfSingleSdkOpResult_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return SdkPerformer.internal_static_protocol_PerfSingleSdkOpResult_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              PerfSingleSdkOpResult.class, PerfSingleSdkOpResult.Builder.class);
    }

    // Construct using com.couchbase.grpc.sdk.protocol.PerfSingleSdkOpResult.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (resultsBuilder_ == null) {
        results_ = null;
      } else {
        results_ = null;
        resultsBuilder_ = null;
      }
      if (initiatedBuilder_ == null) {
        initiated_ = null;
      } else {
        initiated_ = null;
        initiatedBuilder_ = null;
      }
      if (finishedBuilder_ == null) {
        finished_ = null;
      } else {
        finished_ = null;
        finishedBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return SdkPerformer.internal_static_protocol_PerfSingleSdkOpResult_descriptor;
    }

    @java.lang.Override
    public PerfSingleSdkOpResult getDefaultInstanceForType() {
      return PerfSingleSdkOpResult.getDefaultInstance();
    }

    @java.lang.Override
    public PerfSingleSdkOpResult build() {
      PerfSingleSdkOpResult result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public PerfSingleSdkOpResult buildPartial() {
      PerfSingleSdkOpResult result = new PerfSingleSdkOpResult(this);
      if (resultsBuilder_ == null) {
        result.results_ = results_;
      } else {
        result.results_ = resultsBuilder_.build();
      }
      if (initiatedBuilder_ == null) {
        result.initiated_ = initiated_;
      } else {
        result.initiated_ = initiatedBuilder_.build();
      }
      if (finishedBuilder_ == null) {
        result.finished_ = finished_;
      } else {
        result.finished_ = finishedBuilder_.build();
      }
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof PerfSingleSdkOpResult) {
        return mergeFrom((PerfSingleSdkOpResult)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(PerfSingleSdkOpResult other) {
      if (other == PerfSingleSdkOpResult.getDefaultInstance()) return this;
      if (other.hasResults()) {
        mergeResults(other.getResults());
      }
      if (other.hasInitiated()) {
        mergeInitiated(other.getInitiated());
      }
      if (other.hasFinished()) {
        mergeFinished(other.getFinished());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      PerfSingleSdkOpResult parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (PerfSingleSdkOpResult) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private SdkCommandResult results_;
    private com.google.protobuf.SingleFieldBuilderV3<
            SdkCommandResult, SdkCommandResult.Builder, SdkCommandResultOrBuilder> resultsBuilder_;
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     * @return Whether the results field is set.
     */
    public boolean hasResults() {
      return resultsBuilder_ != null || results_ != null;
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     * @return The results.
     */
    public SdkCommandResult getResults() {
      if (resultsBuilder_ == null) {
        return results_ == null ? SdkCommandResult.getDefaultInstance() : results_;
      } else {
        return resultsBuilder_.getMessage();
      }
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    public Builder setResults(SdkCommandResult value) {
      if (resultsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        results_ = value;
        onChanged();
      } else {
        resultsBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    public Builder setResults(
        SdkCommandResult.Builder builderForValue) {
      if (resultsBuilder_ == null) {
        results_ = builderForValue.build();
        onChanged();
      } else {
        resultsBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    public Builder mergeResults(SdkCommandResult value) {
      if (resultsBuilder_ == null) {
        if (results_ != null) {
          results_ =
            SdkCommandResult.newBuilder(results_).mergeFrom(value).buildPartial();
        } else {
          results_ = value;
        }
        onChanged();
      } else {
        resultsBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    public Builder clearResults() {
      if (resultsBuilder_ == null) {
        results_ = null;
        onChanged();
      } else {
        results_ = null;
        resultsBuilder_ = null;
      }

      return this;
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    public SdkCommandResult.Builder getResultsBuilder() {
      
      onChanged();
      return getResultsFieldBuilder().getBuilder();
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    public SdkCommandResultOrBuilder getResultsOrBuilder() {
      if (resultsBuilder_ != null) {
        return resultsBuilder_.getMessageOrBuilder();
      } else {
        return results_ == null ?
            SdkCommandResult.getDefaultInstance() : results_;
      }
    }
    /**
     * <pre>
     * Implementations should not provide the logs field here, to conserve memory &amp; bandwidth
     * </pre>
     *
     * <code>.protocol.SdkCommandResult results = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
            SdkCommandResult, SdkCommandResult.Builder, SdkCommandResultOrBuilder>
        getResultsFieldBuilder() {
      if (resultsBuilder_ == null) {
        resultsBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
                SdkCommandResult, SdkCommandResult.Builder, SdkCommandResultOrBuilder>(
                getResults(),
                getParentForChildren(),
                isClean());
        results_ = null;
      }
      return resultsBuilder_;
    }

    private com.google.protobuf.Timestamp initiated_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder> initiatedBuilder_;
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     * @return Whether the initiated field is set.
     */
    public boolean hasInitiated() {
      return initiatedBuilder_ != null || initiated_ != null;
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     * @return The initiated.
     */
    public com.google.protobuf.Timestamp getInitiated() {
      if (initiatedBuilder_ == null) {
        return initiated_ == null ? com.google.protobuf.Timestamp.getDefaultInstance() : initiated_;
      } else {
        return initiatedBuilder_.getMessage();
      }
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    public Builder setInitiated(com.google.protobuf.Timestamp value) {
      if (initiatedBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        initiated_ = value;
        onChanged();
      } else {
        initiatedBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    public Builder setInitiated(
        com.google.protobuf.Timestamp.Builder builderForValue) {
      if (initiatedBuilder_ == null) {
        initiated_ = builderForValue.build();
        onChanged();
      } else {
        initiatedBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    public Builder mergeInitiated(com.google.protobuf.Timestamp value) {
      if (initiatedBuilder_ == null) {
        if (initiated_ != null) {
          initiated_ =
            com.google.protobuf.Timestamp.newBuilder(initiated_).mergeFrom(value).buildPartial();
        } else {
          initiated_ = value;
        }
        onChanged();
      } else {
        initiatedBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    public Builder clearInitiated() {
      if (initiatedBuilder_ == null) {
        initiated_ = null;
        onChanged();
      } else {
        initiated_ = null;
        initiatedBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    public com.google.protobuf.Timestamp.Builder getInitiatedBuilder() {
      
      onChanged();
      return getInitiatedFieldBuilder().getBuilder();
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    public com.google.protobuf.TimestampOrBuilder getInitiatedOrBuilder() {
      if (initiatedBuilder_ != null) {
        return initiatedBuilder_.getMessageOrBuilder();
      } else {
        return initiated_ == null ?
            com.google.protobuf.Timestamp.getDefaultInstance() : initiated_;
      }
    }
    /**
     * <code>.google.protobuf.Timestamp initiated = 2;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder> 
        getInitiatedFieldBuilder() {
      if (initiatedBuilder_ == null) {
        initiatedBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder>(
                getInitiated(),
                getParentForChildren(),
                isClean());
        initiated_ = null;
      }
      return initiatedBuilder_;
    }

    private com.google.protobuf.Timestamp finished_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder> finishedBuilder_;
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     * @return Whether the finished field is set.
     */
    public boolean hasFinished() {
      return finishedBuilder_ != null || finished_ != null;
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     * @return The finished.
     */
    public com.google.protobuf.Timestamp getFinished() {
      if (finishedBuilder_ == null) {
        return finished_ == null ? com.google.protobuf.Timestamp.getDefaultInstance() : finished_;
      } else {
        return finishedBuilder_.getMessage();
      }
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    public Builder setFinished(com.google.protobuf.Timestamp value) {
      if (finishedBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        finished_ = value;
        onChanged();
      } else {
        finishedBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    public Builder setFinished(
        com.google.protobuf.Timestamp.Builder builderForValue) {
      if (finishedBuilder_ == null) {
        finished_ = builderForValue.build();
        onChanged();
      } else {
        finishedBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    public Builder mergeFinished(com.google.protobuf.Timestamp value) {
      if (finishedBuilder_ == null) {
        if (finished_ != null) {
          finished_ =
            com.google.protobuf.Timestamp.newBuilder(finished_).mergeFrom(value).buildPartial();
        } else {
          finished_ = value;
        }
        onChanged();
      } else {
        finishedBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    public Builder clearFinished() {
      if (finishedBuilder_ == null) {
        finished_ = null;
        onChanged();
      } else {
        finished_ = null;
        finishedBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    public com.google.protobuf.Timestamp.Builder getFinishedBuilder() {
      
      onChanged();
      return getFinishedFieldBuilder().getBuilder();
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    public com.google.protobuf.TimestampOrBuilder getFinishedOrBuilder() {
      if (finishedBuilder_ != null) {
        return finishedBuilder_.getMessageOrBuilder();
      } else {
        return finished_ == null ?
            com.google.protobuf.Timestamp.getDefaultInstance() : finished_;
      }
    }
    /**
     * <code>.google.protobuf.Timestamp finished = 3;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder> 
        getFinishedFieldBuilder() {
      if (finishedBuilder_ == null) {
        finishedBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Timestamp, com.google.protobuf.Timestamp.Builder, com.google.protobuf.TimestampOrBuilder>(
                getFinished(),
                getParentForChildren(),
                isClean());
        finished_ = null;
      }
      return finishedBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:protocol.PerfSingleSdkOpResult)
  }

  // @@protoc_insertion_point(class_scope:protocol.PerfSingleSdkOpResult)
  private static final PerfSingleSdkOpResult DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new PerfSingleSdkOpResult();
  }

  public static PerfSingleSdkOpResult getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PerfSingleSdkOpResult>
      PARSER = new com.google.protobuf.AbstractParser<PerfSingleSdkOpResult>() {
    @java.lang.Override
    public PerfSingleSdkOpResult parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new PerfSingleSdkOpResult(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<PerfSingleSdkOpResult> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PerfSingleSdkOpResult> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public PerfSingleSdkOpResult getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
