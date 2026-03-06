package com.system.complaints.service;

import com.system.complaints.model.Visitor;
import com.system.complaints.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitorService {

    @Autowired
    private VisitorRepository visitorRepository;

    public List<Visitor> getAllVisitors() {
        return visitorRepository.findAll();
    }
    // Method to find a visitor's name by ID
    public String findVisitorNameById(Long visitorId) {
        return visitorRepository.findById(visitorId)
                .map(Visitor::getName) // Assuming the Visitor class has a getName() method.
                .orElse(null); // Return null if no visitor is found.
    }
}
