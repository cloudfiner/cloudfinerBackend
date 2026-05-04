package com.aws.repository;



import com.aws.entity.AlertConfig;
import com.aws.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertConfig, UUID> {

    List<AlertConfig> findByUser(User user);

	Page<AlertConfig> findByUserAndActiveTrue(User user, PageRequest of);
}