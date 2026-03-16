package com.system.complaints.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class HardwareBroadcasts {

    private final HardwareLogService hardwareLogService;

    @Autowired
    public HardwareBroadcasts(HardwareLogService hardwareLogService) {
        this.hardwareLogService = hardwareLogService;
    }

    /**
     * Broadcast AFTER the surrounding transaction commits.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHardwareChanged(HardwareLogService.HardwareDomainChangedEvent e) {
        hardwareLogService.broadcastCourierStatusCounts();
        hardwareLogService.broadcastTrendsPerDate();
    }
}
