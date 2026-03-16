package com.system.complaints.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String complaintId;

    @Column(name = "scheduled_for", columnDefinition = "date")
    @Temporal(TemporalType.DATE)
    private Date scheduledFor;


    private Boolean successful;

    @Column(name = "scheduled_at", columnDefinition = "datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledAt;

    @Column(name = "completed_at", columnDefinition = "datetime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    private String outcomeStatus;
    private String performedBy;

    @PrePersist
    protected void onCreate() {
        scheduledAt = new Date();
    }

    // getters/setters...


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(String complaintId) {
        this.complaintId = complaintId;
    }

    public Date getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(Date scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public Boolean getSuccessful() {
        return successful;
    }

    public void setSuccessful(Boolean successful) {
        this.successful = successful;
    }

    public Date getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Date scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public String getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(String outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}
