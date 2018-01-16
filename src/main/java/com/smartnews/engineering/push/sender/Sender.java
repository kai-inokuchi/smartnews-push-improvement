package com.smartnews.engineering.push.sender;

import com.smartnews.engineering.push.models.SQSMessage;

public interface Sender {
    void sendAsync(SQSMessage sqsMessage);

    void destroy();
}
