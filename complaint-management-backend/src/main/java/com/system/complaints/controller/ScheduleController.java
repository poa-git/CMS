package com.system.complaints.controller;

import com.system.complaints.dto.ScheduleDTO;
import com.system.complaints.model.Schedule;
import com.system.complaints.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    /**
     * Get all schedules for a specific date.
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<Schedule>> getSchedulesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        List<Schedule> schedules = scheduleService.findByScheduledFor(Date.valueOf(date));
        return ResponseEntity.ok(schedules);
    }

    /**
     * Get all schedules between two dates (inclusive).
     */
    @GetMapping("/by-range")
    public ResponseEntity<List<ScheduleDTO>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        List<ScheduleDTO> dtos = scheduleService.findScheduleDTOsByRange(Date.valueOf(start), Date.valueOf(end));
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all schedules for a specific complaint.
     */
    @GetMapping("/by-complaint")
    public ResponseEntity<List<Schedule>> getSchedulesByComplaint(@RequestParam String complaintId) {
        List<Schedule> schedules = scheduleService.findByComplaintId(complaintId);
        return ResponseEntity.ok(schedules);
    }

    /**
     * Create a new schedule log (for manual use/testing).
     */
    @PostMapping("/log")
    public ResponseEntity<Schedule> logSchedule(@RequestBody Map<String, String> body) {
        String complaintId = body.get("complaintId");
        Date scheduledFor = Date.valueOf(body.get("scheduledFor"));
        String performedBy = body.getOrDefault("performedBy", "system");
        Schedule schedule = scheduleService.logSchedule(complaintId, scheduledFor, performedBy);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Mark a schedule as successful.
     * Optional: Useful for admin/testing/manual patching.
     */
    @PostMapping("/mark-successful")
    public ResponseEntity<String> markSuccessful(@RequestBody Map<String, String> body) {
        String complaintId = body.get("complaintId");
        Date scheduledFor = Date.valueOf(body.get("scheduledFor"));
        scheduleService.markScheduleSuccessful(complaintId, scheduledFor);
        return ResponseEntity.ok("Schedule marked as successful.");
    }

    /**
     * Mark a schedule as expired.
     * Optional: Useful for admin/testing/manual patching.
     */
    @PostMapping("/mark-expired")
    public ResponseEntity<String> markExpired(@RequestBody Map<String, String> body) {
        String complaintId = body.get("complaintId");
        Date scheduledFor = Date.valueOf(body.get("scheduledFor"));
        scheduleService.markScheduleExpired(complaintId, scheduledFor);
        return ResponseEntity.ok("Schedule marked as expired.");
    }
}
