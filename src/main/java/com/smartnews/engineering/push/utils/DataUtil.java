package com.smartnews.engineering.push.utils;

import com.smartnews.engineering.push.models.SQSMessage;
import com.smartnews.engineering.push.models.UserData;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataUtil {
    public static Collection<List<SQSMessage>> generateDummyData(
            int dequeueConcurrency,
            int numberOfSQSMessages,
            int numberOfDataInMessage
    ) {
        return IntStream.range(0, numberOfSQSMessages)
                .mapToObj(id -> {
                    SQSMessage message = new SQSMessage();
                    message.setId(id);
                    message.setDataList(generateUserDataList(id, numberOfDataInMessage));
                    return message;
                })
                .collect(Collectors.groupingBy(
                        message -> message.hashCode() % dequeueConcurrency,
                        Collectors.mapping(
                                Function.identity(),
                                Collectors.toList()
                        )
                ))
                .values();
    }

    private static List<UserData> generateUserDataList(int parentId, int count) {
        return IntStream.range(0, count)
                .mapToObj(dataId -> {
                    UserData data = new UserData();
                    data.setParentId(parentId);
                    data.setId(dataId);
                    return data;
                })
                .collect(Collectors.toList());
    }
}
