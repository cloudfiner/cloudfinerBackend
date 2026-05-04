package com.aws.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.aws.entity.TelegramLink;
import com.aws.entity.User;

public interface TelegramLinkRepository extends JpaRepository<TelegramLink, UUID> {
  
    Optional<TelegramLink> findByToken(String token);

    Optional<TelegramLink> findTopByUserAndUsedTrueOrderByIdDesc(User user);

    // FIXED DELETE QUERY
    @Modifying
    @Transactional
    void deleteByUserAndUsedFalse(User user);

    boolean existsByUserAndChatIdIsNotNullAndUsedTrue(User user);

    List<TelegramLink> findAllByUser(User user);
    
    @Query("SELECT t FROM TelegramLink t WHERE LOWER(TRIM(t.token)) = LOWER(TRIM(:token))")
    Optional<TelegramLink> findByTokenSafe(String token);
}