package com.aws.service;

public interface TemplateService {
	
	 public String buildMessage(String key, Object... args);
	 public String getPriority(String key);

}
