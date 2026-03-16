package com.system.complaints.service;

import com.system.complaints.model.PendingForClosedLog;
import com.system.complaints.repository.PendingForClosedLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PendingForClosedLogService {

    @Autowired
    private PendingForClosedLogRepository repository;

    public void logPendingForClosed(String complaintId, String source, Long userId, String username) {
        PendingForClosedLog log = new PendingForClosedLog(complaintId, source, userId, username);
        repository.save(log);
    }

    public long countMobileMarked() {
        return repository.countMarkedFromMobile();
    }

    public long countPortalMarked() {
        return repository.countMarkedFromPortal();
    }
}
