package com.aws.repository;


import com.aws.entity.AlertRule;
import com.aws.entity.User;
import com.aws.template.AlertType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    // 🔥 User specific rules
    List<AlertRule> findByTypeAndUserAndActiveTrue(AlertType type, User user);

    // 🔥 Global rules (optional future use)
    List<AlertRule> findByTypeAndActiveTrue(AlertType type);

    // 🔥 Admin filtering
    List<AlertRule> findByUser(User user);

    List<AlertRule> findByActiveTrue();

}