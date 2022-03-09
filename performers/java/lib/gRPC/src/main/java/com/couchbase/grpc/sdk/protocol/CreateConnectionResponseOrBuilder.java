// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sdk_performer.proto

package com.couchbase.grpc.sdk.protocol;

public interface CreateConnectionResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:protocol.CreateConnectionResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Human-readable string identifying the performer.  For example, "java".
   * </pre>
   *
   * <code>string performerUserAgent = 1;</code>
   * @return The performerUserAgent.
   */
  java.lang.String getPerformerUserAgent();
  /**
   * <pre>
   * Human-readable string identifying the performer.  For example, "java".
   * </pre>
   *
   * <code>string performerUserAgent = 1;</code>
   * @return The bytes for performerUserAgent.
   */
  com.google.protobuf.ByteString
      getPerformerUserAgentBytes();

  /**
   * <pre>
   * Identifies the version of the library under test.  For example, "1.1.2".
   * </pre>
   *
   * <code>string performerLibraryVersion = 2;</code>
   * @return The performerLibraryVersion.
   */
  java.lang.String getPerformerLibraryVersion();
  /**
   * <pre>
   * Identifies the version of the library under test.  For example, "1.1.2".
   * </pre>
   *
   * <code>string performerLibraryVersion = 2;</code>
   * @return The bytes for performerLibraryVersion.
   */
  com.google.protobuf.ByteString
      getPerformerLibraryVersionBytes();

  /**
   * <pre>
   * Defined https://hackmd.io/foGjnSSIQmqfks2lXwNp8w#Protocol-Versions
   * Must be "1.0" or "2.0", any other values will cause the driver to abort.
   * </pre>
   *
   * <code>string protocolVersion = 3;</code>
   * @return The protocolVersion.
   */
  java.lang.String getProtocolVersion();
  /**
   * <pre>
   * Defined https://hackmd.io/foGjnSSIQmqfks2lXwNp8w#Protocol-Versions
   * Must be "1.0" or "2.0", any other values will cause the driver to abort.
   * </pre>
   *
   * <code>string protocolVersion = 3;</code>
   * @return The bytes for protocolVersion.
   */
  com.google.protobuf.ByteString
      getProtocolVersionBytes();

  /**
   * <code>string clusterConnectionId = 4;</code>
   * @return The clusterConnectionId.
   */
  java.lang.String getClusterConnectionId();
  /**
   * <code>string clusterConnectionId = 4;</code>
   * @return The bytes for clusterConnectionId.
   */
  com.google.protobuf.ByteString
      getClusterConnectionIdBytes();
}
