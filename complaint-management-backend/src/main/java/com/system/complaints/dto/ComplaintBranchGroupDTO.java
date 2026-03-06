package com.system.complaints.dto;

import com.system.complaints.model.ComplaintLog;
import java.util.List;

public class ComplaintBranchGroupDTO {
    private String bankName;
    private String branchCode;
    private String branchName;
    private List<ComplaintLog> complaints;

    public ComplaintBranchGroupDTO() {}

    public ComplaintBranchGroupDTO(String bankName, String branchCode, String branchName, List<ComplaintLog> complaints) {
        this.bankName = bankName;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.complaints = complaints;
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

    public List<ComplaintLog> getComplaints() {
        return complaints;
    }

    public void setComplaints(List<ComplaintLog> complaints) {
        this.complaints = complaints;
    }
}
