package com.system.complaints.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pending_for_closed_log",
        indexes = {
                @Index(name = "idx_complaint_id", columnList = "complaint_id"),
                @Index(name = "idx_source", columnList = "source")
        }
)
public class PendingForClosedLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private String complaintId;

    @Column(nullable = false)
    private String source; // MOBILE or PORTAL

    @Column(name = "marked_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime markedAt;  // ✅ avoids datetime(6) issue

    @Column(name = "user_id")
    private Long userId; // optional — who triggered it

    @Column
    private String username; // for audit readability

    public PendingForClosedLog() {}

    public PendingForClosedLog(String complaintId, String source, Long userId, String username) {
        this.complaintId = complaintId;
        this.source = source;
        this.userId = userId;
        this.username = username;
        this.markedAt = LocalDateTime.now(); // ✅ clean modern API, no Timestamp
    }

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
