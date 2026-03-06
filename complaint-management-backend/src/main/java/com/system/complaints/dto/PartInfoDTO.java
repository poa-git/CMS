package com.system.complaints.dto;

public class PartInfoDTO {
    private String hardwareName;
    private String branchCode;
    private String bankName;
    private String branchName;

    public PartInfoDTO(String hardwareName, String branchCode, String bankName, String branchName) {
        this.hardwareName = hardwareName;
        this.branchCode = branchCode;
        this.bankName = bankName;
        this.branchName = branchName;
    }

    // Getters only (immutable DTO is fine)
    public String getHardwareName() { return hardwareName; }
    public String getBranchCode() { return branchCode; }
    public String getBankName() { return bankName; }
    public String getBranchName() { return branchName; }
}
