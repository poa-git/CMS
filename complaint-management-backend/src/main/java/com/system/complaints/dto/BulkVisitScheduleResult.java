// src/main/java/com/system/complaints/dto/BulkVisitScheduleResult.java
package com.system.complaints.dto;

import java.util.List;

public class BulkVisitScheduleResult {

    private int updatedCount;
    private List<Skip> skipped;

    public BulkVisitScheduleResult(int updatedCount, List<Skip> skipped) {
        this.updatedCount = updatedCount;
        this.skipped = skipped;
    }

    public int getUpdatedCount() { return updatedCount; }
    public List<Skip> getSkipped() { return skipped; }

    public static class Skip {
        private final String bankName;
        private final String branchCode;
        private final String reason;

        public Skip(String bankName, String branchCode, String reason) {
            this.bankName = bankName;
            this.branchCode = branchCode;
            this.reason = reason;
        }

        public String getBankName() { return bankName; }
        public String getBranchCode() { return branchCode; }
        public String getReason() { return reason; }
    }
}
