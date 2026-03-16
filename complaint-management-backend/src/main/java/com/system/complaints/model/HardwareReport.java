package com.system.complaints.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.sql.Timestamp;

@Entity
@Table(name = "hardware_reports")
public class HardwareReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to HardwareLog
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hardware_log_id", nullable = false)
    @JsonIgnoreProperties({"reports"})
    private HardwareLog hardwareLog;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME")
    private Timestamp createdAt;


    // Constructors
    public HardwareReport() {
    }

    public HardwareReport(String content, String createdBy, HardwareLog hardwareLog) {
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.hardwareLog = hardwareLog;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public HardwareLog getHardwareLog() {
        return hardwareLog;
    }

    public void setHardwareLog(HardwareLog hardwareLog) {
        this.hardwareLog = hardwareLog;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content; 
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
