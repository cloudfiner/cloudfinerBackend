package com.aws.dto;

import java.io.Serializable;

import com.aws.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlertEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private User user;
    private String templateKey;
    private Object value;
    private String priority;
}