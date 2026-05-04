package com.aws.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import com.aws.entity.ActivityLog;
import com.aws.repository.ActivityLogRepository;
import com.aws.service.ActivityLogService;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    @Autowired
    private ActivityLogRepository repository;

    @Override
    public void log(String action, String email, String details) {

        ActivityLog log = new ActivityLog();
        log.setAction(action);
        log.setEmail(email);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());

        repository.save(log);
    }

    // ✅ NEW METHOD
    @Override
    public List<ActivityLog> getAllLogs() {
    	return repository.findAll(
    		    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"))
    		).getContent();
    }
}