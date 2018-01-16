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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FixedSender2 implements Sender {
    private final ExecutorService sendWorker;
    private final ApnsClient client;
    private final CountDownLatch countDownLatch;

    public FixedSender2(int sendConcurrency, CountDownLatch countDownLatch) {
        this.sendWorker = Executors.newFixedThreadPool(sendConcurrency, new ThreadFactoryBuilder()
                .setNameFormat("fixed-sender-2-send-%d")
                .build()
        );
        this.client = ApnsUtils.generateApnsClient();
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void destroy() {
        if (this.sendWorker != null) {
            this.sendWorker.shutdown();
            try {
                this.sendWorker.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.sendWorker.shutdownNow();
            }
        }

        if (this.client != null) {
            try {
                this.client.close().await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void sendAsync(SQSMessage sqsMessage) {
        if (client == null) {
            return;
        }

        List<UserData> dataList = sqsMessage.getDataList();
        dataList.forEach(data -> sendWorker.submit(() -> doSend(data)));
    }

    private void doSend(UserData data) {
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
            Object response = completedFuture.getNow();
            // Do post process with response
        });
    }
}
