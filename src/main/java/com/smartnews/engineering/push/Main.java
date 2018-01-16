package com.smartnews.engineering.push;

import com.smartnews.engineering.push.models.SenderType;
import org.apache.commons.lang3.EnumUtils;

public class Main {
    public static void main(String[] args) {
        int numberOfSQSMessages = Integer.parseInt(args[0]);
        int numberOfDataInMessage = Integer.parseInt(args[1]);
        SenderType senderType = EnumUtils.getEnum(SenderType.class, args[2]);

        new ProblemEmulator().emulate(senderType, numberOfSQSMessages, numberOfDataInMessage);
    }
}
