package com.aws.controller;

import com.aws.entity.NotificationTemplate;
import com.aws.repository.NotificationTemplateRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/templates")
@PreAuthorize("hasRole('ADMIN')")
public class TemplateAdminController {

    @Autowired
    private NotificationTemplateRepository repository;

    // CREATE
    @PostMapping
    public NotificationTemplate create(@RequestBody NotificationTemplate t) {
        return repository.save(t);
    }

    // READ ALL
    @GetMapping
    public List<NotificationTemplate> getAll() {
        return repository.findAll();
    }

    // UPDATE
    @PutMapping("/{id}")
    public NotificationTemplate update(@PathVariable UUID id,
                                       @RequestBody NotificationTemplate t) {

        NotificationTemplate existing = repository.findById(id)
                .orElseThrow();

        existing.setKey(t.getKey());
        existing.setTemplate(t.getTemplate());
        existing.setDefaultPriority(t.getDefaultPriority());

        return repository.save(existing);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        repository.deleteById(id);
    }
}