    package com.system.complaints.service;

    import com.system.complaints.dto.ComplaintBranchGroupDTO;
    import com.system.complaints.model.*;
    import com.system.complaints.repository.ComplaintLogRepository;
    import com.system.complaints.repository.ComplaintHistoryRepository;
    import com.system.complaints.repository.HardwareLogRepository;
    import jakarta.annotation.PostConstruct;
    import jakarta.persistence.EntityManager;
    import jakarta.persistence.PersistenceContext;
    import jakarta.persistence.TypedQuery;
    import jakarta.persistence.criteria.*;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageImpl;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.web.context.request.RequestAttributes;
    import org.springframework.web.context.request.RequestContextHolder;

    import java.math.BigDecimal;
    import java.sql.Date;
    import java.time.LocalDate;
    import java.util.*;
    import java.sql.Timestamp;
    import java.util.stream.Collectors;

    @Service
    public class ComplaintLogService {


        @PersistenceContext
        private EntityManager entityManager;

        @Autowired
        private ComplaintLogRepository complaintLogRepository;

        @Autowired
        private HardwareLogService hardwareLogService; // Existing dependency

        @Autowired
        private RemarksUpdateService remarksUpdateService;

        @Autowired
        private ComplaintHistoryRepository complaintHistoryRepository; // New dependency for history

        @Autowired
        private UserDetailsServiceImpl userDetailsService; // Inject the UserDetailsService

        @Autowired
        private HardwareLogRepository hardwareLogRepository;

        @Autowired
        private VisitorService visitorService;  // Service to fetch visitor details.

        @Autowired
        private ScheduleService scheduleService;

        @Autowired
        private PendingForClosedLogService pendingForClosedLogService;

        /**
         * Update complaints where schedule date has passed and status is "Visit Schedule"
         */
        @Scheduled(fixedRate = 3600000)
        public void updateExpiredVisitSchedules() {
            LocalDate today = LocalDate.now();
            List<ComplaintLog> expiredVisits = complaintLogRepository.findByComplaintStatusAndScheduleDateBefore(
                    "Visit Schedule", Date.valueOf(today)
            );

            for (ComplaintLog complaint : expiredVisits) {
                String oldStatus = complaint.getComplaintStatus();

                // Fetch previous status from history
                String revertStatus = getPreviousNonVisitScheduleStatus(complaint.getComplaintId());

                complaint.setComplaintStatus(revertStatus);
                complaint.setScheduleDate(null); // <--- Set scheduleDate to null

                complaintLogRepository.save(complaint);

                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "complaintStatus",
                        oldStatus,
                        revertStatus,
                        "Scheduled visit date expired - status reverted and scheduleDate set to null"
                );
            }
        }


        /**
         * Helper methods
         */

        private Date parseDate(String dateString, String fieldName) {
            if (dateString == null || dateString.trim().isEmpty()) {
                return null;
            }

            try {
                // Try direct SQL Date parsing first (yyyy-MM-dd format)
                return Date.valueOf(dateString.trim());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid date format for " + fieldName + ": " + dateString +
                        ". Expected format: yyyy-MM-dd (e.g., 2024-07-01)");
                return null;
            }
        }


        private String getLoggedInUsername() {
            // Grab the current Authentication object from the SecurityContext
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            // If no authentication or not authenticated, return "anonymousUser" (or handle as needed)
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("No valid authentication found. User is anonymous.");
                return "anonymousUser";
            }

            // Extract principal from authentication
            Object principal = authentication.getPrincipal();

            // If principal is an instance of UserDetails, return the username
            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            }

            // Otherwise, fallback to principal's toString()
            return principal.toString();
        }

        /**
         * Helper method to log changes to complaint fields.
         * This method automatically sets the changedBy field using getLoggedInUsername().
         */
        public void saveComplaintHistory(String complaintId, String fieldName, String oldValue, String newValue, String reason) {
            ComplaintHistory history = new ComplaintHistory();
            history.setComplaintId(complaintId);
            history.setFieldName(fieldName);
            history.setOldValue(oldValue);
            history.setNewValue(newValue);
            history.setChangeDate(new Timestamp(System.currentTimeMillis()));
            history.setChangedBy(getLoggedInUsername());
            history.setReasonForChange(reason);

            // Optionally, log the username of the person who initially logged the complaint
            ComplaintLog complaintLog = complaintLogRepository.findByComplaintId(complaintId)
                    .orElseThrow(() -> new RuntimeException("ComplaintLog not found with complaintId: " + complaintId));
            history.setLoggedBy(complaintLog.getLoggedBy());

            complaintHistoryRepository.save(history);
        }

        public void updateOnlyDcGenerated(ComplaintLog complaintLog) {
            // Only save the object, do not log "Complaint Logged" or any other history here.
            complaintLogRepository.save(complaintLog);
        }

        public void updateOnlyMarkedInPool(ComplaintLog complaintLog) {
            complaintLogRepository.save(complaintLog);
        }


        /**
         * Update the job card path for a specific complaint.
         */
        public boolean updateJobCardPath(Long id, String jobCardPath) {
            int rowsAffected = complaintLogRepository.updateJobCardPath(id, jobCardPath);
            complaintLogRepository.findById(id).ifPresent(complaint -> {
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "jobCardPath",
                        complaint.getJobCardPath(),
                        jobCardPath,
                        "Automated update"
                );
            });
            return rowsAffected > 0;
        }

        /**
         * Mark a complaint as resolved and update related fields.
         */
        public boolean markAsResolved(Long id, String staffRemarks, String specialRemarks) {
            Optional<ComplaintLog> complaintLogOpt = complaintLogRepository.findById(id);

            if (complaintLogOpt.isPresent()) {
                ComplaintLog complaintLog = complaintLogOpt.get();

                String oldStaffRemarks = complaintLog.getStaffRemarks();
                String oldSpecialRemarks = complaintLog.getSpecialRemarks();
                String oldStatus = complaintLog.getComplaintStatus();
                Date oldPendingForClosedDate = complaintLog.getPendingForClosedDate(); // Capture old date
                Date autoDate = Date.valueOf(LocalDate.now());

                // Update remarks and status
                complaintLog.setStaffRemarks(staffRemarks);
                complaintLog.setSpecialRemarks(specialRemarks);
                complaintLog.setComplaintStatus("Pending For Closed");
                complaintLog.setPendingForClosedDate(autoDate);

                // Log the changes
                saveComplaintHistory(complaintLog.getComplaintId(), "staffRemarks", oldStaffRemarks, staffRemarks, "Automated update");
                saveComplaintHistory(complaintLog.getComplaintId(), "specialRemarks", oldSpecialRemarks, specialRemarks, "Automated update");
                saveComplaintHistory(complaintLog.getComplaintId(), "complaintStatus", oldStatus, "Pending For Closed", "Automated update");

                // Log the pendingForClosedDate change
                if (oldPendingForClosedDate == null || !oldPendingForClosedDate.equals(autoDate)) {
                    saveComplaintHistory(complaintLog.getComplaintId(), "pendingForClosedDate",
                            oldPendingForClosedDate != null ? oldPendingForClosedDate.toString() : "N/A",
                            autoDate.toString(),
                            "Automated update");
                }
                try {
                    pendingForClosedLogService.logPendingForClosed(
                            complaintLog.getComplaintId(),
                            "MOBILE",                     // source
                            null,                         // userId if available, else null
                            "MOBILE_APP"                  // username or source label
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    // Optionally log but do not fail the main operation
                }
                // Recalculate aging days
                calculateAndSetAgingDays(complaintLog);
                // Mark schedule as successful if there is a scheduleDate
                if (complaintLog.getScheduleDate() != null) {
                    scheduleService.markScheduleSuccessful(
                            complaintLog.getComplaintId(),
                            complaintLog.getScheduleDate()
                    );
                }

                complaintLogRepository.save(complaintLog);
                return true;
            }

            return false;
        }


        /**
         * Check if a complaint is a repeat complaint.
         */
        public boolean isRepeatComplaint(ComplaintLog complaintLog) {
            // Validate input fields
            if (complaintLog.getBankName() == null ||
                    complaintLog.getBranchCode() == null ||
                    complaintLog.getBranchName() == null ||
                    complaintLog.getComplaintType() == null ||
                    complaintLog.getDate() == null) {
                return false;
            }

            // Define the 15-day range using the date
            LocalDate complaintDate = complaintLog.getDate().toLocalDate();
            LocalDate startDate = complaintDate.minusDays(15);
            LocalDate endDate = complaintDate.plusDays(15);

            // Fetch similar complaints
            List<ComplaintLog> similarComplaints = complaintLogRepository.findSimilarComplaintsWithin15Days(
                    complaintLog.getBankName(),
                    complaintLog.getBranchCode(),
                    complaintLog.getBranchName(),
                    complaintLog.getComplaintType(),
                    Date.valueOf(startDate),
                    Date.valueOf(endDate)
            );

            // Exclude the current complaint itself if present in the results
            return similarComplaints.stream()
                    .anyMatch(existingComplaint -> !existingComplaint.getId().equals(complaintLog.getId()));
        }

        /**
         * Save a new complaint log, checking for repeat complaints.
         */
        public ComplaintLog saveComplaintLog(ComplaintLog complaintLog) {
            // Check if the complaint is a repeat
            boolean isRepeat = isRepeatComplaint(complaintLog);

            // Set the repeatComplaint field
            complaintLog.setRepeatComplaint(isRepeat);

            // Calculate aging days
            calculateAndSetAgingDays(complaintLog);

            // Set the username of the person who logged the complaint
            String loggedBy = getLoggedInUsername();
            complaintLog.setLoggedBy(loggedBy);

            // Save the complaint log
            ComplaintLog savedComplaintLog = complaintLogRepository.save(complaintLog);

            // Save an initial entry in the complaint history
            saveComplaintHistory(
                    savedComplaintLog.getComplaintId(),
                    "Complaint Logged",
                    null,
                    "Complaint Created",
                    "Initial complaint log"
            );

            return savedComplaintLog;
        }
        /**
         * Add a new remarks update and log it in the complaint history.
         */
        public RemarksUpdate addRemarks(Long complaintId, String remarks) {
            RemarksUpdate remarksUpdate = remarksUpdateService.addRemarksUpdate(complaintId, remarks);

            saveComplaintHistory(
                    remarksUpdate.getComplaintLog().getComplaintId(),
                    "Remarks",
                    null,
                    remarks,
                    "Remarks added"
            );

            return remarksUpdate;
        }

        /**
         * Retrieve full history of a specific complaint by complaintId, including remarks.
         */
        public Map<String, Object> getFullComplaintHistory(String complaintId) {
            ComplaintLog complaintLog = complaintLogRepository.findByComplaintId(complaintId)
                    .orElseThrow(() -> new RuntimeException("ComplaintLog not found with complaintId: " + complaintId));

            List<ComplaintHistory> history = complaintHistoryRepository.findByComplaintIdOrderByChangeDateAsc(complaintId);

            // Transform remarks to include the name of the person who wrote the comment
            List<Map<String, Object>> simplifiedRemarks = remarksUpdateService.getRemarksHistory(complaintLog.getId()).stream()
                    .map(remark -> {
                        Map<String, Object> simplifiedRemark = new HashMap<>();
                        simplifiedRemark.put("id", remark.getId());
                        simplifiedRemark.put("remarks", remark.getRemarks());
                        simplifiedRemark.put("timestamp", remark.getTimestamp());
                        simplifiedRemark.put("commentedBy", remark.getLoggedBy()); // Include the name of the commenter
                        return simplifiedRemark;
                    })
                    .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("complaintDetails", complaintLog);
            result.put("history", history);
            result.put("remarks", simplifiedRemarks);

            return result;
        }

        /**
         * Retrieve a ComplaintLog by ID.
         */
        public ComplaintLog getComplaintById(Long id) {
            return complaintLogRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("ComplaintLog not found with ID: " + id));
        }

        /**
         * Retrieve all complaints.
         */
        public List<ComplaintLog> getAllComplaints() {
            return complaintLogRepository.findAll();
        }

        /**
         * Retrieve complaints by status.
         */
        public List<ComplaintLog> getComplaintsByStatus(String complaintStatus) {
            return complaintLogRepository.findByComplaintStatus(complaintStatus);
        }

        /**
         * Retrieve complaints by both date and status.
         */
        public List<ComplaintLog> getComplaintsByDateAndStatus(Date date, String complaintStatus) {
            return complaintLogRepository.findByDateAndComplaintStatus(date, complaintStatus);
        }

        /**
         * Calculate and set aging days for a complaint log.
         */
        public void calculateAndSetAgingDays(ComplaintLog complaintLog) {
            if (complaintLog.getClosedDate() == null || complaintLog.getDate() == null) {
                complaintLog.setAgingDays(null);
                return;
            }

            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    complaintLog.getDate().toLocalDate(),
                    complaintLog.getClosedDate().toLocalDate()
            );

            complaintLog.setAgingDays((int) daysBetween);
        }

        /**
         * Recalculate aging days for a specific complaint by ID.
         */
        public void recalculateAgingDaysByComplaintId(Long id) {
            Optional<ComplaintLog> complaintLogOpt = complaintLogRepository.findById(id);

            if (complaintLogOpt.isPresent()) {
                ComplaintLog complaintLog = complaintLogOpt.get();
                calculateAndSetAgingDays(complaintLog);
                complaintLogRepository.save(complaintLog);
            } else {
                throw new RuntimeException("Complaint with ID " + id + " not found");
            }
        }

        /**
         * Retrieve complaints by visitor ID.
         */
        public List<ComplaintLog> getComplaintsByVisitorId(Long visitorId) {
            return complaintLogRepository.findByVisitorId(visitorId);
        }
        /**
         * Fetch all complaints with a null visitorId.
         */
        public List<ComplaintLog> getComplaintsByNullVisitorId() {
            return complaintLogRepository.findByVisitorIdIsNullAndIsMarkedInPoolTrue();
        }

        /**
         * Assign a complaint to a visitor by updating the visitorId of the complaint.
         */
        public boolean assignComplaintToVisitor(String complaintId, Long visitorId) {
            Optional<ComplaintLog> complaintOpt = complaintLogRepository.findByComplaintId(complaintId);
            if (complaintOpt.isPresent()) {
                ComplaintLog complaint = complaintOpt.get();

                // --- Save old values for history BEFORE making changes ---
                Long oldVisitorId = complaint.getVisitorId();
                String oldVisitorName = complaint.getVisitorName();
                String oldStatus = complaint.getComplaintStatus();
                Date oldScheduleDate = complaint.getScheduleDate();

                // Fetch visitor name from the visitor service
                String visitorName = visitorService.findVisitorNameById(visitorId);
                if (visitorName == null) {
                    return false; // Return false or handle as needed if no visitor is found.
                }

                // Assign visitor details
                complaint.setVisitorId(visitorId);
                complaint.setVisitorName(visitorName);

                // Update complaint status to "Visit Schedule"
                complaint.setComplaintStatus("Visit Schedule");

                // Set scheduleDate to the current date
                Date today = Date.valueOf(LocalDate.now());
                complaint.setScheduleDate(today);

                // Log the schedule visit
                scheduleService.logSchedule(complaint.getComplaintId(), today, getLoggedInUsername());

                // Marked in pool should be false now
                complaint.setMarkedInPool(false);

                // Save updated complaint details
                complaintLogRepository.save(complaint);

                // --- Save history for each field changed ---
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "visitorId",
                        oldVisitorId != null ? oldVisitorId.toString() : null,
                        visitorId.toString(),
                        "Assigned to visitor"
                );
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "visitorName",
                        oldVisitorName,
                        visitorName,
                        "Assigned to visitor"
                );
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "complaintStatus",
                        oldStatus,
                        "Visit Schedule",
                        "Assigned to visitor"
                );
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "scheduleDate",
                        oldScheduleDate != null ? oldScheduleDate.toString() : null,
                        today.toString(),
                        "Visit scheduled"
                );

                return true;
            }
            return false;
        }


        /**
         * Unassign a complaint by setting its visitorId to null.
         */
        public boolean unassignComplaintVisitor(String complaintId) {
            Optional<ComplaintLog> complaintOpt = complaintLogRepository.findByComplaintId(complaintId);
            if (complaintOpt.isPresent()) {
                ComplaintLog complaint = complaintOpt.get();
                complaint.setVisitorId(null);
                complaint.setVisitorName(null);

                // Revert status to previous (from history)
                String revertStatus = getPreviousNonVisitScheduleStatus(complaint.getComplaintId());
                complaint.setComplaintStatus(revertStatus);

                // If there was a scheduleDate before, log the unscheduling/cancellation
                Date previousScheduleDate = complaint.getScheduleDate();
                if (previousScheduleDate != null) {
                    // Log the schedule cancellation (method name may vary in your ScheduleService)
                    scheduleService.logScheduleCancellation(
                            complaint.getComplaintId(),
                            previousScheduleDate,
                            getLoggedInUsername()
                    );
                }

                complaint.setScheduleDate(null);
                complaint.setMarkedInPool(true);

                complaintLogRepository.save(complaint);

                // Optionally log this in complaint history
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "visitorId",
                        "previous",
                        null,
                        "Unassigned visitor"
                );
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "complaintStatus",
                        "Visit Schedule",
                        revertStatus,
                        "Unassigned visitor, reverted status"
                );
                saveComplaintHistory(
                        complaint.getComplaintId(),
                        "scheduleDate",
                        previousScheduleDate != null ? previousScheduleDate.toString() : null,
                        null,
                        "Unassigned visitor, schedule canceled"
                );

                return true;
            }
            return false;
        }




        /**
         * Retrieve a ComplaintLog by Complaint ID.
         */
        public ComplaintLog getComplaintByComplaintId(String complaintId) {
            ComplaintLog log = complaintLogRepository.findByComplaintId(complaintId)
                    .orElseThrow(() -> new RuntimeException("ComplaintLog not found with complaintId: " + complaintId));

            // ✅ Fetch latest courier status
            String latestCourierStatus = hardwareLogRepository
                    .findTopByComplaintLog_ComplaintIdOrderByIdDesc(complaintId)
                    .map(HardwareLog::getCourierStatus)
                    .orElse(null);

            log.setCourierStatus(latestCourierStatus);

            return log;
        }


        /**
         * Update specific fields of a ComplaintLog by its ID.
         * Now includes history logging.
         */
        public Optional<ComplaintLog> updateComplaintLogFields(Long id, Map<String, Object> updates) {
            Optional<ComplaintLog> existingLogOpt = complaintLogRepository.findById(id);

            if (existingLogOpt.isPresent()) {
                ComplaintLog existingLog = existingLogOpt.get();

                // Track if we need to recalculate aging days
                final boolean[] recalculateAgingDays = {false};

                String reason = "Automated update";

                // Update fields dynamically based on provided keys
                updates.forEach((key, value) -> {
                    switch (key) {
                        case "quotationDate": {
                            Date oldVal = existingLog.getQuotationDate();
                            Date newVal = parseDate(value);
                            existingLog.setQuotationDate(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "quotationDate",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal != null ? newVal.toString() : null,
                                    reason
                            );
                            break;
                        }
                        case "approvedDate": {
                            Date oldVal = existingLog.getApprovedDate();
                            Date newVal = parseDate(value);
                            existingLog.setApprovedDate(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "approvedDate",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal != null ? newVal.toString() : null,
                                    reason
                            );
                            break;
                        }
                        case "closedDate": {
                            Date oldVal = existingLog.getClosedDate();
                            Date newVal = parseDate(value);
                            existingLog.setClosedDate(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "closedDate",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal != null ? newVal.toString() : null,
                                    reason
                            );
                            recalculateAgingDays[0] = true;
                            break;
                        }
                        case "scheduleDate": {
                            Date oldVal = existingLog.getScheduleDate();
                            Date newVal = parseDate(value);
                            existingLog.setScheduleDate(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "scheduleDate",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal != null ? newVal.toString() : null,
                                    reason
                            );
                            // LOG to Schedules if status is "Visit Schedule"
                            if ("Visit Schedule".equals(existingLog.getComplaintStatus()) && newVal != null) {
                                scheduleService.logSchedule(existingLog.getComplaintId(), newVal, getLoggedInUsername());
                            }
                            break;
                        }
                        case "hardwarePickedDate": {
                            Date oldVal = existingLog.getHardwarePickedDate();
                            Date newVal = parseDate(value);
                            existingLog.setHardwarePickedDate(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "hardwarePickedDate",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal != null ? newVal.toString() : null,
                                    reason
                            );
                            break;
                        }
                        case "date": {
                            Date oldVal = existingLog.getDate();
                            Date newVal = parseDate(value);
                            existingLog.setDate(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "date",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal != null ? newVal.toString() : null,
                                    reason
                            );
                            recalculateAgingDays[0] = true;
                            break;
                        }
                        case "complaintStatus": {
                            String oldVal = existingLog.getComplaintStatus();
                            String newVal = value.toString();
                            existingLog.setComplaintStatus(newVal);
                            saveComplaintHistory(existingLog.getComplaintId(), "complaintStatus", oldVal, newVal, reason);

                            if ("Wait For Approval".equals(newVal) && existingLog.getQuotationDate() == null) {
                                Date autoDate = Date.valueOf(LocalDate.now());
                                existingLog.setQuotationDate(autoDate);
                                saveComplaintHistory(existingLog.getComplaintId(), "quotationDate", null, autoDate.toString(), "Status-triggered update");
                            }
                            if ("Approved".equals(newVal) && existingLog.getApprovedDate() == null) {
                                Date autoDate = Date.valueOf(LocalDate.now());
                                existingLog.setApprovedDate(autoDate);
                                saveComplaintHistory(existingLog.getComplaintId(), "approvedDate", null, autoDate.toString(), "Status-triggered update");
                            }
                            if ("FOC".equals(newVal) && existingLog.getFocDate() == null) {
                                Date autoDate = Date.valueOf(LocalDate.now());
                                existingLog.setFocDate(autoDate);
                                saveComplaintHistory(existingLog.getComplaintId(), "focDate", null, autoDate.toString(), "Status-triggered update");
                            }
                            if ("Hardware Picked".equals(newVal) && existingLog.getHardwarePickedDate() == null) {
                                Date autoDate = Date.valueOf(LocalDate.now());
                                existingLog.setHardwarePickedDate(autoDate);
                                saveComplaintHistory(existingLog.getComplaintId(), "hardwarePickedDate", null, autoDate.toString(), "Status-triggered update");
                            }
                            if ("Pending For Closed".equals(newVal)) {
                                Date autoDate = Date.valueOf(LocalDate.now());
                                if (existingLog.getPendingForClosedDate() == null) {
                                    existingLog.setPendingForClosedDate(autoDate);
                                    saveComplaintHistory(
                                            existingLog.getComplaintId(),
                                            "pendingForClosedDate",
                                            null,
                                            autoDate.toString(),
                                            "Status-triggered update"
                                    );
                                }

                                // ✅ Log in PendingForClosedLog table (PORTAL source)
                                try {
                                    pendingForClosedLogService.logPendingForClosed(
                                            existingLog.getComplaintId(),
                                            "PORTAL",
                                            null,          // userId not used
                                            "PORTAL_USER"  // generic label
                                    );

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // Do not fail the update if log insert fails
                                }
                            }
                            if ("Closed".equals(newVal) && existingLog.getClosedDate() == null) {
                                Date autoDate = Date.valueOf(LocalDate.now());
                                existingLog.setClosedDate(autoDate);
                                saveComplaintHistory(existingLog.getComplaintId(), "closedDate", null, autoDate.toString(), "Status-triggered update");
                            }
                            if ("Visit Schedule".equals(newVal) && existingLog.getScheduleDate() != null) {
                                scheduleService.logSchedule(existingLog.getComplaintId(), existingLog.getScheduleDate(), getLoggedInUsername());
                            }

                            // ---- ADD THIS BLOCK ----
                            // Automatically mark schedule as successful if status changes to Hardware Picked or Pending For Closed
                            if (
                                    ("Hardware Picked".equals(newVal) || "Pending For Closed".equals(newVal)|| "Closed".equals(newVal))
                                            && "Visit Schedule".equals(oldVal)
                                            && existingLog.getScheduleDate() != null
                            ) {
                                scheduleService.markScheduleSuccessful(existingLog.getComplaintId(), existingLog.getScheduleDate());
                            }
                            // ---- END ----

                            // Handle logging for valid statuses
                            List<String> validStatuses = Arrays.asList("Hardware Picked", "FOC", "Quotation");
                            if (validStatuses.contains(newVal)) {
                                hardwareLogService.createHardwareLogIfHardwarePicked(existingLog.getComplaintId());
                            }
                            break;
                        }

                        case "specialRemarks": {
                            String oldVal = existingLog.getSpecialRemarks();
                            String newVal = value.toString();
                            existingLog.setSpecialRemarks(newVal);
                            saveComplaintHistory(existingLog.getComplaintId(), "specialRemarks", oldVal, newVal, reason);
                            break;
                        }
                        case "approvalRemarks": {
                            String oldVal = existingLog.getApprovalRemarks();
                            String newVal = value.toString();
                            existingLog.setApprovalRemarks(newVal);
                            saveComplaintHistory(existingLog.getComplaintId(), "approvalRemarks", oldVal, newVal, reason);
                            break;
                        }
                        case "closedStatus": {
                            String oldVal = existingLog.getClosedStatus();
                            String newVal = value.toString();
                            existingLog.setClosedStatus(newVal);
                            saveComplaintHistory(existingLog.getComplaintId(), "closedStatus", oldVal, newVal, reason);
                            break;
                        }
                        case "visitorName": {
                            String oldVal = existingLog.getVisitorName();
                            String newVal = value.toString();
                            existingLog.setVisitorName(newVal);
                            saveComplaintHistory(existingLog.getComplaintId(), "visitorName", oldVal, newVal, reason);
                            break;
                        }
                        case "visitorId": {
                            Long oldVal = existingLog.getVisitorId();
                            Long newVal = Long.parseLong(value.toString()); // Parse value to Integer
                            existingLog.setVisitorId(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "visitorId",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal.toString(),
                                    reason
                            );
                            break;
                        }
                        case "isPriority": {
                            Boolean oldVal = existingLog.getPriority();
                            Boolean newVal = Boolean.parseBoolean(value.toString());
                            existingLog.setPriority(newVal);
                            saveComplaintHistory(
                                    existingLog.getComplaintId(),
                                    "isPriority",
                                    oldVal != null ? oldVal.toString() : null,
                                    newVal.toString(),
                                    reason
                            );
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unexpected field: " + key);
                    }
                });

                // Recalculate aging days if necessary
                if (recalculateAgingDays[0]) {
                    calculateAndSetAgingDays(existingLog);
                }

                // Save and return the updated entry
                return Optional.of(complaintLogRepository.save(existingLog));
            } else {
                return Optional.empty();
            }
        }

        /**
         * Parse a value into a java.sql.Date.
         */
        private Date parseDate(Object value) {
            try {
                return Date.valueOf(value.toString());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid date format. Expected format: yyyy-MM-dd", e);
            }
        }

        /**
         * Update the staff remarks for a specific complaint.
         */
        public boolean updateStaffRemarks(Long id, String staffRemarks) {
            Optional<ComplaintLog> complaintLogOptional = complaintLogRepository.findById(id);

            if (complaintLogOptional.isPresent()) {
                ComplaintLog complaintLog = complaintLogOptional.get();
                String oldVal = complaintLog.getStaffRemarks();
                complaintLog.setStaffRemarks(staffRemarks);
                complaintLogRepository.save(complaintLog);

                // Log this change
                saveComplaintHistory(complaintLog.getComplaintId(), "staffRemarks", oldVal, staffRemarks, "Automated update");

                return true;
            }

            return false; // Complaint not found
        }

        /**
         * Retrieve today's complaint metrics (counts of open and closed).
         */
        public Map<String, Integer> getTodaysComplaintMetrics() {
            Map<String, Integer> metrics = new HashMap<>();
            int todayOpenComplaints = complaintLogRepository.getTodaysOpenComplaints();
            int todayClosedComplaints = complaintLogRepository.getTodaysClosedComplaints();

            metrics.put("todayOpenComplaints", todayOpenComplaints);
            metrics.put("todayClosedComplaints", todayClosedComplaints);

            return metrics;
        }

        /**
         * Retrieve city-wise today's complaint metrics (open and closed).
         */
        public Map<String, Map<String, Integer>> getCityWiseTodaysComplaintMetrics() {
            Map<String, Map<String, Integer>> cityWiseMetrics = new HashMap<>();

            // Fetch open complaints grouped by city
            List<Object[]> openComplaints = complaintLogRepository.getCityWiseTodaysOpenComplaints();
            if (openComplaints != null && !openComplaints.isEmpty()) {
                for (Object[] result : openComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int openCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("todayOpenComplaints", openCount);
                }
            }

            // Fetch closed complaints grouped by city
            List<Object[]> closedComplaints = complaintLogRepository.getCityWiseTodaysClosedComplaints();
            if (closedComplaints != null && !closedComplaints.isEmpty()) {
                for (Object[] result : closedComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int closedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("todayClosedComplaints", closedCount);
                }
            }

            return cityWiseMetrics;
        }

        /**
         * Fetch today's open/closed complaints grouped by bank.
         */
        public Map<String, Map<String, Integer>> getBankWiseTodaysComplaintMetrics() {
            Map<String, Map<String, Integer>> bankWiseMetrics = new HashMap<>();

            // Fetch open complaints grouped by bank
            List<Object[]> openComplaints = complaintLogRepository.getBankWiseTodaysOpenComplaints();
            if (openComplaints != null && !openComplaints.isEmpty()) {
                for (Object[] result : openComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int openCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("todayOpenComplaints", openCount);
                }
            }

            // Fetch closed complaints grouped by bank
            List<Object[]> closedComplaints = complaintLogRepository.getBankWiseTodaysClosedComplaints();
            if (closedComplaints != null && !closedComplaints.isEmpty()) {
                for (Object[] result : closedComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int closedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("todayClosedComplaints", closedCount);
                }
            }

            return bankWiseMetrics;
        }

        // -------------------------------------------------------------------------
        // NEW METHODS: All-time (unfiltered by date) City-wise and Bank-wise
        // -------------------------------------------------------------------------
        /**
         * Retrieve overall complaint metrics (open and closed counts).
         */
        public Map<String, Integer> getOverallComplaintMetrics() {
            Map<String, Integer> metrics = new HashMap<>();

            // Define statuses that are considered as "Open"
            List<String> openStatuses = List.of(
                    "Open",
                    "FOC",
                    "Quotation",
                    "Network Issue",
                    "Visit Schedule",
                    "Hardware Picked",
                    "Visit On Hold",
                    "Dispatched",
                    "Delivered"
            );

            // Count total complaints with open statuses
            int totalOpenComplaints = complaintLogRepository.countByComplaintStatusIn(openStatuses);

            // Count total complaints with "Closed" status
            int totalClosedComplaints = complaintLogRepository.countByComplaintStatus("Closed");

            // Count total complaints with "Wait For Approval" status
            int totalWaitForApprovalComplaints = complaintLogRepository.countByComplaintStatus("Wait For Approval");

            // Count total complaints with "Approved" status
            int totalApprovedComplaints = complaintLogRepository.countByComplaintStatus("Approved");

            // Add metrics to the map
            metrics.put("totalOpenComplaints", totalOpenComplaints);
            metrics.put("totalClosedComplaints", totalClosedComplaints);
            metrics.put("totalWaitForApprovalComplaints", totalWaitForApprovalComplaints);
            metrics.put("totalApprovedComplaints", totalApprovedComplaints);

            return metrics;
        }

        /**
         * Retrieve all-time city-wise complaints metrics (open and closed).
         */
        public Map<String, Map<String, Integer>> getCityWiseAllComplaintMetrics() {
            Map<String, Map<String, Integer>> cityWiseMetrics = new HashMap<>();

            // Fetch open complaints grouped by city (all-time)
            List<Object[]> openComplaints = complaintLogRepository.getCityWiseAllOpenComplaints();
            if (openComplaints != null && !openComplaints.isEmpty()) {
                for (Object[] result : openComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int openCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allOpenComplaints", openCount);
                }
            }
            // Fetch quotation complaints grouped by city (all-time)
            List<Object[]> quotationComplaints = complaintLogRepository.getCityWiseAllQuotationComplaints();
            if (quotationComplaints != null && !quotationComplaints.isEmpty()) {
                for (Object[] result : quotationComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int quotationCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allQuotationComplaints", quotationCount);
                }
            }
            // Fetch visit schedule complaints grouped by city (all-time)
            List<Object[]> visitScheduleComplaints = complaintLogRepository.getCityWiseAllVisitScheduleComplaints();
            if (visitScheduleComplaints != null && !visitScheduleComplaints.isEmpty()) {
                for (Object[] result : visitScheduleComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int visitScheduleCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allVisitScheduleComplaints", visitScheduleCount);
                }
            }
            // Fetch FOC complaints grouped by city (all-time)
            List<Object[]> focComplaints = complaintLogRepository.getCityWiseAllFocComplaints();
            if (focComplaints != null && !focComplaints.isEmpty()) {
                for (Object[] result : focComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int focCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allFocComplaints", focCount);
                }
            }

            // Fetch closed complaints grouped by city (all-time)
            List<Object[]> closedComplaints = complaintLogRepository.getCityWiseAllClosedComplaints();
            if (closedComplaints != null && !closedComplaints.isEmpty()) {
                for (Object[] result : closedComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int closedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allClosedComplaints", closedCount);
                }
            }
            // Fetch hardware picked complaints grouped by city (all-time)
            List<Object[]> hardwarePickedComplaints = complaintLogRepository.getCityWiseAllHardwarePickedComplaints();
            if (hardwarePickedComplaints != null && !hardwarePickedComplaints.isEmpty()) {
                for (Object[] result : hardwarePickedComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int hardwarePickedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allHardwarePickedComplaints", hardwarePickedCount);
                }
            }
            // Fetch approved complaints grouped by city (all-time)
            List<Object[]> approvedComplaints = complaintLogRepository.getCityWiseAllApprovedComplaints();
            if (approvedComplaints != null && !approvedComplaints.isEmpty()) {
                for (Object[] result : approvedComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int approvedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allApprovedComplaints", approvedCount);
                }
            }
            // Fetch wait for approval complaints grouped by city (all-time)
            List<Object[]> waitForApprovalComplaints = complaintLogRepository.getCityWiseAllWaitForApprovalComplaints();
            if (waitForApprovalComplaints != null && !waitForApprovalComplaints.isEmpty()) {
                for (Object[] result : waitForApprovalComplaints) {
                    String city = result[0] != null ? result[0].toString().trim().toUpperCase() : "OTHERS";
                    int waitForApprovalCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    cityWiseMetrics.putIfAbsent(city, new HashMap<>());
                    cityWiseMetrics.get(city).put("allWaitForApprovalComplaints", waitForApprovalCount);
                }
            }

            return cityWiseMetrics;
        }

        /**
         * Retrieve all-time bank-wise complaints metrics (open and closed).
         */
        public Map<String, Map<String, Integer>> getBankWiseAllComplaintMetrics() {
            Map<String, Map<String, Integer>> bankWiseMetrics = new HashMap<>();

            // Fetch open complaints grouped by bank (all-time)
            List<Object[]> openComplaints = complaintLogRepository.getBankWiseAllOpenComplaints();
            if (openComplaints != null && !openComplaints.isEmpty()) {
                for (Object[] result : openComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int openCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allOpenComplaints", openCount);
                }
            }

            // Fetch closed complaints grouped by bank (all-time)
            List<Object[]> closedComplaints = complaintLogRepository.getBankWiseAllClosedComplaints();
            if (closedComplaints != null && !closedComplaints.isEmpty()) {
                for (Object[] result : closedComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int closedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allClosedComplaints", closedCount);
                }
            }
    // Fetch hardware picked complaints grouped by bank (all-time)
            List<Object[]> hardwarePickedComplaints = complaintLogRepository.getBankWiseAllHardwarePickedComplaints();
            if (hardwarePickedComplaints != null && !hardwarePickedComplaints.isEmpty()) {
                for (Object[] result : hardwarePickedComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int hardwarePickedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allHardwarePickedComplaints", hardwarePickedCount);
                }
            }

            // Fetch approved complaints grouped by bank (all-time)
            List<Object[]> approvedComplaints = complaintLogRepository.getBankWiseAllApprovedComplaints();
            if (approvedComplaints != null && !approvedComplaints.isEmpty()) {
                for (Object[] result : approvedComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int approvedCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allApprovedComplaints", approvedCount);
                }
            }

            // Fetch wait for approval complaints grouped by bank (all-time)
            List<Object[]> waitForApprovalComplaints = complaintLogRepository.getBankWiseAllWaitForApprovalComplaints();
            if (waitForApprovalComplaints != null && !waitForApprovalComplaints.isEmpty()) {
                for (Object[] result : waitForApprovalComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int waitForApprovalCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allWaitForApprovalComplaints", waitForApprovalCount);
                }
            }
            // Fetch Visit Schedule complaints grouped by bank (all-time)
            List<Object[]> visitScheduleComplaints = complaintLogRepository.getBankWiseVisitScheduleComplaints();
            if (visitScheduleComplaints != null && !visitScheduleComplaints.isEmpty()) {
                for (Object[] result : visitScheduleComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int visitScheduleCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allVisitScheduleComplaints", visitScheduleCount);
                }
            }
            // Fetch Quotation complaints grouped by bank (all-time)
            List<Object[]> quotationComplaints = complaintLogRepository.getBankWiseQuotationComplaints();
            if (quotationComplaints != null && !quotationComplaints.isEmpty()) {
                for (Object[] result : quotationComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int quotationCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allQuotationComplaints", quotationCount);
                }
            }
            // Fetch FOC complaints grouped by bank (all-time)
            List<Object[]> focComplaints = complaintLogRepository.getBankWiseFocComplaints();
            if (focComplaints != null && !focComplaints.isEmpty()) {
                for (Object[] result : focComplaints) {
                    String bank = result[0] != null ? result[0].toString().trim() : "UNKNOWN";
                    int focCount = result[1] != null ? ((Number) result[1]).intValue() : 0;

                    bankWiseMetrics.putIfAbsent(bank, new HashMap<>());
                    bankWiseMetrics.get(bank).put("allFocComplaints", focCount);
                }
            }
            return bankWiseMetrics;
        }
        /**
         * Retrieve all complaints and also populate their related HardwareLogs (and courierStatus).
         */
        public List<ComplaintLog> getAllComplaintsWithHardwareLogs() {
            // 1) Fetch all complaints
            List<ComplaintLog> allComplaints = complaintLogRepository.findAll();
            if (allComplaints.isEmpty()) {
                return allComplaints;
            }

            // 2) Fetch all related HardwareLogs using `findByComplaintLogIn(...)`
            List<HardwareLog> hardwareLogs = hardwareLogRepository.findByComplaintLogIn(allComplaints);

            // 3) Group hardware logs by the ComplaintLog ID
            Map<Long, List<HardwareLog>> hwLogsByComplaintId = hardwareLogs.stream()
                    .collect(Collectors.groupingBy(hw -> hw.getComplaintLog().getId()));

            // 4) Attach the grouped HardwareLogs to the corresponding ComplaintLog’s transient fields
            for (ComplaintLog complaint : allComplaints) {
                List<HardwareLog> hwForComplaint = hwLogsByComplaintId.getOrDefault(complaint.getId(), List.of());
                complaint.setHardwareLogs(hwForComplaint);

                // Set a single courierStatus from the first HW log (if applicable)
                if (!hwForComplaint.isEmpty()) {
                    HardwareLog firstHwLog = hwForComplaint.get(0);
                    complaint.setCourierStatus(firstHwLog.getCourierStatus());

                    // Instead of calling getReport(), use the new set of reports.
                    Set<HardwareReport> reportsSet = firstHwLog.getReports();
                    if (reportsSet != null && !reportsSet.isEmpty()) {
                        // Get the first report using iterator
                        HardwareReport firstReport = reportsSet.iterator().next();
                        complaint.setReport(firstReport.getContent());
                    } else {
                        complaint.setReport(null);
                    }

                    complaint.setEquipmentDescription(firstHwLog.getEquipmentDescription());
                } else {
                    complaint.setCourierStatus(null);
                    complaint.setReport(null);
                    complaint.setHardwareLogs(null);
                }
            }

            return allComplaints;
        }

        public List<ComplaintLog> getAllComplaintsWithHardwareLogsAndReports() {
            List<ComplaintLog> complaints = complaintLogRepository.findAll();
            if (complaints.isEmpty()) return complaints;

            // Use the new query method to fetch hardware logs with reports
            List<HardwareLog> hwLogsWithReports = hardwareLogRepository.findAllWithReportsByComplaints(complaints);

            // Group by complaint ID
            Map<Long, List<HardwareLog>> grouped = hwLogsWithReports.stream()
                    .collect(Collectors.groupingBy(hw -> hw.getComplaintLog().getId()));

            // Attach them to the complaints
            for (ComplaintLog complaint : complaints) {
                List<HardwareLog> logs = grouped.getOrDefault(complaint.getId(), List.of());
                complaint.setHardwareLogs(logs);

                // Optional: Attach first report content & courier status
                if (!logs.isEmpty()) {
                    HardwareLog firstLog = logs.get(0);
                    complaint.setCourierStatus(firstLog.getCourierStatus());

                    // If reports is now a Set<HardwareReport>
                    Set<HardwareReport> reportsSet = firstLog.getReports();
                    if (reportsSet != null && !reportsSet.isEmpty()) {
                        HardwareReport firstReport = reportsSet.iterator().next();
                        complaint.setReport(firstReport.getContent());
                    } else {
                        complaint.setReport(null);
                    }

                    complaint.setEquipmentDescription(firstLog.getEquipmentDescription());
                }
            }

            return complaints;
        }


        public Map<String, Object> getComplaintDashboardSummary() {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.valueOf(today);

            List<ComplaintLog> allComplaints = complaintLogRepository.findAll();

            // Today logged complaints with allowed statuses
            Set<String> validStatuses = Set.of("Open", "Hardware Picked", "Wait For Approval", "FOC", "Approved","Closed");

            long todayLogged = allComplaints.stream()
                    .filter(c -> todayDate.equals(c.getDate()))
                    .count();

            long sameDayClosed = allComplaints.stream()
                    .filter(c -> todayDate.equals(c.getDate()) && todayDate.equals(c.getClosedDate()))
                    .filter(c -> "Closed".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long hardwarePickedToday = allComplaints.stream()
                    .filter(c -> todayDate.equals(c.getDate()) && todayDate.equals(c.getHardwarePickedDate()))
                    .filter(c -> "Hardware Picked".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long waitForApprovalToday = allComplaints.stream()
                    .filter(c -> todayDate.equals(c.getDate()) && todayDate.equals(c.getQuotationDate()))
                    .filter(c -> "Wait For Approval".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long focToday = allComplaints.stream()
                    .filter(c -> todayDate.equals(c.getDate()) && todayDate.equals(c.getFocDate()))
                    .filter(c -> "FOC".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long approvedToday = allComplaints.stream()
                    .filter(c -> todayDate.equals(c.getDate()) && todayDate.equals(c.getApprovedDate()))
                    .filter(c -> "Approved".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long pendingToday = todayLogged - sameDayClosed - hardwarePickedToday - waitForApprovalToday - focToday - approvedToday;

            // OLD complaints — before today
            long oldLogged = allComplaints.stream()
                    .filter(c -> c.getDate() != null && c.getDate().before(todayDate))
                    .filter(c -> "Open".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long oldClosed = allComplaints.stream()
                    .filter(c -> c.getClosedDate() != null && todayDate.equals(c.getClosedDate()))
                    .filter(c -> c.getDate() == null || !todayDate.equals(c.getDate()))
                    .filter(c -> "Closed".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long oldHardwarePicked = allComplaints.stream()
                    .filter(c -> c.getHardwarePickedDate() != null && todayDate.equals(c.getHardwarePickedDate()))
                    .filter(c -> c.getDate() == null || !todayDate.equals(c.getDate()))
                    .filter(c -> "Hardware Picked".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long oldApproved = allComplaints.stream()
                    .filter(c -> c.getApprovedDate() != null && todayDate.equals(c.getApprovedDate()))
                    .filter(c -> c.getDate() == null || !todayDate.equals(c.getDate()))
                    .filter(c -> "Approved".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();
            long oldWaitForApproval = allComplaints.stream()
                    .filter(c -> c.getQuotationDate() != null && todayDate.equals(c.getQuotationDate()))
                    .filter(c -> c.getDate() == null || !todayDate.equals(c.getDate()))
                    .filter(c -> "Wait For Approval".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();


            long oldFoc = allComplaints.stream()
                    .filter(c -> c.getFocDate() != null && todayDate.equals(c.getFocDate()))
                    .filter(c -> c.getDate() == null || !todayDate.equals(c.getDate()))
                    .filter(c -> "FOC".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                    .count();

            long oldPending = oldLogged - oldClosed - oldHardwarePicked - oldWaitForApproval - oldApproved - oldFoc;

            // Total stats

            // Final response
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("todayComplaintLogged", Map.of(
                    "logged", todayLogged,
                    "sameDayClose", sameDayClosed,
                    "hardwarePicked", hardwarePickedToday,
                    "waitForApproval", waitForApprovalToday,
                    "approved", approvedToday,
                    "foc", focToday,
                    "pending", pendingToday
            ));

            result.put("oldComplaintLogged", Map.of(
                    "logged", oldLogged,
                    "closed", oldClosed,
                    "hardwarePicked", oldHardwarePicked,
                    "approved", oldApproved,
                    "foc", oldFoc,
                    "waitForApproval", oldWaitForApproval,
                    "pending", oldPending
            ));

            result.put("completeDetail", Map.of(
                    "logged", todayLogged + oldLogged,
                    "Closed", sameDayClosed + oldClosed,
                    "hardwarePicked", hardwarePickedToday + oldHardwarePicked,
                    "waitForApproval", waitForApprovalToday + oldWaitForApproval,
                    "approved", approvedToday + oldApproved,
                    "foc", focToday + oldFoc,
                    "pending", pendingToday + oldPending
            ));


            return result;
        }

        public Map<String, Object> getCityWiseEngineerSummary() {
            LocalDate today = LocalDate.now();
            Date todayDate = Date.valueOf(today);

            // Use a list for majorCities if you want specific display order.
            List<String> majorCities = Arrays.asList(
                    "Karachi", "Lahore", "Islamabad", "Peshawar", "Hyderabad", "Quetta",
                    "Sukkur", "Sadiqabad", "Bahawalpur", "Multan", "Sahiwal", "Jhang",
                    "Faisalabad", "Sargodha", "Sialkot", "Jhelum", "Abbottabad"
            );

            List<ComplaintLog> allComplaints = complaintLogRepository.findAll();

            // Complaints with real visit activity today
            Map<String, Map<String, List<ComplaintLog>>> groupedByCityAndEngineer =
                    allComplaints.stream()
                            .filter(c ->
                                    c.getVisitorId() != null &&
                                            (
                                                    todayDate.equals(c.getHardwarePickedDate()) ||
                                                            todayDate.equals(c.getPendingForClosedDate()) ||
                                                            todayDate.equals(c.getClosedDate())
                                            )
                            )
                            .collect(Collectors.groupingBy(
                                    ComplaintLog::getCity,
                                    Collectors.groupingBy(ComplaintLog::getVisitorName)
                            ));

            // All complaints grouped by city for pending calculation
            Map<String, List<ComplaintLog>> groupedByCity = allComplaints.stream()
                    .collect(Collectors.groupingBy(ComplaintLog::getCity));

            Map<String, Object> finalResult = new LinkedHashMap<>();
            Map<String, Object> othersBlock = new LinkedHashMap<>(); // Temporarily collect "Others"

            // First, process major cities (in the order of majorCities list)
            for (String city : majorCities) {
                if (!groupedByCity.containsKey(city)) continue;

                List<ComplaintLog> cityComplaints = groupedByCity.get(city);
                Map<String, List<ComplaintLog>> engineers = groupedByCityAndEngineer.getOrDefault(city, Map.of());

                List<Map<String, Object>> engineerStats = new ArrayList<>();
                int totalComplaints = 0;
                int totalService = 0;

                for (String eng : engineers.keySet()) {
                    List<ComplaintLog> logs = engineers.getOrDefault(eng, List.of());

                    int complaints = logs.size();
                    int serviceVisits = (int) logs.stream()
                            .filter(c -> "Service".equalsIgnoreCase(c.getComplaintType()))
                            .count();

                    totalComplaints += complaints;
                    totalService += serviceVisits;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", eng);
                    row.put("city", city);
                    row.put("complaints", complaints);
                    row.put("serviceVisit", serviceVisits);
                    engineerStats.add(row);
                }

                long todayPending = cityComplaints.stream()
                        .filter(c -> todayDate.equals(c.getDate()))
                        .filter(c -> !"Closed".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                        .count();

                long oldPending = cityComplaints.stream()
                        .filter(c -> c.getDate() != null && c.getDate().before(todayDate))
                        .filter(c -> "Open".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                        .count();

                Map<String, Object> totals = Map.of(
                        "complaintsAttended", totalComplaints,
                        "total", totalComplaints + totalService,
                        "todayPending", todayPending,
                        "oldPending", oldPending,
                        "totalPending", todayPending + oldPending
                );

                Map<String, Object> cityBlock = new LinkedHashMap<>();
                cityBlock.put("engineers", engineerStats);
                cityBlock.put("totals", totals);

                finalResult.put(city, cityBlock);
            }

            // Then, process all non-major cities and group as "Others"
            for (String city : groupedByCity.keySet()) {
                if (majorCities.contains(city)) continue;

                List<ComplaintLog> cityComplaints = groupedByCity.get(city);
                Map<String, List<ComplaintLog>> engineers = groupedByCityAndEngineer.getOrDefault(city, Map.of());

                List<Map<String, Object>> engineerStats = new ArrayList<>();
                int totalComplaints = 0;
                int totalService = 0;

                for (String eng : engineers.keySet()) {
                    List<ComplaintLog> logs = engineers.getOrDefault(eng, List.of());

                    int complaints = logs.size();
                    int serviceVisits = (int) logs.stream()
                            .filter(c -> "Service".equalsIgnoreCase(c.getComplaintType()))
                            .count();

                    totalComplaints += complaints;
                    totalService += serviceVisits;

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", eng);
                    row.put("city", city);
                    row.put("complaints", complaints);
                    row.put("serviceVisit", serviceVisits);
                    engineerStats.add(row);
                }

                long todayPending = cityComplaints.stream()
                        .filter(c -> todayDate.equals(c.getDate()))
                        .filter(c -> !"Closed".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                        .count();

                long oldPending = cityComplaints.stream()
                        .filter(c -> c.getDate() != null && c.getDate().before(todayDate))
                        .filter(c -> "Open".equalsIgnoreCase(safeStatus(c.getComplaintStatus())))
                        .count();

                Map<String, Object> totals = Map.of(
                        "complaintsAttended", totalComplaints,
                        "total", totalComplaints + totalService,
                        "todayPending", todayPending,
                        "oldPending", oldPending,
                        "totalPending", todayPending + oldPending
                );

                // Merge engineers
                List<Map<String, Object>> othersEngineers = (List<Map<String, Object>>) othersBlock.getOrDefault("engineers", new ArrayList<>());
                othersEngineers.addAll(engineerStats);

                // Merge totals
                Map<String, Object> existingTotals = (Map<String, Object>) othersBlock.getOrDefault("totals", new LinkedHashMap<>());
                Map<String, Object> newTotals = mergeTotals(existingTotals, totals);

                othersBlock.put("engineers", othersEngineers);
                othersBlock.put("totals", newTotals);
            }

            // Add "Others" at the end
            if (!othersBlock.isEmpty()) {
                finalResult.put("Others", othersBlock);
            }

            return finalResult;
        }


        // Helper method to safely merge two totals maps
        private Map<String, Object> mergeTotals(Map<String, Object> base, Map<String, Object> add) {
            Map<String, Object> merged = new LinkedHashMap<>();

            for (String key : add.keySet()) {
                int baseVal = base.getOrDefault(key, 0) instanceof Integer ? (int) base.getOrDefault(key, 0) : 0;
                int addVal = add.getOrDefault(key, 0) instanceof Integer ? (int) add.getOrDefault(key, 0) : 0;
                merged.put(key, baseVal + addVal);
            }

            return merged;
        }

        // Helper method for null-safe status
        private String safeStatus(String status) {
            return status == null ? "" : status.trim();
        }
        public String getPreviousNonVisitScheduleStatus(String complaintId) {
            // Get all history entries for this complaint, most recent first
            List<ComplaintHistory> history = complaintHistoryRepository
                    .findByComplaintIdOrderByChangeDateDesc(complaintId);

            // Find the last status *before* "Visit Schedule"
            for (ComplaintHistory entry : history) {
                if ("complaintStatus".equals(entry.getFieldName()) &&
                        !"Visit Schedule".equals(entry.getNewValue())) {
                    return entry.getNewValue();
                }
            }
            return "Open"; // Fallback
        }
        public Map<String, Map<String, Integer>> getDashboardCounts() {
            // List of statuses for "Open"
            List<String> openStatuses = Arrays.asList(
                    "Open", "FOC", "Quotation", "Network Issue", "Visit Schedule", "Hardware Picked",
                    "Visit On Hold", "Dispatched", "Delivered", "Received Inward", "Dispatch Inward",
                    "Marked In Pool", "On Call", "Testing", "Renovation", "Disapproved",
                    "Additional Counter", "Verify Approval", "BFC Approval", "AHO Approval", "BFC/AHO","Pre Approved"
            );

            Map<String, Map<String, Integer>> result = new LinkedHashMap<>();

            // Open
            result.put("open", Map.of(
                    "overall", complaintLogRepository.countAllTimeByStatuses(openStatuses),
                    "today", complaintLogRepository.countTodayByStatuses(openStatuses)
            ));

            // In Progress
            List<String> inProgressStatuses = Collections.singletonList("In Progress");
            result.put("inProgress", Map.of(
                    "overall", complaintLogRepository.countAllTimeByStatuses(inProgressStatuses),
                    "today", complaintLogRepository.countTodayByStatuses(inProgressStatuses)
            ));

            // Closed: "today" now means same-day closed (opened & closed today)
            result.put("closed", Map.of(
                    "overall", complaintLogRepository.countAllTimeByStatus("Closed"),
                    "today", complaintLogRepository.countSameDayClosed() // <--- changed here
            ));

            // Approved
            result.put("approved", Map.of(
                    "overall", complaintLogRepository.countAllTimeByStatus("Approved"),
                    "today", complaintLogRepository.countTodayByStatus("Approved")
            ));

            // Wait For Approval
            result.put("waitForApproval", Map.of(
                    "overall", complaintLogRepository.countAllTimeByStatus("Wait For Approval"),
                    "today", complaintLogRepository.countTodayByStatus("Wait For Approval")
            ));

            // Pending For Closed
            result.put("pendingForClosed", Map.of(
                    "overall", complaintLogRepository.countAllTimeByStatus("Pending For Closed"),
                    "today", complaintLogRepository.countTodayByStatus("Pending For Closed")
            ));

            // Overall (all complaints)
            result.put("overall", Map.of(
                    "overall", complaintLogRepository.countAllComplaints()
            ));

            // Total Closed: "today" means all complaints closed today (regardless of open date)
            result.put("totalClosed", Map.of(
                    "today", complaintLogRepository.countClosedToday() // <--- changed here
            ));

            // Today's Registered (all complaints logged today)
            result.put("todaysRegistered", Map.of(
                    "today", complaintLogRepository.countTodaysRegistered()
            ));

            return result;
        }
        public Page<ComplaintLog> getComplaintsByStatuses(List<String> statuses, Pageable pageable) {
            return complaintLogRepository.findByComplaintStatusInOrderByIdDesc(statuses, pageable);
        }

        public Page<ComplaintLog> getComplaintsByStatusPaginated(String status, Pageable pageable) {
            return complaintLogRepository.findByComplaintStatusOrderByIdDesc(status, pageable);
        }
        public Page<ComplaintLog> getAllComplaintsPaginated(Pageable pageable) {
            return complaintLogRepository.findAllByOrderByIdDesc(pageable);
        }

        public Page<ComplaintBranchGroupDTO> searchComplaints(
                String status,
                String bankName,
                String branchCode,
                String branchName,
                String engineerName,
                List<String> city,
                String complaintStatus,
                String subStatus,
                String dateFrom,
                String dateTo,
                String approvedDateFrom,
                String approvedDateTo,
                String closedDateFrom,
                String closedDateTo,
                String quotationDateFrom,
                String quotationDateTo,
                String pendingForClosedDateFrom,
                String pendingForClosedDateTo,
                String date,                // exact date
                String approvedDate,
                String closedDate,
                String pendingForClosedDate,
                String quotationDate,
                String priority,
                String inPool,
                Boolean hasReport,
                String reportType,
                Pageable pageable
        ) {
            Specification<ComplaintLog> spec = Specification.where(null);

            // --- Status filtering ---
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase("Open")) {
                    List<String> openStatuses = Arrays.asList(
                            "Open", "FOC", "Quotation", "Network Issue", "Visit Schedule", "Hardware Picked",
                            "Visit On Hold", "Dispatched", "Delivered", "Received Inward", "Dispatch Inward",
                            "Marked In Pool", "On Call", "Testing", "Renovation", "Disapproved",
                            "Additional Counter", "Verify Approval", "BFC Approval", "AHO Approval", "BFC/AHO", "Approved", "Wait For Approval"
                    );
                    spec = spec.and((root, query, cb) -> root.get("complaintStatus").in(openStatuses));
                } else if (status.equalsIgnoreCase("FOC_APPROVED")) {
                    spec = spec.and((root, query, cb) -> root.get("complaintStatus").in(Arrays.asList("FOC", "Approved")));
                } else if (!status.equalsIgnoreCase("Overall")) {
                    spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("complaintStatus")), status.toLowerCase()));
                }
            }

            // --- Partial matches ---
            if (bankName != null && !bankName.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));
            }
            int codeLength = 4; // Use your actual branch code length here!
            if (branchCode != null && !branchCode.trim().isEmpty()) {
                String trimmed = branchCode.trim();
                if (trimmed.matches("\\d+")) { // only digits
                    String unpadded = trimmed.replaceFirst("^0+(?!$)", "");
                    String padded = String.format("%0" + codeLength + "d", Integer.parseInt(unpadded.isEmpty() ? "0" : unpadded));
                    spec = spec.and((root, query, cb) ->
                            cb.or(
                                    cb.equal(root.get("branchCode"), padded),
                                    cb.equal(root.get("branchCode"), unpadded)
                            )
                    );
                } else {
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("branchCode"), trimmed));
                }
            }
            if (branchName != null && !branchName.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("branchName")), "%" + branchName.toLowerCase() + "%"));
            }
            if (engineerName != null && !engineerName.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("visitorName")), "%" + engineerName.toLowerCase() + "%"));
            }
            if (city != null && !city.isEmpty()) {
                if (city.size() == 1) {
                    String singleCity = city.get(0);
                    spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("city")), "%" + singleCity.toLowerCase() + "%"));
                } else {
                    spec = spec.and((root, query, cb) ->
                            root.get("city").in(city.stream().map(String::toLowerCase).toList())
                    );
                }
            }

            // --- Complaint Status ---
            if (complaintStatus != null && !complaintStatus.isEmpty()) {
                if (complaintStatus.equalsIgnoreCase("FOC_APPROVED")) {
                    spec = spec.and((root, query, cb) -> root.get("complaintStatus").in(Arrays.asList("FOC", "Approved")));
                } else {
                    spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("complaintStatus")), complaintStatus.toLowerCase()));
                }
            }
            if (subStatus != null && !subStatus.isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("complaintStatus")), "%" + subStatus.toLowerCase() + "%")
                );
            }

            // --- Priority (boolean) ---
            if (priority != null && !priority.isEmpty()) {
                boolean isPriority = Boolean.parseBoolean(priority);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), isPriority));
            }
            // --- In Pool (boolean) ---
            if (inPool != null && !inPool.isEmpty()) {
                boolean markedInPool = Boolean.parseBoolean(inPool);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("markedInPool"), markedInPool));
            }
            // --- Has Report (boolean) ---
            if (hasReport != null && hasReport) {
                spec = spec.and((root, query, cb) -> {
                    Subquery<Long> subquery = query.subquery(Long.class);
                    Root<HardwareLog> hardwareLogRoot = subquery.from(HardwareLog.class);
                    Join<HardwareLog, ?> reportJoin = hardwareLogRoot.join("reports", JoinType.INNER);
                    subquery.select(hardwareLogRoot.get("id"))
                            .where(
                                    cb.equal(hardwareLogRoot.get("complaintLog"), root),
                                    cb.isNotNull(reportJoin.get("id"))
                            );
                    return cb.exists(subquery);
                });
            }
            // --- Report type ---
            if (reportType != null && !reportType.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("reportType")), "%" + reportType.toLowerCase() + "%"));
            }

            // --- Date Ranges ---
            Date dateFromParsed = parseDate(dateFrom, "dateFrom");
            if (dateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), dateFromParsed));
            }
            Date dateToParsed = parseDate(dateTo, "dateTo");
            if (dateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), dateToParsed));
            }
            Date approvedDateFromParsed = parseDate(approvedDateFrom, "approvedDateFrom");
            if (approvedDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("approvedDate"), approvedDateFromParsed));
            }
            Date approvedDateToParsed = parseDate(approvedDateTo, "approvedDateTo");
            if (approvedDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("approvedDate"), approvedDateToParsed));
            }
            Date closedDateFromParsed = parseDate(closedDateFrom, "closedDateFrom");
            if (closedDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("closedDate"), closedDateFromParsed));
            }
            Date closedDateToParsed = parseDate(closedDateTo, "closedDateTo");
            if (closedDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("closedDate"), closedDateToParsed));
            }
            Date quotationDateFromParsed = parseDate(quotationDateFrom, "quotationDateFrom");
            if (quotationDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quotationDate"), quotationDateFromParsed));
            }
            Date quotationDateToParsed = parseDate(quotationDateTo, "quotationDateTo");
            if (quotationDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("quotationDate"), quotationDateToParsed));
            }
            Date pendingForClosedDateFromParsed = parseDate(pendingForClosedDateFrom, "pendingForClosedDateFrom");
            if (pendingForClosedDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("pendingForClosedDate"), pendingForClosedDateFromParsed));
            }
            Date pendingForClosedDateToParsed = parseDate(pendingForClosedDateTo, "pendingForClosedDateTo");
            if (pendingForClosedDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("pendingForClosedDate"), pendingForClosedDateToParsed));
            }

            // --- Exact match for individual dates ---
            if (date != null && !date.isEmpty()) {
                try {
                    Date sqlDate = Date.valueOf(date);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("date"), sqlDate));
                } catch (Exception e) {}
            }
            if (approvedDate != null && !approvedDate.isEmpty()) {
                try {
                    Date sqlApprovedDate = Date.valueOf(approvedDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("approvedDate"), sqlApprovedDate));
                } catch (Exception e) {}
            }
            if (closedDate != null && !closedDate.isEmpty()) {
                try {
                    Date sqlClosedDate = Date.valueOf(closedDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("closedDate"), sqlClosedDate));
                } catch (Exception e) {}
            }
            if (quotationDate != null && !quotationDate.isEmpty()) {
                try {
                    Date sqlQuotationDate = Date.valueOf(quotationDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("quotationDate"), sqlQuotationDate));
                } catch (Exception e) {}
            }
            if (pendingForClosedDate != null && !pendingForClosedDate.isEmpty()) {
                try {
                    Date sqlPendingDate = Date.valueOf(pendingForClosedDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("pendingForClosedDate"), sqlPendingDate));
                } catch (Exception e) {}
            }

            // --- Fetch all filtered complaints ---
            List<ComplaintLog> allComplaints = complaintLogRepository.findAll(spec);

            // --- Set courierStatus/equipmentDescription efficiently (batch) ---
            List<Long> complaintIds = allComplaints.stream()
                    .map(ComplaintLog::getId)
                    .collect(Collectors.toList());

            if (!complaintIds.isEmpty()) {
                List<HardwareLog> allLogs = hardwareLogRepository.findByComplaintLogIdIn(complaintIds);
                Map<Long, List<HardwareLog>> logsByComplaintId = allLogs.stream()
                        .collect(Collectors.groupingBy(log -> log.getComplaintLog().getId()));

                for (ComplaintLog cl : allComplaints) {
                    List<HardwareLog> logs = logsByComplaintId.get(cl.getId());
                    if (logs != null && !logs.isEmpty()) {
                        HardwareLog latestLog = logs.stream()
                                .max(Comparator.comparing(HardwareLog::getId))
                                .orElse(null);
                        if (latestLog != null) {
                            cl.setCourierStatus(latestLog.getCourierStatus());
                            cl.setEquipmentDescription(latestLog.getEquipmentDescription());
                        }
                    }
                }
            }

            // --- Group by bankName + branchCode (case-insensitive, trimmed) ---
            Map<String, List<ComplaintLog>> grouped = allComplaints.stream()
                    .collect(Collectors.groupingBy(c ->
                            (c.getBankName() == null ? "" : c.getBankName().trim().toLowerCase()) + "__" +
                                    (c.getBranchCode() == null ? "" : c.getBranchCode().trim().toLowerCase())
                    ));

            // --- Convert groups to DTOs (one DTO per branch) ---
            List<ComplaintBranchGroupDTO> branchGroups = new ArrayList<>();
            for (List<ComplaintLog> group : grouped.values()) {
                if (!group.isEmpty()) {
                    // ✅ Sort complaints in this group by latest date (and id as tiebreaker)
                    group.sort((c1, c2) -> {
                        if (c1.getDate() == null && c2.getDate() == null) return 0;
                        if (c1.getDate() == null) return 1;
                        if (c2.getDate() == null) return -1;
                        int dateCompare = c2.getDate().compareTo(c1.getDate()); // descending
                        if (dateCompare != 0) return dateCompare;
                        return Long.compare(c2.getId(), c1.getId()); // tie-breaker
                    });

                    ComplaintLog first = group.get(0);
                    branchGroups.add(new ComplaintBranchGroupDTO(
                            first.getBankName(),
                            first.getBranchCode(),
                            first.getBranchName(),
                            group
                    ));
                }
            }


            // --- Sort branch groups by the latest complaint (already first in each group) ---
            branchGroups.sort((a, b) -> {
                ComplaintLog latestA = a.getComplaints().isEmpty() ? null : a.getComplaints().get(0);
                ComplaintLog latestB = b.getComplaints().isEmpty() ? null : b.getComplaints().get(0);

                if (latestA == null && latestB == null) return 0;
                if (latestA == null) return 1;
                if (latestB == null) return -1;

                // Compare by date desc, then id desc as tie-breaker
                int dateCompare = latestB.getDate().compareTo(latestA.getDate());
                if (dateCompare != 0) return dateCompare;
                return Long.compare(latestB.getId(), latestA.getId());
            });


            // --- Paging by branch group ---
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), branchGroups.size());
            List<ComplaintBranchGroupDTO> pageGroups = (start > end) ? new ArrayList<>() : branchGroups.subList(start, end);

