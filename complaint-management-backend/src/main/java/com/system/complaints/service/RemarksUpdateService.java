package com.system.complaints.service;

import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.RemarksUpdate;
import com.system.complaints.repository.ComplaintLogRepository;
import com.system.complaints.repository.RemarksUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RemarksUpdateService {

    @Autowired
    private ComplaintLogRepository complaintLogRepository;

    @Autowired
    private RemarksUpdateRepository remarksUpdateRepository;

    // Helper: logged-in username
    private String getLoggedInUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    // Add new remark
    public RemarksUpdate addRemarksUpdate(Long complaintId, String remarks) {
        ComplaintLog complaintLog = complaintLogRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        RemarksUpdate remarksUpdate = new RemarksUpdate();
        remarksUpdate.setComplaintLog(complaintLog);
        remarksUpdate.setRemarks(remarks);

        String loggedBy = getLoggedInUsername();
        remarksUpdate.setLoggedBy(loggedBy);

        return remarksUpdateRepository.save(remarksUpdate);
    }

    // Latest remark
    public RemarksUpdate getLatestRemarks(Long complaintId) {
        return remarksUpdateRepository.findByComplaintLogIdOrderByTimestampDesc(complaintId)
                .stream().findFirst().orElse(null);
    }

    // Full history for single complaint
    public List<RemarksUpdate> getRemarksHistory(Long complaintId) {
        return remarksUpdateRepository.findByComplaintLogIdOrderByTimestampDesc(complaintId);
    }

    // Count for single complaint
    public long getRemarksCount(Long complaintId) {
        return remarksUpdateRepository.countByComplaintLogId(complaintId);
    }

    // ✅ Batch history: one query for all complaints
    public Map<Long, List<RemarksUpdate>> getRemarksHistoryBatch(List<Long> complaintIds) {
        List<RemarksUpdate> updates = remarksUpdateRepository.findByComplaintLogIds(complaintIds);

        Map<Long, List<RemarksUpdate>> grouped = updates.stream()
                .collect(Collectors.groupingBy(r -> r.getComplaintLog().getId()));

        // 🔧 Ensure every complaintId exists in the map (even if empty)
        complaintIds.forEach(id -> grouped.putIfAbsent(id, List.of()));

        return grouped;
    }


    // ✅ Batch counts: one query for all complaints
    public Map<Long, Long> getRemarksCountsBatch(List<Long> complaintIds) {
        List<Object[]> counts = remarksUpdateRepository.countByComplaintLogIds(complaintIds);

        Map<Long, Long> result = counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],   // complaintId
                        row -> (Long) row[1]    // count
                ));

        // 🔧 Ensure every complaintId exists in the map (default = 0)
        complaintIds.forEach(id -> result.putIfAbsent(id, 0L));

        return result;
    }
}
