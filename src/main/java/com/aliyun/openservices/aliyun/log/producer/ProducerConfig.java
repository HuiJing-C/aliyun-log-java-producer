package com.aliyun.openservices.aliyun.log.producer;

import com.aliyun.openservices.aliyun.log.producer.internals.Utils;

/**
 * Configuration for {@link LogProducer}. See each each individual set method for details about each
 * parameter.
 */
public class ProducerConfig {

  public static final int DEFAULT_TOTAL_SIZE_IN_BYTES = 100 * 1024 * 1024;

  public static final long DEFAULT_MAX_BLOCK_MS = 60 * 1000L;

  public static final int DEFAULT_IO_THREAD_COUNT =
      Math.max(Runtime.getRuntime().availableProcessors(), 1);

  public static final int DEFAULT_MAX_BATCH_SIZE_IN_BYTES = 3 * 1024 * 1024;

  public static final int MAX_BATCH_SIZE_IN_BYTES_UPPER_LIMIT = 5 * 1024 * 1024;

  public static final int DEFAULT_MAX_BATCH_COUNT = 40960;

  public static final int MAX_BATCH_COUNT_UPPER_LIMIT = 40960;

  public static final int DEFAULT_LINGER_MS = 2000;

  public static final int LINGER_MS_LOWER_LIMIT = 100;

  public static final int DEFAULT_RETRIES = 10;

  public static final long DEFAULT_BASE_RETRY_BACKOFF_MS = 100L;

  public static final long DEFAULT_MAX_RETRY_BACKOFF_MS = 600 * 1000L;

  public static final long DEFAULT_SHARD_HASH_UPDATE_INTERVAL_MS = 600 * 1000L;

  public enum LogFormat {
    PROTOBUF,
    JSON
  }

  public static final LogFormat DEFAULT_LOG_FORMAT = LogFormat.PROTOBUF;

  private final ProjectConfigs projectConfigs;

  private int totalSizeInBytes = DEFAULT_TOTAL_SIZE_IN_BYTES;

  private long maxBlockMs = DEFAULT_MAX_BLOCK_MS;

  private int ioThreadCount = DEFAULT_IO_THREAD_COUNT;

  private int maxBatchSizeInBytes = DEFAULT_MAX_BATCH_SIZE_IN_BYTES;

  private int maxBatchCount = DEFAULT_MAX_BATCH_COUNT;

  private int lingerMs = DEFAULT_LINGER_MS;

  private int retries = DEFAULT_RETRIES;

  private int maxReservedAttempts = DEFAULT_RETRIES + 1;

  private long baseRetryBackoffMs = DEFAULT_BASE_RETRY_BACKOFF_MS;

  private long maxRetryBackoffMs = DEFAULT_MAX_RETRY_BACKOFF_MS;

  private long shardHashUpdateIntervalMS = DEFAULT_SHARD_HASH_UPDATE_INTERVAL_MS;

  private LogFormat logFormat = DEFAULT_LOG_FORMAT;

  public ProducerConfig(ProjectConfigs projectConfigs) {
    Utils.assertArgumentNotNull(projectConfigs, "projectConfigs");
    this.projectConfigs = projectConfigs;
  }

  /** @return {@link ProjectConfigs} of this configuration. */
  public ProjectConfigs getProjectConfigs() {
    return projectConfigs;
  }

  /**
   * @return The total bytes of memory the producer can use to buffer logs waiting to be sent to the
   *     server.
   */
  public int getTotalSizeInBytes() {
    return totalSizeInBytes;
  }

  /**
   * Set the total bytes of memory the producer can use to buffer logs waiting to be sent to the
   * server.
   */
  public void setTotalSizeInBytes(int totalSizeInBytes) {
    if (totalSizeInBytes <= 0) {
      throw new IllegalArgumentException(
          "totalSizeInBytes must be greater than 0, got " + totalSizeInBytes);
    }
    this.totalSizeInBytes = totalSizeInBytes;
  }

  /** @return How long <code>LogProducer.send()</code> will block. */
  public long getMaxBlockMs() {
    return maxBlockMs;
  }

  /** Set how long <code>LogProducer.send()</code> will block. */
  public void setMaxBlockMs(long maxBlockMs) {
    this.maxBlockMs = maxBlockMs;
  }

  /** @return The thread count of the background I/O thread pool. */
  public int getIoThreadCount() {
    return ioThreadCount;
  }

  /** Set the thread count of the background I/O thread pool. */
  public void setIoThreadCount(int ioThreadCount) {
    if (ioThreadCount <= 0) {
      throw new IllegalArgumentException(
          "ioThreadCount must be greater than 0, got " + ioThreadCount);
    }
    this.ioThreadCount = ioThreadCount;
  }

