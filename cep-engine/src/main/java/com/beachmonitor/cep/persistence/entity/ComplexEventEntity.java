package com.beachmonitor.cep.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "complex_events", schema = "beach_monitor")
public class ComplexEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_id", nullable = false)
    private String patternId;

    @Column(name = "pattern_name", nullable = false)
    private String patternName;

    @Column(name = "alert_level", nullable = false)
    private String alertLevel;

    @Column(name = "beach_id", nullable = false)
    private String beachId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    private String message;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt = LocalDateTime.now();

    public ComplexEventEntity() {}

    public ComplexEventEntity(String patternId, String patternName, String alertLevel,
                              String beachId, String details, String message) {
        this.patternId = patternId;
        this.patternName = patternName;
        this.alertLevel = alertLevel;
        this.beachId = beachId;
        this.details = details;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getPatternId() { return patternId; }
    public String getPatternName() { return patternName; }
    public String getAlertLevel() { return alertLevel; }
    public String getBeachId() { return beachId; }
    public String getDetails() { return details; }
    public String getMessage() { return message; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
}
