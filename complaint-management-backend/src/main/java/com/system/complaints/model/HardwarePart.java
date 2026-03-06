package com.system.complaints.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "hardware_part")
public class HardwarePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CHANGED: Reference ComplaintLog directly
    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    @JsonIgnoreProperties("hardwareParts")
    private ComplaintLog complaintLog;

    private String hardwareName;
    private Boolean availableWithEngineer = false;
    private Boolean repaired = false;

    @Column(name = "assigned_engineer")
    private String assignedEngineer;

    @Column(name = "not_repairable")
    private Boolean notRepairable = false;

    @Column(name = "status")
    private String status; // e.g. "pending", "accepted", "repaired", "not_repairable"

    @Column(name = "assigned_at", columnDefinition = "datetime")
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at", columnDefinition = "datetime")
    private LocalDateTime acceptedAt;

    @Column(name = "repaired_at", columnDefinition = "datetime")
    private LocalDateTime repairedAt;

    @Column(name = "not_repairable_at", columnDefinition = "datetime")
    private LocalDateTime notRepairableAt;

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ComplaintLog getComplaintLog() { return complaintLog; }
    public void setComplaintLog(ComplaintLog complaintLog) { this.complaintLog = complaintLog; }

    public String getHardwareName() { return hardwareName; }
    public void setHardwareName(String hardwareName) { this.hardwareName = hardwareName; }

    public Boolean getAvailableWithEngineer() { return availableWithEngineer; }
    public void setAvailableWithEngineer(Boolean availableWithEngineer) { this.availableWithEngineer = availableWithEngineer; }

    public Boolean getRepaired() { return repaired; }
    public void setRepaired(Boolean repaired) { this.repaired = repaired; }

    public String getAssignedEngineer() { return assignedEngineer; }
    public void setAssignedEngineer(String assignedEngineer) { this.assignedEngineer = assignedEngineer; }

    public Boolean getNotRepairable() { return notRepairable; }
    public void setNotRepairable(Boolean notRepairable) { this.notRepairable = notRepairable; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getRepairedAt() { return repairedAt; }
    public void setRepairedAt(LocalDateTime repairedAt) { this.repairedAt = repairedAt; }

    public LocalDateTime getNotRepairableAt() { return notRepairableAt; }
    public void setNotRepairableAt(LocalDateTime notRepairableAt) { this.notRepairableAt = notRepairableAt; }
}
