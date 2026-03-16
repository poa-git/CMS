package com.system.complaints.service;

import com.system.complaints.model.ComplaintType;
import com.system.complaints.repository.ComplaintTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintTypeService {

    private final ComplaintTypeRepository complaintTypeRepository;

    @Autowired
    public ComplaintTypeService(ComplaintTypeRepository complaintTypeRepository) {
        this.complaintTypeRepository = complaintTypeRepository;
    }

    public List<ComplaintType> getAllComplaintTypes() {
        return complaintTypeRepository.findAll();
    }

    public ComplaintType saveComplaintType(ComplaintType complaintType) {
        return complaintTypeRepository.save(complaintType);
    }
}
