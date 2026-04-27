package com.beachmonitor.simulator.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "beaches", schema = "beach_monitor")
public class BeachEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beach_id", nullable = false, unique = true)
    private String beachId;

    private String name;
    private Double lat;
    private Double lon;
    private Boolean enabled;

    public String getBeachId() { return beachId; }
    public String getName() { return name; }
    public Boolean getEnabled() { return enabled; }
}
