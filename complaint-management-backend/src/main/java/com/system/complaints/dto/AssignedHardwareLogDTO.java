package com.system.complaints.dto;

import java.sql.Date;

public class AssignedHardwareLogDTO {
    private final Long id;
    private final Boolean done;
    private final String courierStatus;
    private final String equipmentDescription;
    private final String extraHardware;
    private final Date dispatchInwardDate;
    private final Date receivedInwardDate;
    private final Date dispatchOutwardDate;
    private final Date receivedOutwardDate;
    private final Date hOkDate;
    private final String dispatchCnNumber;
    private final String receivingCnNumber;
    private final String labEngineer;
    private final String report;

    private final Long complaintLogId;
    private final String complaintId;
    private final String complaintStatus;
    private final String bankName;
    private final String branchName;
    private final String branchCode;
    private final String city;
    private final Boolean dcGenerated;

    public AssignedHardwareLogDTO(
            Long id,
            Boolean done,
            String courierStatus,
            String equipmentDescription,
            String extraHardware,
            Date dispatchInwardDate,
            Date receivedInwardDate,
            Date dispatchOutwardDate,
            Date receivedOutwardDate,
            Date hOkDate,
            String dispatchCnNumber,
            String receivingCnNumber,
            String labEngineer,
            String report,
            Long complaintLogId,
            String complaintId,
            String complaintStatus,
            String bankName,
            String branchName,
            String branchCode,
            String city,
            Boolean dcGenerated
    ) {
        this.id = id;
        this.done = done;
        this.courierStatus = courierStatus;
        this.equipmentDescription = equipmentDescription;
        this.extraHardware = extraHardware;
        this.dispatchInwardDate = dispatchInwardDate;
        this.receivedInwardDate = receivedInwardDate;
        this.dispatchOutwardDate = dispatchOutwardDate;
        this.receivedOutwardDate = receivedOutwardDate;
        this.hOkDate = hOkDate;
        this.dispatchCnNumber = dispatchCnNumber;
        this.receivingCnNumber = receivingCnNumber;
        this.labEngineer = labEngineer;
        this.report = report;
        this.complaintLogId = complaintLogId;
        this.complaintId = complaintId;
        this.complaintStatus = complaintStatus;
        this.bankName = bankName;
        this.branchName = branchName;
        this.branchCode = branchCode;
        this.city = city;
        this.dcGenerated = dcGenerated;
    }

    public Long getId() { return id; }
    public Boolean getDone() { return done; }
    public String getCourierStatus() { return courierStatus; }
    public String getEquipmentDescription() { return equipmentDescription; }
    public String getExtraHardware() { return extraHardware; }
    public Date getDispatchInwardDate() { return dispatchInwardDate; }
    public Date getReceivedInwardDate() { return receivedInwardDate; }
    public Date getDispatchOutwardDate() { return dispatchOutwardDate; }
    public Date getReceivedOutwardDate() { return receivedOutwardDate; }
    public Date getHOkDate() { return hOkDate; }
    public String getDispatchCnNumber() { return dispatchCnNumber; }
    public String getReceivingCnNumber() { return receivingCnNumber; }
    public String getLabEngineer() { return labEngineer; }
    public String getReport() { return report; }

    public Long getComplaintLogId() { return complaintLogId; }
    public String getComplaintId() { return complaintId; }
    public String getComplaintStatus() { return complaintStatus; }
    public String getBankName() { return bankName; }
    public String getBranchName() { return branchName; }
    public String getBranchCode() { return branchCode; }
    public String getCity() { return city; }
    public Boolean getDcGenerated() { return dcGenerated; }
}