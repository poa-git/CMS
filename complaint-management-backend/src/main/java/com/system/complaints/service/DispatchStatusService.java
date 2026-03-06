package com.system.complaints.service;

import com.system.complaints.model.DispatchStatus;
import com.system.complaints.repository.DispatchStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DispatchStatusService {

    @Autowired
    private DispatchStatusRepository dispatchStatusRepository;

    /**
     * Retrieve all DispatchStatus records.
     */
    public List<DispatchStatus> getAllDispatchStatuses() {
        return dispatchStatusRepository.findAll();
    }
}
