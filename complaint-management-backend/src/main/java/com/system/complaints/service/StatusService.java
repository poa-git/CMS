package com.system.complaints.service;

import com.system.complaints.model.Status;
import com.system.complaints.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatusService {

    @Autowired
    private StatusRepository statusRepository;

    public Status getStatusById(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));
    }

    // Method to retrieve all statuses
    public List<Status> getAllStatuses() {
        return statusRepository.findAll();
    }
}
