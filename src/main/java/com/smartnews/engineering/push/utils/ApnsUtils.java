package com.smartnews.engineering.push.utils;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.auth.ApnsSigningKey;

import java.io.InputStream;

public class ApnsUtils {
    public static final int APNS_TIMEOUT = 3;
    public static final String APNS_TOPIC = "xxx";
    private static final String APNS_KEY_FILE = "xxx";
    private static final String APNS_TEAM_ID = "xxx";
    private static final String APNS_KEY_ID = "xxx";

    public static ApnsClient generateApnsClient() {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(APNS_KEY_FILE)
        ) {
            return new ApnsClientBuilder()
                    .setSigningKey(ApnsSigningKey.loadFromInputStream(is, APNS_TEAM_ID, APNS_KEY_ID))
                    .setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
                    .build();
        } catch (Exception e) {
            // Error handling
            return null;
        }
    }
}
