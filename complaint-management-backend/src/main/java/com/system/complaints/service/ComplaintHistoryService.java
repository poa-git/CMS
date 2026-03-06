package com.system.complaints.service;

import com.system.complaints.model.ComplaintHistory;
import com.system.complaints.repository.ComplaintHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintHistoryService {

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    /**
     * Retrieve the history of a specific complaint by its complaint ID.
     *
     * @param complaintId The ID of the complaint for which history is retrieved.
     * @return A list of ComplaintHistory objects.
     */
    public List<ComplaintHistory> getComplaintHistoryByComplaintId(String complaintId) {
        return complaintHistoryRepository.findByComplaintId(complaintId);
    }

    /**
     * Save a new complaint history entry.
     *
     * @param complaintHistory The ComplaintHistory object to save.
     * @return The saved ComplaintHistory object.
     */
    public ComplaintHistory saveComplaintHistory(ComplaintHistory complaintHistory) {
        return complaintHistoryRepository.save(complaintHistory);
    }
}
