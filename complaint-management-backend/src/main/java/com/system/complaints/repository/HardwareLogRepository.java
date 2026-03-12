package com.system.complaints.repository;

import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.HardwareLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HardwareLogRepository extends JpaRepository<HardwareLog, Long>, JpaSpecificationExecutor<HardwareLog> {


    Optional<HardwareLog> findTopByComplaintLog_ComplaintIdOrderByIdDesc(String complaintId);

    /**
     * Find a HardwareLog by its associated ComplaintLog.
     */
    Optional<HardwareLog> findByComplaintLog(ComplaintLog complaintLog);

    /**
     * Check if a HardwareLog exists for a specific ComplaintLog.
     */
    boolean existsByComplaintLog(ComplaintLog complaintLog);

    /**
     * Find all HardwareLogs associated with a list of ComplaintLogs.
     */
    List<HardwareLog> findByComplaintLogIn(List<ComplaintLog> complaintLogs);

    @Query("""
    SELECT DISTINCT hl
    FROM HardwareLog hl
    LEFT JOIN FETCH hl.reports
    LEFT JOIN FETCH hl.complaintLog
""")
    List<HardwareLog> findAllWithReports();

    @Query("""
SELECT DISTINCT hl
FROM HardwareLog hl
LEFT JOIN FETCH hl.reports
WHERE hl.complaintLog IN :complaints
""")
    List<HardwareLog> findAllWithReportsByComplaints(List<ComplaintLog> complaints);

    @Query("SELECT hl FROM HardwareLog hl LEFT JOIN FETCH hl.complaintLog WHERE hl.dispatchOutwardDate = :date")
    List<HardwareLog> findAllByDispatchOutwardDate(Date date);

    List<HardwareLog> findByLabEngineerIgnoreCase(String username);

    @Query("""
    SELECT DISTINCT hl
    FROM HardwareLog hl
    LEFT JOIN FETCH hl.complaintLog cl
    LEFT JOIN FETCH cl.hardwareParts hp
""")
    List<HardwareLog> findAllWithParts();


    @Query("""
    SELECT DISTINCT hl
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    JOIN cl.hardwareParts hp
    LEFT JOIN FETCH hl.reports r
    WHERE LOWER(hp.assignedEngineer) = LOWER(:username)
""")
    List<HardwareLog> findAllWithPartsAndReportsAssignedTo(String username);

    List<HardwareLog> findByComplaintLogIdIn(List<Long> complaintLogIds);

    @Query("""
    SELECT 
        LOWER(COALESCE(hl.courierStatus, 'unknown')) AS status,
        COUNT(hl) AS cnt,
        SUM(CASE WHEN LOWER(hl.courierStatus) = 'hardware ready' 
                  AND LOWER(cl.complaintStatus) IN ('approved', 'foc') 
                 THEN 1 ELSE 0 END) AS hardwareReadyApprovedFoc,
        SUM(CASE WHEN LOWER(hl.courierStatus) = 'hardware ready' 
                  AND LOWER(cl.complaintStatus) = 'wait for approval' 
                 THEN 1 ELSE 0 END) AS hardwareReadyWaitApproval
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    WHERE LOWER(cl.complaintStatus) NOT IN ('closed', 'pending for closed')
    GROUP BY LOWER(COALESCE(hl.courierStatus, 'unknown'))
    ORDER BY cnt DESC
    """)
    List<Object[]> getCourierStatusCountsWithHardwareReadyBreakdown();

    @Query("""
    SELECT 'hardwareReady' AS type, DATE(hl.hOkDate) AS logDate, COUNT(hl.id) AS cnt
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    WHERE hl.hOkDate IS NOT NULL
      AND LOWER(cl.complaintStatus) NOT IN ('closed', 'pending for closed')
    GROUP BY DATE(hl.hOkDate)

    UNION ALL

    SELECT 'dispatchInward' AS type, DATE(hl.dispatchInwardDate), COUNT(hl.id)
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    WHERE hl.dispatchInwardDate IS NOT NULL
      AND LOWER(cl.complaintStatus) NOT IN ('closed', 'pending for closed')
    GROUP BY DATE(hl.dispatchInwardDate)

    UNION ALL

    SELECT 'receivedInward' AS type, DATE(hl.receivedInwardDate), COUNT(hl.id)
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    WHERE hl.receivedInwardDate IS NOT NULL
      AND LOWER(cl.complaintStatus) NOT IN ('closed', 'pending for closed')
    GROUP BY DATE(hl.receivedInwardDate)

    UNION ALL

    SELECT 'dispatchOutward' AS type, DATE(hl.dispatchOutwardDate), COUNT(hl.id)
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    WHERE hl.dispatchOutwardDate IS NOT NULL
      AND LOWER(cl.complaintStatus) NOT IN ('closed', 'pending for closed')
    GROUP BY DATE(hl.dispatchOutwardDate)

    UNION ALL

    SELECT 'receivedOutward' AS type, DATE(hl.receivedOutwardDate), COUNT(hl.id)
    FROM HardwareLog hl
    JOIN hl.complaintLog cl
    WHERE hl.receivedOutwardDate IS NOT NULL
      AND LOWER(cl.complaintStatus) NOT IN ('closed', 'pending for closed')
    GROUP BY DATE(hl.receivedOutwardDate)

    ORDER BY logDate ASC
""")
    List<Object[]> getAllTrendsPerDate();


}
