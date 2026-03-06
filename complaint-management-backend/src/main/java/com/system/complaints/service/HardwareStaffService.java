package com.system.complaints.service;

import com.system.complaints.model.HardwareStaff;
import com.system.complaints.repository.HardwareStaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HardwareStaffService {
    @Autowired
    private HardwareStaffRepository hardwareStaffRepository;

    public List<HardwareStaff> getAllHardwareStaff() {
        return hardwareStaffRepository.findAll();
    }
}

