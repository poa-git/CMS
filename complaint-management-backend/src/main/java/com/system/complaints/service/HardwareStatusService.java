package com.system.complaints.service;

import com.system.complaints.model.HardwareStatus;
import com.system.complaints.repository.HardwareStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HardwareStatusService {

    @Autowired
    private HardwareStatusRepository hardwareStatusRepository;

    /**
     * Retrieve all HardwareStatus records.
     */
    public List<HardwareStatus> getAllHardwareStatuses() {
        return hardwareStatusRepository.findAll();
    }
}
