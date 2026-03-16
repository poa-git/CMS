package com.system.complaints.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
@Entity
@Table(name = "complaints_log")
public class ComplaintLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String complaintId;

    private Date date;
    private String bankName;
    private String branchCode;
    private String branchName;
    private String city;
    private String referenceNumber;
    private String complaintStatus;
    private String visitorName;

    @Column(name = "visitor_id")
    private Long visitorId; // New field for visitor ID

    @Column(name = "staff_remarks", length = 255)
    private String staffRemarks; // New field for staff remarks

    private Date closedDate;
    private Date pendingForClosedDate;
    private Boolean repeatComplaint;
    private Date quotationDate;
    private Date approvedDate;
    private Date focDate;
    private Date scheduleDate;
    private Date hardwarePickedDate;
    private String closedStatus;
    private String specialRemarks;
    private String approvalRemarks;
    private Integer agingDays;
    @Column(columnDefinition = "TEXT")
    private String details;
    private String complaintType;
    private String loggedBy; // New field to store the username of the person who logged the complaint
    @Column(name = "dc_generated")
    private Boolean dcGenerated = false;
    @Column(name = "job_card_path", length = 255)
    private String jobCardPath; // New field for storing job card path
    @Column(name = "is_priority")
    private Boolean isPriority = false; // Default to false
    @Column(name = "is_marked_in_pool")
    private Boolean isMarkedInPool = false; // default to false

    @OneToMany(mappedBy = "complaintLog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)   // <- key line: loads all parts for the page in one extra query
    @BatchSize(size = 500)        // optional, helps elsewhere
    private Set<HardwarePart> hardwareParts = new HashSet<>();

    /**
     * Optional: This @Transient field won't be persisted to the DB.
     * You can store hardware logs or courier status here as needed.
     */
    @Transient
    private List<HardwareLog> hardwareLogs;
    // If you only want a single field (e.g., one courierStatus), you could do:
     @Transient
     private String courierStatus;

    @Transient
    private String report; // Adding the `report` field as a transient field, similar to `courierStatus`

    @Transient
    private String equipmentDescription; // Equipment description

    // Generate complaintId before saving
    @PrePersist
    protected void generateComplaintId() {
        if (complaintId == null || complaintId.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String dateTime = LocalDateTime.now().format(formatter);
            complaintId = "QMS-" + dateTime;
        }
    }

    // ----------------------------
    // Getters and Setters
    // ----------------------------

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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getComplaintStatus() {
        return complaintStatus;
    }

    public void setComplaintStatus(String complaintStatus) {
        this.complaintStatus = complaintStatus;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public String getStaffRemarks() {
        return staffRemarks;
    }

    public void setStaffRemarks(String staffRemarks) {
        this.staffRemarks = staffRemarks;
    }

    public Date getClosedDate() {
        return closedDate;
    }

    public Date getFocDate() {
        return focDate;
    }

    public void setFocDate(Date focDate) {
        this.focDate = focDate;
    }

    public void setClosedDate(Date closedDate) {
        this.closedDate = closedDate;
    }

    public Boolean getRepeatComplaint() {
        return repeatComplaint;
    }

    public void setRepeatComplaint(Boolean repeatComplaint) {
        this.repeatComplaint = repeatComplaint;
    }

    public Date getQuotationDate() {
        return quotationDate;
    }

    public void setQuotationDate(Date quotationDate) {
        this.quotationDate = quotationDate;
    }

    public Date getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(Date approvedDate) {
        this.approvedDate = approvedDate;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public Date getHardwarePickedDate() {
        return hardwarePickedDate;
    }

    public void setHardwarePickedDate(Date hardwarePickedDate) {
        this.hardwarePickedDate = hardwarePickedDate;
    }

    public Date getPendingForClosedDate() {
        return pendingForClosedDate;
    }

    public void setPendingForClosedDate(Date pendingForClosedDate) {
        this.pendingForClosedDate = pendingForClosedDate;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getClosedStatus() {
        return closedStatus;
    }

    public void setClosedStatus(String closedStatus) {
        this.closedStatus = closedStatus;
    }

    public String getSpecialRemarks() {
        return specialRemarks;
    }

    public void setSpecialRemarks(String specialRemarks) {
        this.specialRemarks = specialRemarks;
    }

    public String getApprovalRemarks() {
        return approvalRemarks;
    }

    public void setApprovalRemarks(String approvalRemarks) {
        this.approvalRemarks = approvalRemarks;
    }

    public Integer getAgingDays() {
        return agingDays;
    }

    public void setAgingDays(Integer agingDays) {
        this.agingDays = agingDays;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getComplaintType() {
        return complaintType;
    }

    public void setComplaintType(String complaintType) {
        this.complaintType = complaintType;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public String getJobCardPath() {
        return jobCardPath;
    }

    public void setJobCardPath(String jobCardPath) {
        this.jobCardPath = jobCardPath;
    }

    // Transient field getters and setters  
    public List<HardwareLog> getHardwareLogs() {
        return hardwareLogs;
    }

    public void setHardwareLogs(List<HardwareLog> hardwareLogs) {
        this.hardwareLogs = hardwareLogs;
    }

    // If you use a single courierStatus field:
     public String getCourierStatus() {
         return courierStatus;
     }

     public void setCourierStatus(String courierStatus) {
         this.courierStatus = courierStatus;
     }

    public String getReport() {
        return report;
    }

    public String getEquipmentDescription() {
        return equipmentDescription;
    }

    public void setEquipmentDescription(String equipmentDescription) {
        this.equipmentDescription = equipmentDescription;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public Boolean getDcGenerated() {
        return dcGenerated;
    }

    public void setDcGenerated(Boolean dcGenerated) {
        this.dcGenerated = dcGenerated;
    }

    public Boolean getPriority() {
        return isPriority;
    }

    public void setPriority(Boolean priority) {
        isPriority = priority;
    }

    public Boolean getMarkedInPool() {
        return isMarkedInPool;
    }

    public void setMarkedInPool(Boolean markedInPool) {
        isMarkedInPool = markedInPool;
    }

    public Set<HardwarePart> getHardwareParts() {
        return hardwareParts;
    }

    public void setHardwareParts(Set<HardwarePart> hardwareParts) {
        this.hardwareParts = hardwareParts;
    }
    public void addHardwarePart(HardwarePart part) {
        hardwareParts.add(part);
        part.setComplaintLog(this);
    }
    public void removeHardwarePart(HardwarePart part) {
        hardwareParts.remove(part);
        part.setComplaintLog(null);
    }

}
