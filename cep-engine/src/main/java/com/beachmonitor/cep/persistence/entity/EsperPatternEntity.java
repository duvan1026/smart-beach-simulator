package com.beachmonitor.cep.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "esper_patterns", schema = "beach_monitor")
public class EsperPatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_id", nullable = false, unique = true)
    private String patternId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "epl_statement", nullable = false, columnDefinition = "TEXT")
    private String eplStatement;

    @Column(name = "alert_level", nullable = false)
    private String alertLevel;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getPatternId() { return patternId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getEplStatement() { return eplStatement; }
    public String getAlertLevel() { return alertLevel; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
