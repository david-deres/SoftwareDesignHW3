// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: inboxTypes.proto

package il.ac.technion.cs.softwaredesign;

public interface MessagePOrBuilder extends
    // @@protoc_insertion_point(interface_extends:il.ac.technion.cs.softwaredesign.MessageP)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string id = 1;</code>
   * @return The id.
   */
  java.lang.String getId();
  /**
   * <code>string id = 1;</code>
   * @return The bytes for id.
   */
  com.google.protobuf.ByteString
      getIdBytes();

  /**
   * <code>string from = 2;</code>
   * @return The from.
   */
  java.lang.String getFrom();
  /**
   * <code>string from = 2;</code>
   * @return The bytes for from.
   */
  com.google.protobuf.ByteString
      getFromBytes();

  /**
   * <code>string message = 3;</code>
   * @return The message.
   */
  java.lang.String getMessage();
  /**
   * <code>string message = 3;</code>
   * @return The bytes for message.
   */
  com.google.protobuf.ByteString
      getMessageBytes();

  /**
   * <code>.google.protobuf.Timestamp timeSent = 4;</code>
   * @return Whether the timeSent field is set.
   */
  boolean hasTimeSent();
  /**
   * <code>.google.protobuf.Timestamp timeSent = 4;</code>
   * @return The timeSent.
   */
  com.google.protobuf.Timestamp getTimeSent();
  /**
   * <code>.google.protobuf.Timestamp timeSent = 4;</code>
   */
  com.google.protobuf.TimestampOrBuilder getTimeSentOrBuilder();
}
