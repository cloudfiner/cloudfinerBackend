package com.aws.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String action;   // LOGIN, DELETE_USER, ACTIVATE_USER

    private String email;    // kisne kiya

    private String details;  // extra info

    private LocalDateTime timestamp;
}