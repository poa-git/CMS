//package com.system.complaints.scheduler;
//
//import com.system.complaints.service.ComplaintLogService;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//public class AgingDaysScheduler {
//
//    private final ComplaintLogService complaintLogService;
//
//    public AgingDaysScheduler(ComplaintLogService complaintLogService) {
//        this.complaintLogService = complaintLogService;
//    }
//
//    /**
//     * Scheduled task to increment agingDays for open complaints.
//     * Runs every day at midnight.
//     */
//    @Scheduled(cron = "0 0 0 * * ?") // Cron expression for midnight
//    public void updateAgingDays() {
//        complaintLogService.incrementAgingDaysForOpenComplaints();
//    }
//}
