// src/main/java/com/system/complaints/dto/BulkVisitScheduleDTOs.java
package com.system.complaints.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BulkVisitScheduleRequest {

    @NotEmpty(message = "entries cannot be empty")
    private List<Entry> entries;

    // Optional, yyyy-MM-dd
    private String scheduleDate;

    // Optional – if provided, we’ll also set visitorName (resolved if missing)
    private Long visitorId;

    // Optional – if visitorId is absent you can pass name only
    private String visitorName;

    // -------- getters/setters ----------
    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> entries) { this.entries = entries; }

    public String getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(String scheduleDate) { this.scheduleDate = scheduleDate; }

    public Long getVisitorId() { return visitorId; }
    public void setVisitorId(Long visitorId) { this.visitorId = visitorId; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    // ---------- inner class -------------
    public static class Entry {

        @NotNull(message = "bankName is required")
        private String bankName;

        @NotNull(message = "branchCode is required")
        private String branchCode;

        // from file, per row
        private String visitorName;

        // from file, per row (yyyy-MM-dd)
        private String scheduleDate;

        public String getBankName() { return bankName; }
        public void setBankName(String bankName) { this.bankName = bankName; }

        public String getBranchCode() { return branchCode; }
        public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

        public String getVisitorName() { return visitorName; }
        public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

        public String getScheduleDate() { return scheduleDate; }
        public void setScheduleDate(String scheduleDate) { this.scheduleDate = scheduleDate; }
    }

}
