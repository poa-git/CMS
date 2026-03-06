package com.system.complaints.service;

import com.system.complaints.model.Parts;
import com.system.complaints.repository.PartsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartsService {

    @Autowired
    private PartsRepository partsRepository;

    // Fetch all parts
    public List<Parts> getAllParts() {
        return partsRepository.findAll();
    }

    // Add a new part
    public Parts addPart(Parts part) {
        return partsRepository.save(part);
    }

    // Delete a part by ID
    public void deletePartById(Long id) {
        partsRepository.deleteById(id);
    }
}
