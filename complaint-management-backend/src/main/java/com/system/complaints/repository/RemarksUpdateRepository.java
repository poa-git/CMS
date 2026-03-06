package com.system.complaints.repository;

import com.system.complaints.model.RemarksUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface RemarksUpdateRepository extends JpaRepository<RemarksUpdate, Long> {

    // Single complaint history (already existed)
    List<RemarksUpdate> findByComplaintLogIdOrderByTimestampDesc(Long complaintId);

    // Single complaint count (already existed)
    long countByComplaintLogId(Long complaintId);

    // ✅ Batch history for many complaints in one query
    @Query("SELECT r FROM RemarksUpdate r " +
            "WHERE r.complaintLog.id IN :complaintIds " +
            "ORDER BY r.complaintLog.id, r.timestamp DESC")
    List<RemarksUpdate> findByComplaintLogIds(@Param("complaintIds") List<Long> complaintIds);

    // ✅ Batch counts for many complaints in one query
    @Query("SELECT r.complaintLog.id AS complaintId, COUNT(r) AS cnt " +
            "FROM RemarksUpdate r " +
            "WHERE r.complaintLog.id IN :complaintIds " +
            "GROUP BY r.complaintLog.id")
    List<Object[]> countByComplaintLogIds(@Param("complaintIds") List<Long> complaintIds);
}
