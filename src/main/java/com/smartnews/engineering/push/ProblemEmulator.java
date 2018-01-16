package com.smartnews.engineering.push;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.smartnews.engineering.push.models.SQSMessage;
import com.smartnews.engineering.push.models.SenderType;
import com.smartnews.engineering.push.sender.Sender;
import com.smartnews.engineering.push.sender.impl.FixedSender1;
import com.smartnews.engineering.push.sender.impl.FixedSender2;
import com.smartnews.engineering.push.sender.impl.WithProblemSender;
import com.smartnews.engineering.push.utils.DataUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProblemEmulator {
    private static final int DEQUE_SIZE = 10;
    private static final int DEQUE_CONCURRENCY = 5;
    private static final int SEND_CONCURRENCY = 200;

    public void emulate(SenderType senderType, int numberOfSQSMessages, int numberOfDataInMessage) {
        // Set up workers
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(SEND_CONCURRENCY));
        ExecutorService dequeueWorker = Executors.newFixedThreadPool(
                DEQUE_CONCURRENCY,
                new ThreadFactoryBuilder().setNameFormat("dequeue-%d").build()
        );

        // Generate data for each dequeue worker
        Collection<List<SQSMessage>> messagesForEachDequeueWorker = DataUtil.generateDummyData(
                DEQUE_CONCURRENCY,
                numberOfSQSMessages,
                numberOfDataInMessage
        );

        // Initialize sender. To use CountDownLatch here is a naive solution to wait all the tasks.
        CountDownLatch countDownLatch = new CountDownLatch(numberOfSQSMessages * numberOfDataInMessage);
        Sender sender = createSender(senderType, countDownLatch);
        if (sender == null) {
            return;
        }

        // Emulate each dequeue task
        for (List<SQSMessage> messages : messagesForEachDequeueWorker) {
            dequeueWorker.submit(() -> {
                // Each 10 messages are dequeued at once.
                for (List<SQSMessage> dequeuedMessages : Lists.partition(messages, DEQUE_SIZE)) {
                    // Emulate SQS network delay
                    try {
                        Thread.sleep((long) (1 + Math.random() * 10));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // Each queue message is processed by sender synchronously
                    dequeuedMessages.forEach(sender::sendAsync);
                }
            });
        }


        // Wait all the jobs to be completed.
        try {
            countDownLatch.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Finalize
        sender.destroy();
        dequeueWorker.shutdown();
        try {
            dequeueWorker.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            dequeueWorker.shutdownNow();
        }
    }

    private static Sender createSender(
            SenderType senderType,
            CountDownLatch countDownLatch) {
        if (senderType == null) {
            return null;
        }

        switch (senderType) {
            case WITH_PROBLEM:
                return new WithProblemSender(SEND_CONCURRENCY, countDownLatch);
            case FIXED1:
                return new FixedSender1(SEND_CONCURRENCY, countDownLatch);
            case FIXED2:
                return new FixedSender2(SEND_CONCURRENCY, countDownLatch);
            default:
                return null;
        }
    }
}
