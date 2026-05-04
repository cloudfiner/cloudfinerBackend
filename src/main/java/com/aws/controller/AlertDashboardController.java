package com.aws.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aws.entity.AlertExecution;
import com.aws.repository.AlertExecutionRepository;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AlertDashboardController {

    @Autowired
    private AlertExecutionRepository repo;

    @GetMapping("/stats")
    public Map<String, Object> stats() {

        long total = repo.count();

        long active = repo.findAll()
                .stream()
                .filter(AlertExecution::isLastState)
                .count();

        Map<String, Object> map = new HashMap();
        map.put("totalAlerts", total);
        map.put("activeAlerts", active);

        return map;
    }
}