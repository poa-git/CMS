package com.system.complaints.service;

import com.system.complaints.dto.ScheduleDTO;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.Schedule;
import com.system.complaints.repository.ComplaintLogRepository;
import com.system.complaints.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ComplaintLogRepository complaintLogRepository;
    /**
     * Log a new schedule attempt (when a complaint is scheduled).
     */
    public Schedule logSchedule(String complaintId, Date scheduledFor, String performedBy) {
        // Try to find an existing pending schedule
        List<Schedule> existing = scheduleRepository.findByComplaintId(complaintId)
                .stream()
                .filter(s ->
                        s.getScheduledFor().equals(scheduledFor) &&
                                ("Scheduled".equals(s.getOutcomeStatus()) || s.getSuccessful() == null)
                )
                .toList();

        if (!existing.isEmpty()) {
            // Just update the existing record
            Schedule schedule = existing.get(0);
            schedule.setPerformedBy(performedBy); // Update any other fields as needed
            // Don't change outcomeStatus here
            return scheduleRepository.save(schedule);
        } else {
            // No existing pending schedule, create new
            Schedule schedule = new Schedule();
            schedule.setComplaintId(complaintId);
            schedule.setScheduledFor(scheduledFor);
            schedule.setPerformedBy(performedBy);
            schedule.setSuccessful(null); // Pending outcome
            schedule.setOutcomeStatus("Scheduled");
            return scheduleRepository.save(schedule);
        }
    }


    /**
     * Mark a schedule as successful (when visit completed).
     */
    public void markScheduleSuccessful(String complaintId, Date scheduledFor) {
        List<Schedule> schedules = scheduleRepository.findByComplaintId(complaintId)
                .stream()
                .filter(s -> s.getScheduledFor().equals(scheduledFor) && (s.getSuccessful() == null || !s.getSuccessful()))
                .toList();

        for (Schedule schedule : schedules) {
            schedule.setSuccessful(true);
            schedule.setOutcomeStatus("Successful");
            schedule.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            scheduleRepository.save(schedule);
        }
    }

    /**
     * Mark a schedule as expired (when not completed by the scheduled date).
     */
    public void markScheduleExpired(String complaintId, Date scheduledFor) {
        List<Schedule> schedules = scheduleRepository.findByComplaintId(complaintId)
                .stream()
                .filter(s -> s.getScheduledFor().equals(scheduledFor) && (s.getSuccessful() == null || !s.getSuccessful()))
                .toList();

        for (Schedule schedule : schedules) {
            schedule.setSuccessful(false);
            schedule.setOutcomeStatus("Expired");
            schedule.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            scheduleRepository.save(schedule);
        }
    }

    /**
     * Find all schedules for a specific date.
     */
    public List<Schedule> findByScheduledFor(Date date) {
        return scheduleRepository.findByScheduledFor(date);
    }

    /**
     * Find all schedules for a date range.
     */
    public List<Schedule> findByScheduledForBetween(Date start, Date end) {
        return scheduleRepository.findByScheduledForBetween(start, end);
    }

    /**
     * Find all schedules for a specific complaint.
     */
    public List<Schedule> findByComplaintId(String complaintId) {
        return scheduleRepository.findByComplaintId(complaintId);
    }
    public List<ScheduleDTO> findScheduleDTOsByRange(Date start, Date end) {
        List<Schedule> schedules = scheduleRepository.findByScheduledForBetween(start, end);

        // Get all related ComplaintLogs in a single query for efficiency
        List<String> complaintIds = schedules.stream()
                .map(Schedule::getComplaintId)
                .distinct()
                .toList();
        Map<String, ComplaintLog> logMap = complaintLogRepository.findByComplaintIdIn(complaintIds)
                .stream()
                .collect(Collectors.toMap(ComplaintLog::getComplaintId, c -> c));

        List<ScheduleDTO> dtos = new ArrayList<>();
        for (Schedule s : schedules) {
            ComplaintLog c = logMap.get(s.getComplaintId());
            if (c != null) {
                dtos.add(new ScheduleDTO(s, c));
            }
        }
        return dtos;
    }

    /**
     * Log a schedule cancellation/unschedule (when a visit is unassigned or canceled).
     */
    public void logScheduleCancellation(String complaintId, Date scheduledFor, String performedBy) {
        // Find any existing schedule for this complaint & date that is still pending or scheduled
        List<Schedule> schedules = scheduleRepository.findByComplaintId(complaintId)
                .stream()
                .filter(s -> s.getScheduledFor().equals(scheduledFor) && "Scheduled".equals(s.getOutcomeStatus()))
                .toList();

        if (!schedules.isEmpty()) {
            for (Schedule schedule : schedules) {
                schedule.setSuccessful(false); // Mark as unsuccessful/canceled
                schedule.setOutcomeStatus("Canceled");
                schedule.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                schedule.setPerformedBy(performedBy);
                scheduleRepository.save(schedule);
            }
        } else {
            // If no pending schedule found, optionally create a new log record for the cancel event
            Schedule schedule = new Schedule();
            schedule.setComplaintId(complaintId);
            schedule.setScheduledFor(scheduledFor);
            schedule.setPerformedBy(performedBy);
            schedule.setSuccessful(false);
            schedule.setOutcomeStatus("Canceled");
            schedule.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            scheduleRepository.save(schedule);
        }
    }

}
