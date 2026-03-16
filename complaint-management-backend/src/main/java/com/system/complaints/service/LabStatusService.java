package com.system.complaints.service;

import com.system.complaints.model.LabStatus;
import com.system.complaints.repository.LabStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabStatusService {

    private final LabStatusRepository labStatusRepository;

    @Autowired
    public LabStatusService(LabStatusRepository labStatusRepository) {
        this.labStatusRepository = labStatusRepository;
    }

    public List<LabStatus> getAllLabStatuses() {
        return labStatusRepository.findAll();
    }
}
