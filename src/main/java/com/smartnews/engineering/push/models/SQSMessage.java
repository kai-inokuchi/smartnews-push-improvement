package com.smartnews.engineering.push.models;

import lombok.Data;

import java.util.List;

@Data
public class SQSMessage {
    private List<UserData> dataList;
    private int id;
}
