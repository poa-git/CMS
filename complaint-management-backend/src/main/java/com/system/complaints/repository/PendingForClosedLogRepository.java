package com.system.complaints.repository;

import com.system.complaints.model.PendingForClosedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PendingForClosedLogRepository extends JpaRepository<PendingForClosedLog, Long> {

    // OLD (kept in case used elsewhere)
    @Query("SELECT COUNT(p) FROM PendingForClosedLog p WHERE p.source = 'MOBILE'")
    long countMarkedFromMobile();

    @Query("SELECT COUNT(p) FROM PendingForClosedLog p WHERE p.source = 'PORTAL'")
    long countMarkedFromPortal();

    @Query("SELECT COUNT(p) FROM PendingForClosedLog p WHERE p.complaintId = :complaintId")
    long countByComplaintId(@Param("complaintId") String complaintId);


    // ✅ FIXED: Count DISTINCT complaintIds for a source from the visible list
    @Query("SELECT COUNT(DISTINCT p.complaintId) " +
            "FROM PendingForClosedLog p " +
            "WHERE p.source = :source " +
            "AND p.complaintId IN :complaintIds")
    long countDistinctBySourceAndComplaintIdIn(
            @Param("source") String source,
            @Param("complaintIds") List<String> complaintIds
    );


    // ✅ NEW: Get DISTINCT complaintIds for the given source
    @Query("SELECT DISTINCT p.complaintId " +
            "FROM PendingForClosedLog p " +
            "WHERE p.source = :source " +
            "AND p.complaintId IN :complaintIds")
    List<String> getDistinctComplaintIdsBySource(
            @Param("source") String source,
            @Param("complaintIds") List<String> complaintIds
    );
}
