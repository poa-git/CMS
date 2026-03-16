package com.system.complaints.dto;

import java.util.Date;

public interface ComplaintBranchGroupProjection {
    String getBankName();
    String getBranchCode();
    String getBranchName();
    Long getComplaintId();
    String getComplaintStatus();
    String getCity();
    Date getDate();
    Date getApprovedDate();
    Date getClosedDate();
    Date getQuotationDate();
    Date getPendingForClosedDate();
    String getVisitorName();
    Boolean getPriority();
    Boolean getMarkedInPool();
    String getReportType();
    String getCourierStatus();
    String getEquipmentDescription();
}

