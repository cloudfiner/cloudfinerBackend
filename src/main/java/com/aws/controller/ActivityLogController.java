package com.aws.controller;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aws.entity.ActivityLog;
import com.aws.service.ActivityLogService;


@RestController
@RequestMapping("/activitylog")
public class ActivityLogController {

    @Autowired
    ActivityLogService service;

    @GetMapping("/admin/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ActivityLog> getLogs() {
        return service.getAllLogs(); //  correct
    }
}