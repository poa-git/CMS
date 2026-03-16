// src/main/java/com/system/complaints/service/BulkVisitSchedulingService.java
package com.system.complaints.service;

import com.system.complaints.dto.BulkVisitScheduleRequest;
import com.system.complaints.dto.BulkVisitScheduleResult;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.repository.ComplaintLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BulkVisitSchedulingService {

    private final ComplaintLogRepository complaintLogRepository;
    private final ComplaintLogService complaintLogService;
    private final VisitorService visitorService;

    private static final int BRANCH_CODE_LENGTH = 4;

    public BulkVisitSchedulingService(ComplaintLogRepository complaintLogRepository,
                                      ComplaintLogService complaintLogService,
                                      VisitorService visitorService) {
        this.complaintLogRepository = complaintLogRepository;
        this.complaintLogService = complaintLogService;
        this.visitorService = visitorService;
    }

    @Transactional
    public BulkVisitScheduleResult bulkVisitSchedule(BulkVisitScheduleRequest req) {
        int updated = 0;
        List<BulkVisitScheduleResult.Skip> skipped = new ArrayList<>();

        if (req == null || req.getEntries() == null || req.getEntries().isEmpty()) {
            return new BulkVisitScheduleResult(0, List.of(
                    new BulkVisitScheduleResult.Skip("", "", "No entries provided")
            ));
        }

        // ---------- Global values ----------
        String globalScheduleDate = safe(req.getScheduleDate());
        String resolvedVisitorName = safe(req.getVisitorName());

        if (resolvedVisitorName.isBlank() && req.getVisitorId() != null) {
            String vn = visitorService.findVisitorNameById(req.getVisitorId());
            if (vn != null) resolvedVisitorName = vn.trim();
        }

        // --------------------------------------------------------------------
        for (BulkVisitScheduleRequest.Entry e : req.getEntries()) {

            String bank = safe(e.getBankName());
            String codeRaw = safe(e.getBranchCode());

            if (bank.isEmpty() || codeRaw.isEmpty()) {
                skipped.add(new BulkVisitScheduleResult.Skip(bank, codeRaw, "Missing bankName/branchCode"));
                continue;
            }

            String branchCode = normalizeBranchCode(codeRaw);

            List<ComplaintLog> opens = complaintLogRepository
                    .findByBankNameIgnoreCaseAndBranchCodeAndComplaintStatus(bank, branchCode, "Open");

            if (opens == null || opens.isEmpty()) {
                skipped.add(new BulkVisitScheduleResult.Skip(bank, branchCode, "No Open complaints found"));
                continue;
            }

            // ---------- Row-level values ----------
            String rowVisitorName = safe(e.getVisitorName());
            String rowScheduleDate = safe(e.getScheduleDate());

            String effectiveVisitorName = !rowVisitorName.isEmpty()
                    ? rowVisitorName
                    : resolvedVisitorName;

            String effectiveScheduleDate = !rowScheduleDate.isEmpty()
                    ? rowScheduleDate
                    : globalScheduleDate;

            // ---------- NEW: Resolve visitorId based on name ----------
            Long effectiveVisitorId = req.getVisitorId();  // global id first

            if (effectiveVisitorId == null && !effectiveVisitorName.isEmpty()) {
                effectiveVisitorId = visitorService.findVisitorIdByName(effectiveVisitorName);

                if (effectiveVisitorId == null) {
                    skipped.add(new BulkVisitScheduleResult.Skip(
                            bank, branchCode,
                            "Visitor not found for name: " + effectiveVisitorName
                    ));
                    continue;
                }
            }

            // --------------------------------------------------------
            for (ComplaintLog c : opens) {

                Map<String, Object> updates = new LinkedHashMap<>();

                updates.put("complaintStatus", "Visit Schedule");

                if (!effectiveScheduleDate.isEmpty()) {
                    updates.put("scheduleDate", effectiveScheduleDate);
                }

                if (effectiveVisitorId != null) {
                    updates.put("visitorId", effectiveVisitorId);
                }

                if (!effectiveVisitorName.isEmpty()) {
                    updates.put("visitorName", effectiveVisitorName);
                }

                complaintLogService.updateComplaintLogFields(c.getId(), updates);

                if (Boolean.TRUE.equals(c.getMarkedInPool())) {
                    c.setMarkedInPool(false);
                    complaintLogRepository.save(c);
                    complaintLogService.saveComplaintHistory(
                            c.getComplaintId(), "markedInPool", "true", "false", "Bulk visit scheduling"
                    );
                }

                updated++;
            }
        }

        return new BulkVisitScheduleResult(updated, skipped);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String normalizeBranchCode(String code) {
        String t = code.trim();
        if (t.matches("\\d+")) {
            String unpadded = t.replaceFirst("^0+(?!$)", "");
            return String.format("%0" + BRANCH_CODE_LENGTH + "d",
                    Integer.parseInt(unpadded.isEmpty() ? "0" : unpadded));
        }
        return t;
    }
}
