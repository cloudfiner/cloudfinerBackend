package com.aws.service;

import com.aws.dto.AlertResponseDto;
import com.aws.entity.AlertConfig;

import java.util.List;
import java.util.UUID;

public interface AlertConfigService  {

	AlertResponseDto  createAlert(double threshold);

	 List<AlertResponseDto> getUserAlerts(String type);
	    void deleteAlert(UUID alertId);

	    AlertResponseDto toggleAlert(UUID alertId);
}