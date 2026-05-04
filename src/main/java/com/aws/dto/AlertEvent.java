package com.aws.dto;


import com.aws.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlertEvent {

    private User user;
    private String templateKey;
    private Object value;
    private String priority;
}