// --- Total complaints count (across all groups) ---
            long totalComplaints = branchGroups.stream()
                    .mapToLong(group -> group.getComplaints().size())
                    .sum();

// --- Complaints count before this page (for serial offset) ---
            int safeStart = Math.min(start, branchGroups.size()); // guard against OOB
            long complaintsBeforePage = 0L;
            for (int i = 0; i < safeStart; i++) {
                List<ComplaintLog> gc = branchGroups.get(i).getComplaints();
                if (gc != null) complaintsBeforePage += gc.size();
            }

// Stash values on current request so the controller can put them in headers
            RequestAttributes ra = RequestContextHolder.getRequestAttributes();
            if (ra != null) {
                ra.setAttribute("complaintsBeforePage", complaintsBeforePage, RequestAttributes.SCOPE_REQUEST);
                ra.setAttribute("totalGroups", branchGroups.size(), RequestAttributes.SCOPE_REQUEST); // optional
            }

// Keep pagination BY GROUPS; totalElements = TOTAL COMPLAINTS (your requirement)
            return new PageImpl<>(pageGroups, pageable, totalComplaints);

        }


        public Page<ComplaintBranchGroupDTO> searchOpenComplaintsWithBranchFiltering(
                String status,
                String bankName,
                String branchCode,
                String branchName,
                String engineerName,
                List<String> city,
                String complaintStatus,
                String subStatus,
                String dateFrom,
                String dateTo,
                String approvedDateFrom,
                String approvedDateTo,
                String closedDateFrom,
                String closedDateTo,
                String quotationDateFrom,
                String quotationDateTo,
                String pendingForClosedDateFrom,
                String pendingForClosedDateTo,
                String date,
                String approvedDate,
                String closedDate,
                String pendingForClosedDate,
                String quotationDate,
                String priority,
                String inPool,
                Boolean hasReport,
                String reportType,
                Pageable pageable
        ) {
            Specification<ComplaintLog> spec = Specification.where(null);

            // 1. Status logic
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase("Open")) {
                    List<String> openStatuses = Arrays.asList(
                            "Open", "FOC", "Quotation", "Network Issue", "Visit Schedule", "Hardware Picked",
                            "Visit On Hold", "Dispatched", "Delivered", "Received Inward", "Dispatch Inward",
                            "Marked In Pool", "On Call", "Testing", "Renovation", "Disapproved",
                            "Additional Counter", "Verify Approval", "BFC Approval", "AHO Approval", "BFC/AHO",
                            "Approved", "Wait For Approval","Pre Approved"
                    );
                    spec = spec.and((root, query, cb) -> root.get("complaintStatus").in(openStatuses));
                } else if (status.equalsIgnoreCase("FOC_APPROVED")) {
                    // NEW: Match either "FOC" or "Approved"
                    spec = spec.and((root, query, cb) -> root.get("complaintStatus").in(Arrays.asList("FOC", "Approved")));
                } else if (!status.equalsIgnoreCase("Overall")) {
                    spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("complaintStatus")), status.toLowerCase()));
                }
            }

            // 2. Partial match fields
            if (bankName != null && !bankName.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));
            }
            int codeLength = 4; // or whatever your code length is
            if (branchCode != null && !branchCode.trim().isEmpty()) {
                String trimmed = branchCode.trim();
                if (trimmed.matches("\\d+")) { // only digits
                    String unpadded = trimmed.replaceFirst("^0+(?!$)", ""); // remove leading zeros
                    String padded = String.format("%0" + codeLength + "d", Integer.parseInt(unpadded.isEmpty() ? "0" : unpadded));
                    spec = spec.and((root, query, cb) ->
                            cb.or(
                                    cb.equal(root.get("branchCode"), padded),
                                    cb.equal(root.get("branchCode"), unpadded)
                            )
                    );
                } else {
                    // fallback for non-numeric: search as-is
                    spec = spec.and((root, query, cb) ->
                            cb.equal(root.get("branchCode"), trimmed)
                    );
                }
            }


            if (branchName != null && !branchName.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("branchName")), "%" + branchName.toLowerCase() + "%"));
            }
            if (engineerName != null && !engineerName.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("visitorName")), "%" + engineerName.toLowerCase() + "%"));
            }
            if (city != null && !city.isEmpty()) {
                if (city.size() == 1) {
                    String singleCity = city.get(0);
                    spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("city")), "%" + singleCity.toLowerCase() + "%"));
                } else {
                    spec = spec.and((root, query, cb) ->
                            root.get("city").in(city.stream().map(String::toLowerCase).toList())
                    );
                }
            }

            // 3. Dropdown/exact match fields
            if (complaintStatus != null && !complaintStatus.isEmpty()) {
                if (complaintStatus.equalsIgnoreCase("FOC_APPROVED")) {
                    // NEW: Match either "FOC" or "Approved"
                    spec = spec.and((root, query, cb) -> root.get("complaintStatus").in(Arrays.asList("FOC", "Approved")));
                } else {
                    spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("complaintStatus")), complaintStatus.toLowerCase()));
                }
            }
            if (subStatus != null && !subStatus.isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("complaintStatus")), "%" + subStatus.toLowerCase() + "%")
                );
            }

            // 4. Priority (boolean)
            if (priority != null && !priority.isEmpty()) {
                boolean isPriority = Boolean.parseBoolean(priority);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), isPriority));
            }
            // 5. In Pool (boolean)
            if (inPool != null && !inPool.isEmpty()) {
                boolean markedInPool = Boolean.parseBoolean(inPool);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("markedInPool"), markedInPool));
            }
            // 6. Has Report (boolean)
            if (hasReport != null && hasReport) {
                spec = spec.and((root, query, cb) -> {
                    Subquery<Long> subquery = query.subquery(Long.class);
                    Root<HardwareLog> hardwareLogRoot = subquery.from(HardwareLog.class);
                    Join<HardwareLog, ?> reportJoin = hardwareLogRoot.join("reports", JoinType.INNER);
                    subquery.select(hardwareLogRoot.get("id"))
                            .where(
                                    cb.equal(hardwareLogRoot.get("complaintLog"), root),
                                    cb.isNotNull(reportJoin.get("id"))
                            );
                    return cb.exists(subquery);
                });
            }
            // 7. Report type
            if (reportType != null && !reportType.isEmpty()) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("reportType")), "%" + reportType.toLowerCase() + "%"));
            }

            // 8. Date Range (main date field)
            Date dateFromParsed = parseDate(dateFrom, "dateFrom");
            if (dateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), dateFromParsed));
            }
            Date dateToParsed = parseDate(dateTo, "dateTo");
            if (dateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), dateToParsed));
            }
            Date approvedDateFromParsed = parseDate(approvedDateFrom, "approvedDateFrom");
            if (approvedDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("approvedDate"), approvedDateFromParsed));
            }
            Date approvedDateToParsed = parseDate(approvedDateTo, "approvedDateTo");
            if (approvedDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("approvedDate"), approvedDateToParsed));
            }
            Date closedDateFromParsed = parseDate(closedDateFrom, "closedDateFrom");
            if (closedDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("closedDate"), closedDateFromParsed));
            }
            Date closedDateToParsed = parseDate(closedDateTo, "closedDateTo");
            if (closedDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("closedDate"), closedDateToParsed));
            }
            Date quotationDateFromParsed = parseDate(quotationDateFrom, "quotationDateFrom");
            if (quotationDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quotationDate"), quotationDateFromParsed));
            }
            Date quotationDateToParsed = parseDate(quotationDateTo, "quotationDateTo");
            if (quotationDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("quotationDate"), quotationDateToParsed));
            }
            Date pendingForClosedDateFromParsed = parseDate(pendingForClosedDateFrom, "pendingForClosedDateFrom");
            if (pendingForClosedDateFromParsed != null) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("pendingForClosedDate"), pendingForClosedDateFromParsed));
            }
            Date pendingForClosedDateToParsed = parseDate(pendingForClosedDateTo, "pendingForClosedDateTo");
            if (pendingForClosedDateToParsed != null) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("pendingForClosedDate"), pendingForClosedDateToParsed));
            }

            // 9. Individual Dates (exact match)
            if (date != null && !date.isEmpty()) {
                try {
                    Date sqlDate = Date.valueOf(date);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("date"), sqlDate));
                } catch (Exception e) {}
            }
            if (approvedDate != null && !approvedDate.isEmpty()) {
                try {
                    Date sqlApprovedDate = Date.valueOf(approvedDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("approvedDate"), sqlApprovedDate));
                } catch (Exception e) {}
            }
            if (closedDate != null && !closedDate.isEmpty()) {
                try {
                    Date sqlClosedDate = Date.valueOf(closedDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("closedDate"), sqlClosedDate));
                } catch (Exception e) {}
            }
            if (quotationDate != null && !quotationDate.isEmpty()) {
                try {
                    Date sqlQuotationDate = Date.valueOf(quotationDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("quotationDate"), sqlQuotationDate));
                } catch (Exception e) {}
            }
            if (pendingForClosedDate != null && !pendingForClosedDate.isEmpty()) {
                try {
                    Date sqlPendingDate = Date.valueOf(pendingForClosedDate);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("pendingForClosedDate"), sqlPendingDate));
                } catch (Exception e) {}
            }

            // -------------- BRANCH GROUP PAGING LOGIC --------------

            // 1. Fetch all filtered complaints (no paging yet)
            List<ComplaintLog> allComplaints = complaintLogRepository.findAll(spec);

