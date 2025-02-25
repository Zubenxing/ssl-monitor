package com.sslmonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "domains")
public class Domain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String domainName;

    @Column
    private LocalDateTime certificateExpiryDate;

    @Column
    private String certificateStatus;

    @Column
    private LocalDateTime lastChecked;

    @Column
    private LocalDateTime lastRenewal;

    @Column
    private boolean autoRenewal = true;

    @Column(length = 2048)
    private String certificateDetails;
} 