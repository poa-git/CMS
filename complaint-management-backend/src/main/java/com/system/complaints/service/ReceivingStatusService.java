package com.system.complaints.service;

import com.system.complaints.model.ReceivingStatus;
import com.system.complaints.repository.ReceivingStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReceivingStatusService {

    @Autowired
    private ReceivingStatusRepository receivingStatusRepository;

    /**
     * Retrieve all ReceivingStatus records.
     */
    public List<ReceivingStatus> getAllReceivingStatuses() {
        return receivingStatusRepository.findAll();
    }
}
