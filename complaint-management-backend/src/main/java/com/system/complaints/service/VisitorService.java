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

    // ---------------------------------------------------------
    // Existing methods (NOT modified)
    // ---------------------------------------------------------

    public List<Visitor> getAllVisitors() {
        return visitorRepository.findAll();
    }

    // Method to find a visitor's name by ID
    public String findVisitorNameById(Long visitorId) {
        return visitorRepository.findById(visitorId)
                .map(Visitor::getName)
                .orElse(null);
    }

    // ---------------------------------------------------------
    // NEW METHODS (added safely, nothing overridden)
    // ---------------------------------------------------------

    /**
     * Find a Visitor by name (case-insensitive)
     */
    public Visitor findVisitorByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        return visitorRepository
                .findFirstByNameIgnoreCase(name.trim())
                .orElse(null);
    }

    /**
     * Find Visitor ID by name (case-insensitive)
     */
    public Long findVisitorIdByName(String name) {
        Visitor visitor = findVisitorByName(name);
        return (visitor != null) ? visitor.getId() : null;
    }
}