// ======== Set courierStatus and equipmentDescription efficiently ========
            List<Long> complaintIds = allComplaints.stream()
                    .map(ComplaintLog::getId)
                    .collect(Collectors.toList());

            if (!complaintIds.isEmpty()) {
                List<HardwareLog> allLogs = hardwareLogRepository.findByComplaintLogIdIn(complaintIds);

                Map<Long, List<HardwareLog>> logsByComplaintId = allLogs.stream()
                        .collect(Collectors.groupingBy(log -> log.getComplaintLog().getId()));

                for (ComplaintLog cl : allComplaints) {
                    List<HardwareLog> logs = logsByComplaintId.get(cl.getId());
                    if (logs != null && !logs.isEmpty()) {
                        HardwareLog latestLog = logs.stream()
                                .max(Comparator.comparing(HardwareLog::getId))
                                .orElse(null);
                        if (latestLog != null) {
                            cl.setCourierStatus(latestLog.getCourierStatus());
                            cl.setEquipmentDescription(latestLog.getEquipmentDescription());
                        }
                    }
                }
            }

            // 2. Group by bankName + branchCode
            Map<String, List<ComplaintLog>> grouped = allComplaints.stream()
                    .collect(Collectors.groupingBy(c ->
                            (c.getBankName() == null ? "" : c.getBankName().trim().toLowerCase()) + "__" +
                                    (c.getBranchCode() == null ? "" : c.getBranchCode().trim().toLowerCase())
                    ));

            // 3. Only include groups where not all are "Wait For Approval"
            List<ComplaintBranchGroupDTO> branchGroups = new ArrayList<>();
            for (List<ComplaintLog> group : grouped.values()) {
                boolean allWait = group.stream()
                        .allMatch(c -> {
                            String statusVal = c.getComplaintStatus() == null ? "" : c.getComplaintStatus().trim().toLowerCase();
                            return statusVal.equals("wait for approval");
                        });
                if (!allWait) {
                    ComplaintLog first = group.get(0);
                    branchGroups.add(new ComplaintBranchGroupDTO(
                            first.getBankName(),
                            first.getBranchCode(),
                            first.getBranchName(),
                            group
                    ));
                }
            }

            // 4. Sort groups by latest complaint date DESC
            branchGroups.sort((a, b) -> {
                Date aDate = a.getComplaints().stream()
                        .map(ComplaintLog::getDate)
                        .filter(Objects::nonNull)
                        .max(Date::compareTo).orElse(null);
                Date bDate = b.getComplaints().stream()
                        .map(ComplaintLog::getDate)
                        .filter(Objects::nonNull)
                        .max(Date::compareTo).orElse(null);
                if (aDate == null && bDate == null) return 0;
                if (aDate == null) return 1;
                if (bDate == null) return -1;
                return aDate.compareTo(bDate); // DESC
            });

            // 5. Paging the branch groups (not complaints)
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), branchGroups.size());
            List<ComplaintBranchGroupDTO> pageGroups = (start > end) ? new ArrayList<>() : branchGroups.subList(start, end);

// --- Calculate how many complaints are before this page ---
            long complaintsBeforePage = branchGroups.stream()
                    .limit(start) // all groups before this page
                    .mapToLong(g -> g.getComplaints().size())
                    .sum();

// Store this in request attributes so controller can send it as header
            RequestAttributes ra = RequestContextHolder.getRequestAttributes();
            if (ra != null) {
                ra.setAttribute("complaintsBeforePage", complaintsBeforePage, RequestAttributes.SCOPE_REQUEST);
            }

// 6. Return as Page of groups
            return new PageImpl<>(pageGroups, pageable, branchGroups.size());

        }



        public boolean existsOpenComplaint(String bankName, String branchCode) {
            return complaintLogRepository.existsByBankNameIgnoreCaseAndBranchCodeAndComplaintStatusNot(
                    bankName.trim(), branchCode.trim(), "Closed"
            );
        }



    }
