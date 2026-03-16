package com.system.complaints.repository;

import com.system.complaints.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByScheduledFor(Date scheduledFor);
    List<Schedule> findByComplaintId(String complaintId);
    List<Schedule> findByScheduledForBetween(Date start, Date end);
}