  /** @return The upper limit of batch size. */
  public int getMaxBatchSizeInBytes() {
    return maxBatchSizeInBytes;
  }

  /** Set the upper limit of batch size. */
  public void setMaxBatchSizeInBytes(int maxBatchSizeInBytes) {
    if (maxBatchSizeInBytes < 1 || maxBatchSizeInBytes > MAX_BATCH_SIZE_IN_BYTES_UPPER_LIMIT) {
      throw new IllegalArgumentException(
          String.format(
              "maxBatchSizeInBytes must be between 1 and %d, got %d",
              MAX_BATCH_SIZE_IN_BYTES_UPPER_LIMIT, maxBatchSizeInBytes));
    }
    this.maxBatchSizeInBytes = maxBatchSizeInBytes;
  }

  /** @return The upper limit of batch count. */
  public int getMaxBatchCount() {
    return maxBatchCount;
  }

  /** Set the upper limit of batch count. */
  public void setMaxBatchCount(int maxBatchCount) {
    if (maxBatchCount < 1 || maxBatchCount > MAX_BATCH_COUNT_UPPER_LIMIT) {
      throw new IllegalArgumentException(
          String.format(
              "maxBatchCount must be between 1 and %d, got %d",
              MAX_BATCH_COUNT_UPPER_LIMIT, maxBatchCount));
    }
    this.maxBatchCount = maxBatchCount;
  }

  /** @return The max linger time of a log. */
  public int getLingerMs() {
    return lingerMs;
  }

  /** Set the max linger time of a log. */
  public void setLingerMs(int lingerMs) {
    if (lingerMs < LINGER_MS_LOWER_LIMIT) {
      throw new IllegalArgumentException(
          String.format(
              "lingerMs must be greater than or equal to %d, got %d",
              LINGER_MS_LOWER_LIMIT, lingerMs));
    }
    this.lingerMs = lingerMs;
  }

  /** @return The retry times for transient error. */
  public int getRetries() {
    return retries;
  }

  /**
   * Set the retry times for transient error. Setting a value greater than zero will cause the
   * client to resend any log whose send fails with a potentially transient error.
   */
  public void setRetries(int retries) {
    this.retries = retries;
  }

  /** @return How many {@link Attempt}s will be reserved in a {@link Result}. */
  public int getMaxReservedAttempts() {
    return maxReservedAttempts;
  }

  /** Set how many {@link Attempt}s will be reserved in a {@link Result}. */
  public void setMaxReservedAttempts(int maxReservedAttempts) {
    if (maxReservedAttempts <= 0) {
      throw new IllegalArgumentException(
          "maxReservedAttempts must be greater than 0, got " + maxReservedAttempts);
    }
    this.maxReservedAttempts = maxReservedAttempts;
  }

  /**
   * @return The amount of time to wait before attempting to retry a failed request for the first
   *     time.
   */
  public long getBaseRetryBackoffMs() {
    return baseRetryBackoffMs;
  }

  /**
   * Set the amount of time to wait before attempting to retry a failed request for the first time.
   */
  public void setBaseRetryBackoffMs(long baseRetryBackoffMs) {
    if (baseRetryBackoffMs <= 0) {
      throw new IllegalArgumentException(
          "baseRetryBackoffMs must be greater than 0, got " + baseRetryBackoffMs);
    }
    this.baseRetryBackoffMs = baseRetryBackoffMs;
  }

  /** @return The upper limit of time to wait before attempting to retry a failed request. */
  public long getMaxRetryBackoffMs() {
    return maxRetryBackoffMs;
  }

  /** Set the upper limit of time to wait before attempting to retry a failed request. */
  public void setMaxRetryBackoffMs(long maxRetryBackoffMs) {
    if (maxRetryBackoffMs <= 0) {
      throw new IllegalArgumentException(
          "maxRetryBackoffMs must be greater than 0, got " + maxRetryBackoffMs);
    }
    this.maxRetryBackoffMs = maxRetryBackoffMs;
  }

  /** @return The time interval for updating all shard information. */
  public long getShardHashUpdateIntervalMS() {
    return shardHashUpdateIntervalMS;
  }

  /** Set the time interval for updating all shard information. */
  public void setShardHashUpdateIntervalMS(long shardHashUpdateIntervalMS) {
    if (shardHashUpdateIntervalMS < 100) {
      throw new IllegalArgumentException(
          "shardHashUpdateIntervalMS must be greater than or equal to 100, got "
              + shardHashUpdateIntervalMS);
    }
    this.shardHashUpdateIntervalMS = shardHashUpdateIntervalMS;
  }

  /** @return The content type of the request. */
  public LogFormat getLogFormat() {
    return logFormat;
  }

  /** Set the content type of the request. */
  public void setLogFormat(LogFormat logFormat) {
    this.logFormat = logFormat;
  }
}
