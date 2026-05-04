package com.aws.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.aws.entity.Notification;
import com.aws.entity.User;


@Repository
public interface NotificationRepository  extends JpaRepository<Notification, UUID> {
	

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadStatusFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.user = :user AND n.readStatus = false")
    void markAllAsRead(User user);

}
