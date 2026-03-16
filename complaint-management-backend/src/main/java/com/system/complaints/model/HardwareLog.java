package com.system.complaints.model;

import java.sql.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hardware_log")
public class HardwareLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    @JsonIgnoreProperties({"hardwareLogs"})
    private ComplaintLog complaintLog;

    // New one-to-many mapping for multiple reports
    @OneToMany(mappedBy = "hardwareLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("hardwareLog")
    private Set<HardwareReport> reports = new HashSet<>();

    // Incoming Hardware
    private Date dispatchInwardDate;   // Delivered to the office
    private Date receivedInwardDate;   // Received in the office

    // Outgoing Hardware
    private Date dispatchOutwardDate; // Dispatched from the office
    private Date receivedOutwardDate;  // Received at external location

    // CN Numbers for Tracking
    private String dispatchCnNumber;   // CN number for outgoing dispatch
    private String receivingCnNumber;  // CN number for incoming receiving

    private Date hOkDate; // Hardware OK date
    private String remarks; // Additional remarks
    private String equipmentDescription; // Equipment description
    private String problem; // Problem description
    private Date outOfStockDate ;
    // New fields
    private String extraHardware; // Extra Hardware
    private String labStatus; // Lab status
    private String courierStatus; // Courier status
    @Column(name = "lab_engineer")
    private String labEngineer;

    @Column(name = "done")
    private Boolean done = false;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ComplaintLog getComplaintLog() {
        return complaintLog;
    }

    public void setComplaintLog(ComplaintLog complaintLog) {
        this.complaintLog = complaintLog;
    }

    public Date getDispatchInwardDate() {
        return dispatchInwardDate;
    }

    public void setDispatchInwardDate(Date dispatchInwardDate) {
        this.dispatchInwardDate = dispatchInwardDate;
    }

    public Date getReceivedInwardDate() {
        return receivedInwardDate;
    }

    public void setReceivedInwardDate(Date receivedInwardDate) {
        this.receivedInwardDate = receivedInwardDate;
    }

    public Date getDispatchOutwardDate() {
        return dispatchOutwardDate;
    }

    public void setDispatchOutwardDate(Date dispatchOutwardDate) {
        this.dispatchOutwardDate = dispatchOutwardDate;
    }

    public Date getReceivedOutwardDate() {
        return receivedOutwardDate;
    }

    public void setReceivedOutwardDate(Date receivedOutwardDate) {
        this.receivedOutwardDate = receivedOutwardDate;
    }

    public String getDispatchCnNumber() {
        return dispatchCnNumber;
    }

    public void setDispatchCnNumber(String dispatchCnNumber) {
        this.dispatchCnNumber = dispatchCnNumber;
    }

    public String getReceivingCnNumber() {
        return receivingCnNumber;
    }

    public void setReceivingCnNumber(String receivingCnNumber) {
        this.receivingCnNumber = receivingCnNumber;
    }


    public Date getHOkDate() {
        return hOkDate;
    }

    public void setHOkDate(Date hOkDate) {
        this.hOkDate = hOkDate;
    }


    public Date getOutOfStockDate() {
        return outOfStockDate;
    }

    public void setOutOfStockDate(Date outOfStockDate) {
        this.outOfStockDate = outOfStockDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getEquipmentDescription() {
        return equipmentDescription;
    }

    public void setEquipmentDescription(String equipmentDescription) {
        this.equipmentDescription = equipmentDescription;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }


    public String getLabStatus() {
        return labStatus;
    }

    public void setLabStatus(String labStatus) {
        this.labStatus = labStatus;
    }

    public String getCourierStatus() {
        return courierStatus;
    }

    public void setCourierStatus(String courierStatus) {
        this.courierStatus = courierStatus;
    }

    public String getExtraHardware() {
        return extraHardware;
    }

    public void setExtraHardware(String extraHardware) {
        this.extraHardware = extraHardware;
    }

    public Set<HardwareReport> getReports() {
        return reports;
    }

    public void setReports(Set<HardwareReport> reports) {
        this.reports = reports;
    }


    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public String getLabEngineer() {
        return labEngineer;
    }

    public void setLabEngineer(String labEngineer) {
        this.labEngineer = labEngineer;
    }

    // Helper methods for managing the reports relationship
    public void addReport(HardwareReport report) {
        reports.add(report);
        report.setHardwareLog(this);
    }

    public void removeReport(HardwareReport report) {
        reports.remove(report);
        report.setHardwareLog(null);
    }

}
