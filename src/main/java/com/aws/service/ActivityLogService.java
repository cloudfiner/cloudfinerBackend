package com.aws.service;

import java.util.List;

import com.aws.entity.ActivityLog;

public interface ActivityLogService {
	 void log(String action, String email, String details);

	    List<ActivityLog> getAllLogs(); 
}