package com.smartnews.engineering.push.sender.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.smartnews.engineering.push.models.SQSMessage;
import com.smartnews.engineering.push.models.UserData;
import com.smartnews.engineering.push.sender.Sender;
import com.smartnews.engineering.push.utils.ApnsUtils;
import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.DeliveryPriority;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import io.netty.util.concurrent.Future;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FixedSender1 implements Sender {
    private final ExecutorService sendWorker;
    private final ExecutorService closeWorker;
    private final CountDownLatch countDownLatch;


    public FixedSender1(int sendConcurrency, CountDownLatch countDownLatch) {
        this.sendWorker = Executors.newFixedThreadPool(sendConcurrency, new ThreadFactoryBuilder()
                .setNameFormat("fixed-sender-1-send-%d")
                .build()
        );
        this.closeWorker = Executors.newFixedThreadPool(sendConcurrency, new ThreadFactoryBuilder()
                .setNameFormat("fixed-sender-1-close-%d")
                .build()
        );
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void destroy() {
        Arrays.asList(this.sendWorker, this.closeWorker).forEach(worker -> {
            if (worker == null) {
                return;
            }
            worker.shutdown();
            try {
                worker.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                worker.shutdownNow();
            }
        });
    }

    @Override
    public void sendAsync(SQSMessage sqsMessage) {
        ApnsClient client = ApnsUtils.generateApnsClient();
        if (client == null) {
            return;
        }

        List<UserData> dataList = sqsMessage.getDataList();
        CountDownLatch latchForClose = new CountDownLatch(dataList.size());
        dataList.forEach(data -> sendWorker.submit(() -> doSend(client, data, latchForClose)));

        // Close client after all the messages are sent.
        closeWorker.submit(() -> {
            try {
                latchForClose.await(3, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                System.out.println(sqsMessage);
                Thread.currentThread().interrupt();
            }

            try {
                client.close().await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void doSend(ApnsClient client, UserData data, CountDownLatch latchForClose) {
        String pushToken = String.format("dummy_for_%s", data);
        String payload = String.format("test for %s", data);
        String collapseKey = String.format("collapse_key_for_%s", data);

        // Start sending a notification to APNs
        Future<PushNotificationResponse<SimpleApnsPushNotification>> future = client.sendNotification(
                new SimpleApnsPushNotification(
                        pushToken,
                        ApnsUtils.APNS_TOPIC,
                        payload,
                        null,
                        DeliveryPriority.IMMEDIATE,
                        collapseKey
                )
        );

        // Add listener to handle the result
        future.addListener(completedFuture -> {
            System.out.println(data);
            countDownLatch.countDown();
            latchForClose.countDown();
            Object response = completedFuture.getNow();
            // Do post process with response
        });
    }
}
