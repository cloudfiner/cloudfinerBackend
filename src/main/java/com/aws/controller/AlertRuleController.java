package com.aws.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.aws.entity.AlertRule;
import com.aws.repository.AlertRuleRepository;

@RestController
@RequestMapping("/api/admin/rules")
public class AlertRuleController {

    @Autowired
    private AlertRuleRepository repo;

    @PostMapping
    public AlertRule create(@RequestBody AlertRule r) {
        return repo.save(r);
    }

    @GetMapping
    public List<AlertRule> getAll() {
        return repo.findAll();
    }

    @PutMapping("/{id}")
    public AlertRule update(@PathVariable UUID id, @RequestBody AlertRule r) {

        AlertRule existing = repo.findById(id).orElseThrow();

        existing.setThreshold(r.getThreshold());
        existing.setCondition(r.getCondition());
        existing.setTemplateKey(r.getTemplateKey());
        existing.setPriority(r.getPriority());

        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        repo.deleteById(id);
    }
}