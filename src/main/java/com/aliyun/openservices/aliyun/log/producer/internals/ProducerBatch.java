package com.aliyun.openservices.aliyun.log.producer.internals;

import com.aliyun.openservices.aliyun.log.producer.*;
import com.aliyun.openservices.aliyun.log.producer.errors.ResultFailedException;
import com.aliyun.openservices.log.common.LogItem;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ProducerBatch implements Delayed {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerBatch.class);

    private final GroupKey groupKey;

    private final String packageId;

    private final int maxBatchSizeInBytes;

    private final int maxBatchCount;

    private final List<LogItem> logItems = new ArrayList<LogItem>();

    private final List<Thunk> thunks = new ArrayList<Thunk>();

    private final long createdMs;

    private long nextRetryMs;

    private int curBatchSizeInBytes;

    private int curBatchCount;

    private final EvictingQueue<Attempt> reservedAttempts;

    private int attemptCount;

    public ProducerBatch(GroupKey groupKey,
                         String packageId,
                         int maxBatchSizeInBytes,
                         int maxBatchCount,
                         int maxReservedAttempts,
                         long nowMs) {
        this.groupKey = groupKey;
        this.packageId = packageId;
        this.createdMs = nowMs;
        this.maxBatchSizeInBytes = maxBatchSizeInBytes;
        this.maxBatchCount = maxBatchCount;
        this.curBatchCount = 0;
        this.curBatchSizeInBytes = 0;
        this.reservedAttempts = EvictingQueue.create(maxReservedAttempts);
        this.attemptCount = 0;
    }

    public ListenableFuture<Result> tryAppend(LogItem logItem, int logSizeInBytes, Callback callback) {
        if (!hasRoomFor(logSizeInBytes, 1)) {
            return null;
        } else {
            SettableFuture<Result> future = SettableFuture.create();
            logItems.add(logItem);
            thunks.add(new Thunk(callback, future));
            curBatchCount++;
            curBatchSizeInBytes += logSizeInBytes;
            return future;
        }
    }

    public void appendAttempt(Attempt attempt) {
        reservedAttempts.add(attempt);
        this.attemptCount++;
    }

    public boolean isFull() {
        return curBatchSizeInBytes >= maxBatchSizeInBytes && curBatchCount >= maxBatchCount;
    }

    public long remainingMs(long nowMs, long lingerMs) {
        return lingerMs - createdTimeMs(nowMs);
    }

    public void fireCallbacksAndSetFutures() {
        List<Attempt> attempts = new ArrayList<Attempt>(reservedAttempts);
        Attempt attempt = Iterables.getLast(attempts);
        Result result = new Result(
                groupKey.getProject(),
                groupKey.getLogStore(),
                attempt.isSuccess(),
                attempt.getErrorCode(),
                attempt.getErrorMessage(),
                attempts,
                attemptCount);
        fireCallbacks(result);
        setFutures(result);
    }

    public GroupKey getGroupKey() {
        return groupKey;
    }

    public String getPackageId() {
        return packageId;
    }

    public List<LogItem> getLogItems() {
        return logItems;
    }

    public long getNextRetryMs() {
        return nextRetryMs;
    }

    public void setNextRetryMs(long nextRetryMs) {
        this.nextRetryMs = nextRetryMs;
    }

    public String getProject() {
        return groupKey.getProject();
    }

    public String getLogStore() {
        return groupKey.getLogStore();
    }

    public String getTopic() {
        return groupKey.getTopic();
    }

    public String getSource() {
        return groupKey.getSource();
    }

    public String getShardHash() {
        return groupKey.getShardHash();
    }

    public int getCurBatchSizeInBytes() {
        return curBatchSizeInBytes;
    }

    public int getRetries() {
        return Math.max(0, attemptCount - 1);
    }

    private boolean hasRoomFor(int sizeInBytes, int count) {
        return curBatchSizeInBytes + sizeInBytes <= maxBatchSizeInBytes
                && curBatchCount + count <= maxBatchCount;
    }

    private long createdTimeMs(long nowMs) {
        return Math.max(0, nowMs - createdMs);
    }

    private void fireCallbacks(Result result) {
        for (Thunk thunk : thunks) {
            try {
                if (thunk.callback != null)
                    thunk.callback.onCompletion(result);
            } catch (Exception e) {
                LOGGER.error("Failed to execute user-provided callback, groupKey={}, e=", groupKey, e);
            }
        }
    }

    private void setFutures(Result result) {
        for (Thunk thunk : thunks) {
            try {
                if (result.isSuccessful()) {
                    thunk.future.set(result);
                } else {
                    thunk.future.setException(new ResultFailedException(result));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to set future, groupKey={}, e=", groupKey, e);
            }
        }
    }

    @Override
    public long getDelay(@Nonnull TimeUnit unit) {
        return unit.convert(nextRetryMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@Nonnull Delayed o) {
        return (int) (nextRetryMs - ((ProducerBatch) o).getNextRetryMs());
    }

    @Override
    public String toString() {
        return "ProducerBatch{" +
                "groupKey=" + groupKey +
                ", packageId='" + packageId + '\'' +
                ", maxBatchSizeInBytes=" + maxBatchSizeInBytes +
                ", maxBatchCount=" + maxBatchCount +
                ", logItems=" + logItems +
                ", thunks=" + thunks +
                ", createdMs=" + createdMs +
                ", nextRetryMs=" + nextRetryMs +
                ", curBatchSizeInBytes=" + curBatchSizeInBytes +
                ", curBatchCount=" + curBatchCount +
                ", reservedAttempts=" + reservedAttempts +
                ", attemptCount=" + attemptCount +
                '}';
    }

    final private static class Thunk {
        final Callback callback;

        final SettableFuture<Result> future;

        Thunk(Callback callback, SettableFuture<Result> future) {
            this.callback = callback;
            this.future = future;
        }
    }

}
