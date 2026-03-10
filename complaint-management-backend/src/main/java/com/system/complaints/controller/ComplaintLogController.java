package com.system.complaints.controller;

import com.system.complaints.dto.ComplaintBranchGroupDTO;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.RemarksUpdate;
import com.system.complaints.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.sql.Date;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/complaints")
public class ComplaintLogController {

    @Autowired
    private ComplaintLogService complaintLogService;

    @Autowired
    private RemarksUpdateService remarksUpdateService;
    @Autowired
    private ComplaintHistoryService complaintHistoryService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private GoogleDriveService googleDriveService;

    /**
     * Upload a job card for a specific complaint.
     */
    @PostMapping("/{id}/upload-job-card")
    public ResponseEntity<String> uploadJobCard(
            @PathVariable Long id,
            @RequestParam("jobCard") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file selected.");
            }

            String cloudUrl = googleDriveService.uploadFile(file);
            boolean isUpdated = complaintLogService.updateJobCardPath(id, cloudUrl);

            if (isUpdated) {
                return ResponseEntity.ok(cloudUrl);
            } else {
                return ResponseEntity.badRequest().body("Failed to update job card path in database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to upload job card.");
        }
    }

    /**
     * Log a new complaint.
     */
    @PostMapping("/log")
    public ResponseEntity<ComplaintLog> logComplaint(@RequestBody ComplaintLog complaintLog) {
        try {
            ComplaintLog savedLog = complaintLogService.saveComplaintLog(complaintLog);

            // Send a socket message with complaintId and action for CREATE
            messagingTemplate.convertAndSend(
                    "/topic/paginated-by-status",
                    Map.of(
                            "complaintId", savedLog.getComplaintId(),
                            "action", "created"
                    )
            );

            return ResponseEntity.ok(savedLog);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }


    /**
     * Retrieve all complaints.
     */
//    @GetMapping("/all")
//    public ResponseEntity<List<ComplaintLog>> getAllComplaints() {
//        try {
//            // Use the new method that populates hardware logs
//            List<ComplaintLog> complaints = complaintLogService.getAllComplaintsWithHardwareLogs();
//            return ResponseEntity.ok(complaints);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(null);
//        }
//    }
//    @GetMapping("/all-with-reports")
//    public ResponseEntity<List<ComplaintLog>> getComplaintsWithReports() {
//        try {
//            List<ComplaintLog> complaints = complaintLogService.getAllComplaintsWithHardwareLogsAndReports();
//            return ResponseEntity.ok(complaints);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(null);
//        }
//    }


    /**
     * Retrieve complaints by status.
     */
    @GetMapping("/by-status")
    public ResponseEntity<List<ComplaintLog>> getComplaintsByStatus(@RequestParam String complaintStatus) {
        try {
            List<ComplaintLog> complaints = complaintLogService.getComplaintsByStatus(complaintStatus);
            return ResponseEntity.ok(complaints);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Mark a complaint as resolved.
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<String> markComplaintAsResolved(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody
    ) {
        try {
            // Extract staffRemarks and specialRemarks from the request body
            String staffRemarks = requestBody.get("staffRemarks");
            String specialRemarks = requestBody.get("specialRemarks");

            // Call the service method with the extracted data
            boolean isResolved = complaintLogService.markAsResolved(id, staffRemarks, specialRemarks);

            if (isResolved) {
                return ResponseEntity.ok("Complaint updated successfully with remarks");
            } else {
                return ResponseEntity.badRequest().body("Complaint not found or update failed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    /**
     * Retrieve complaints by both date and status.
     */
    @GetMapping("/by-date-and-status")
    public ResponseEntity<List<ComplaintLog>> getComplaintsByDateAndStatus(
            @RequestParam String date,
            @RequestParam String complaintStatus
    ) {
        try {
            // Convert date string to java.sql.Date
            Date parsedDate = Date.valueOf(date); // Assumes format is "yyyy-MM-dd"

            // Call service to get complaints by date and status
            List<ComplaintLog> complaints = complaintLogService.getComplaintsByDateAndStatus(parsedDate, complaintStatus);
            return ResponseEntity.ok(complaints);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Retrieve complaints by visitorId.
     */
    @GetMapping("/by-visitor")
    public ResponseEntity<List<ComplaintLog>> getComplaintsByVisitorId(@RequestParam Long visitorId) {
        try {
            List<ComplaintLog> complaints = complaintLogService.getComplaintsByVisitorId(visitorId);
            return ResponseEntity.ok(complaints);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    /**
     * Retrieve complaints where visitorId is null.
     */
    @GetMapping("/by-visitor-null")
    public ResponseEntity<List<ComplaintLog>> getComplaintsByNullVisitorId() {
        try {
            List<ComplaintLog> complaints = complaintLogService.getComplaintsByNullVisitorId();
            return ResponseEntity.ok(complaints);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    /**
     * Assign a complaint to a visitor.
     */
    @PutMapping("/{complaintId}/assign-to-visitor")
    public ResponseEntity<String> assignComplaintToVisitor(@PathVariable String complaintId, @RequestParam Long visitorId) {
        try {
            boolean isAssigned = complaintLogService.assignComplaintToVisitor(complaintId, visitorId);
            if (isAssigned) {
                return ResponseEntity.ok("Complaint assigned successfully to visitor ID: " + visitorId);
            } else {
                return ResponseEntity.badRequest().body("Failed to assign complaint to visitor. Visitor ID may not exist.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }
    /**
     * Unassign a complaint, setting its visitorId to null.
     */
    @PutMapping("/{complaintId}/unassign-visitor")
    public ResponseEntity<String> unassignComplaintVisitor(@PathVariable String complaintId) {
        try {
            boolean isUnassigned = complaintLogService.unassignComplaintVisitor(complaintId);
            if (isUnassigned) {
                return ResponseEntity.ok("Complaint unassigned successfully.");
            } else {
                return ResponseEntity.badRequest().body("Failed to unassign complaint.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    /**
     * Add a new remarks update to a complaint.
     */
    @PostMapping("/{id}/remarks")
    public ResponseEntity<RemarksUpdate> addRemarksUpdate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String remarks = body.get("remarks");
            RemarksUpdate remarksUpdate = remarksUpdateService.addRemarksUpdate(id, remarks);
            return ResponseEntity.ok(remarksUpdate);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get the latest remarks of a complaint.
     */
    @GetMapping("/{id}/remarks/latest")
    public ResponseEntity<RemarksUpdate> getLatestRemarks(@PathVariable Long id) {
        try {
            RemarksUpdate remarksUpdate = remarksUpdateService.getLatestRemarks(id);
            return ResponseEntity.ok(remarksUpdate);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get the remarks history of a complaint.
     */
    @GetMapping("/{id}/remarks/history")
    public ResponseEntity<List<RemarksUpdate>> getRemarksHistory(@PathVariable Long id) {
        try {
            List<RemarksUpdate> history = remarksUpdateService.getRemarksHistory(id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    } /**
     * Get the remarks history of a complaint.
     */

    @PostMapping("/remarks/history/batch")
    public ResponseEntity<Map<Long, List<RemarksUpdate>>> getRemarksHistoryBatch(
            @RequestBody List<Long> complaintIds) {
        try {
            return ResponseEntity.ok(remarksUpdateService.getRemarksHistoryBatch(complaintIds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * New endpoint to fetch remarks counts for multiple complaints.
     */
    @PostMapping("/remarks/counts")
    public ResponseEntity<Map<Long, Long>> getRemarksCounts(@RequestBody List<Long> complaintIds) {
        try {
            return ResponseEntity.ok(remarksUpdateService.getRemarksCountsBatch(complaintIds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Update specific fields of a complaint.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ComplaintLog> updateComplaintLog(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            Optional<ComplaintLog> updatedComplaintLog = complaintLogService.updateComplaintLogFields(id, updates);

            if (updatedComplaintLog.isPresent()) {
                ComplaintLog log = updatedComplaintLog.get();

                // Send a socket message with complaintId and action
                messagingTemplate.convertAndSend(
                        "/topic/paginated-by-status",
                        Map.of(
                                "complaintId", log.getComplaintId(), // <-- your business complaintId, NOT the db id
                                "action", "updated"
                        )
                );

                return ResponseEntity.ok(log);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }


    /**
     * Add a new staff-remarks update to a complaint.
     */
    @PostMapping("/{id}/update-staff-remarks")
    public ResponseEntity<String> updateStaffRemarks(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String staffRemarks = body.get("staffRemarks");
            boolean isUpdated = complaintLogService.updateStaffRemarks(id, staffRemarks);

            if (isUpdated) {
                return ResponseEntity.ok("Staff remarks updated successfully!");
            } else {
                return ResponseEntity.badRequest().body("Failed to update staff remarks. Complaint not found.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to update staff remarks.");
        }
    }

    /**
     * Retrieve today's complaint metrics (counts of open and closed).
     */
    @GetMapping("/todays-complaints")
    public ResponseEntity<Map<String, Integer>> getTodaysComplaintMetrics() {
        try {
            Map<String, Integer> metrics = complaintLogService.getTodaysComplaintMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Retrieve city-wise today's metrics (open and closed).
     */
    @GetMapping("/city-wise-todays-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getCityWiseTodaysMetrics() {
        try {
            Map<String, Map<String, Integer>> metrics = complaintLogService.getCityWiseTodaysComplaintMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Retrieve bank-wise today's metrics (open and closed).
     */
    @GetMapping("/bank-wise-todays-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getBankWiseTodaysMetrics() {
        try {
            Map<String, Map<String, Integer>> metrics = complaintLogService.getBankWiseTodaysComplaintMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // -------------------------------------------------------------------------
    // NEW ENDPOINTS: All-time (unfiltered) city-wise and bank-wise metrics
    // -------------------------------------------------------------------------

    /**
     * Retrieve all-time city-wise complaints metrics (open and closed).
     */
    @GetMapping("/city-wise-all-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getCityWiseAllComplaintMetrics() {
        try {
            Map<String, Map<String, Integer>> metrics = complaintLogService.getCityWiseAllComplaintMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Retrieve all-time bank-wise complaints metrics (open and closed).
     */
    @GetMapping("/bank-wise-all-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getBankWiseAllComplaintMetrics() {
        try {
            Map<String, Map<String, Integer>> metrics = complaintLogService.getBankWiseAllComplaintMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    /**
     * Retrieve overall complaint metrics (total open and closed).
     */
    @GetMapping("/overall-metrics")
    public ResponseEntity<Map<String, Integer>> getOverallComplaintMetrics() {
        try {
            Map<String, Integer> metrics = complaintLogService.getOverallComplaintMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    /**
     * Retrieve complaint history by complaint ID.
     */
    @GetMapping("/{complaintId}/history")
    public ResponseEntity<Map<String, Object>> getComplaintFullHistory(
            @PathVariable String complaintId) {
        try {
            Map<String, Object> fullHistory = complaintLogService.getFullComplaintHistory(complaintId);
            return ResponseEntity.ok(fullHistory);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/complaints-summary")
    public ResponseEntity<Map<String, Object>> getComplaintDashboardSummary() {
        return ResponseEntity.ok(complaintLogService.getComplaintDashboardSummary());
    }
    @GetMapping("/city-wise-summary")
    public ResponseEntity<Map<String, Object>> getCityWiseSummary() {
        return ResponseEntity.ok(complaintLogService.getCityWiseEngineerSummary());
    }

    @PatchMapping("/{id}/dc-generated")
    public ResponseEntity<?> updateDcGenerated(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload
    ) {
        Boolean dcGenerated = payload.get("dcGenerated");
        try {
            ComplaintLog complaintLog = complaintLogService.getComplaintById(id); // throws if not found
            Boolean oldVal = complaintLog.getDcGenerated();
            complaintLog.setDcGenerated(dcGenerated);

            // Save to DB using repository directly or a dedicated update method, NOT saveComplaintLog
            complaintLogService.updateOnlyDcGenerated(complaintLog);

            // Log in history (only for dcGenerated change)
            complaintLogService.saveComplaintHistory(
                    complaintLog.getComplaintId(),
                    "dcGenerated",
                    oldVal == null ? "null" : oldVal.toString(),
                    dcGenerated == null ? "null" : dcGenerated.toString(),
                    "DC Generated"
            );

            return ResponseEntity.ok(complaintLog);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update dcGenerated");
        }
    }

    @PatchMapping("/{id}/in-pool")
    public ResponseEntity<?> updateIsMarkedInPool(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload
    ) {
        Boolean markedInPool = payload.get("markedInPool");
        try {
            ComplaintLog complaintLog = complaintLogService.getComplaintById(id);
            Boolean oldVal = complaintLog.getMarkedInPool();
            complaintLog.setMarkedInPool(markedInPool);

            // Save markedInPool
            complaintLogService.updateOnlyMarkedInPool(complaintLog);

            // Optionally log the change
            complaintLogService.saveComplaintHistory(
                    complaintLog.getComplaintId(),
                    "isMarkedInPool",
                    oldVal == null ? "null" : oldVal.toString(),
                    markedInPool == null ? "null" : markedInPool.toString(),
                    "Marked In Pool"
            );

            // Unassign visitor and update fields if markedInPool is true
            if (Boolean.TRUE.equals(markedInPool)) {
                // Convert to String if necessary for the service method
                complaintLogService.unassignComplaintVisitor(complaintLog.getComplaintId());
            }

            return ResponseEntity.ok(complaintLog);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update MarkedInPool");
        }
    }
    @GetMapping("/dashboard-counts")
    public ResponseEntity<Map<String, Map<String, Integer>>> getDashboardCounts() {
        try {
            Map<String, Map<String, Integer>> counts = complaintLogService.getDashboardCounts();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/paginated-by-status")
    public ResponseEntity<?> getPaginatedComplaintsByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String engineerName,
            @RequestParam(required = false) List<String> city,
            @RequestParam(required = false) String complaintStatus,
            @RequestParam(required = false) String subStatus,

            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String approvedDateFrom,
            @RequestParam(required = false) String approvedDateTo,
            @RequestParam(required = false) String closedDateFrom,
            @RequestParam(required = false) String closedDateTo,
            @RequestParam(required = false) String quotationDateFrom,
            @RequestParam(required = false) String quotationDateTo,
            @RequestParam(required = false) String pendingForClosedDateFrom,
            @RequestParam(required = false) String pendingForClosedDateTo,

            @RequestParam(required = false) String date,
            @RequestParam(required = false) String approvedDate,
            @RequestParam(required = false) String closedDate,
            @RequestParam(required = false) String pendingForClosedDate,
            @RequestParam(required = false) String quotationDate,

            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String inPool,
            @RequestParam(required = false) Boolean hasReport,
            @RequestParam(required = false) String reportType
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date", "id"));

            Page<ComplaintBranchGroupDTO> resultPage;

            if ("Open".equalsIgnoreCase(status)) {
                // For Open: return Page<ComplaintBranchGroupDTO>
                resultPage = complaintLogService.searchOpenComplaintsWithBranchFiltering(
                        status,
                        bankName,
                        branchCode,
                        branchName,
                        engineerName,
                        city,
                        complaintStatus,
                        subStatus,

                        dateFrom, dateTo,
                        approvedDateFrom, approvedDateTo,
                        closedDateFrom, closedDateTo,
                        quotationDateFrom, quotationDateTo,
                        pendingForClosedDateFrom, pendingForClosedDateTo,

                        date,
                        approvedDate,
                        closedDate,
                        pendingForClosedDate,
                        quotationDate,

                        priority,
                        inPool,
                        hasReport,
                        reportType,
                        pageable
                );
            } else {
                // For other statuses, return Page<ComplaintBranchGroupDTO>
                resultPage = complaintLogService.searchComplaints(
                        status,
                        bankName,
                        branchCode,
                        branchName,
                        engineerName,
                        city,
                        complaintStatus,
                        subStatus,

                        dateFrom, dateTo,
                        approvedDateFrom, approvedDateTo,
                        closedDateFrom, closedDateTo,
                        quotationDateFrom, quotationDateTo,
                        pendingForClosedDateFrom, pendingForClosedDateTo,

                        date,
                        approvedDate,
                        closedDate,
                        pendingForClosedDate,
                        quotationDate,

                        priority,
                        inPool,
                        hasReport,
                        reportType,
                        pageable
                );
            }

            // Read the offset the service stored on this request
            long complaintsBeforePage = 0L;
            RequestAttributes ra = RequestContextHolder.getRequestAttributes();
            if (ra != null) {
                Object attr = ra.getAttribute("complaintsBeforePage", RequestAttributes.SCOPE_REQUEST);
                if (attr instanceof Long l) {
                    complaintsBeforePage = l;
                } else if (attr instanceof Integer i) {
                    complaintsBeforePage = i.longValue();
                }
            }

            return ResponseEntity.ok()
                    .header("X-Complaints-Before-Page", String.valueOf(complaintsBeforePage))
                    // expose the custom header to browsers (you can also do this in global CORS config)
                    .header("Access-Control-Expose-Headers", "X-Complaints-Before-Page")
                    .body(resultPage);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Page.empty());
        }
    }

    @GetMapping("/duplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @RequestParam String bankName,
            @RequestParam String branchCode
    ) {
        boolean exists = complaintLogService.existsOpenComplaint(bankName, branchCode);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/by-id")
    public ResponseEntity<ComplaintLog> getComplaintByComplaintId(@RequestParam String complaintId) {
        try {
            ComplaintLog log = complaintLogService.getComplaintByComplaintId(complaintId);
            if (log != null) {
                return ResponseEntity.ok(log);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }


}
