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

public class WithProblemSender implements Sender {
    private final ExecutorService sendWorker;
    private final CountDownLatch countDownLatch;

    public WithProblemSender(int sendConcurrency, CountDownLatch countDownLatch) {
        this.sendWorker = Executors.newFixedThreadPool(sendConcurrency, new ThreadFactoryBuilder()
                .setNameFormat("with-problem-sender-send-%d")
                .build()
        );
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
    }

    @Override
    public void sendAsync(SQSMessage sqsMessage) {
        sendWorker.submit(() -> {
            ApnsClient client = ApnsUtils.generateApnsClient();
            if (client == null) {
                return;
            }

            List<UserData> dataList = sqsMessage.getDataList();
            dataList.parallelStream().forEach(data -> doSend(client, data));

            try {
                client.close().await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Error handling
            }
        });
    }

    private void doSend(ApnsClient client, UserData data) {
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

        try {
            // Get the result from APNs
            PushNotificationResponse<SimpleApnsPushNotification> response = future.get(
                    ApnsUtils.APNS_TIMEOUT,
                    TimeUnit.SECONDS
            );
            // Do post process with response
        } catch (Exception e) {
            // Do error handling
        } finally {
            System.out.println(data);
            countDownLatch.countDown();
        }
    }
}
