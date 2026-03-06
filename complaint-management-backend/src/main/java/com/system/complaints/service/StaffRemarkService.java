package com.system.complaints.service;

import com.system.complaints.model.StaffRemark;
import com.system.complaints.repository.StaffRemarkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffRemarkService {
    private final StaffRemarkRepository repository;

    public StaffRemarkService(StaffRemarkRepository repository) {
        this.repository = repository;
    }

    public List<StaffRemark> getAllRemarks() {
        return repository.findAll();
    }
}
