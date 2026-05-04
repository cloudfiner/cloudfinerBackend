package com.aws.service;

import com.aws.entity.User;

public interface SmartNotificationService {
	
	 public void send(User user, String templateKey, Object... args) ;

}
