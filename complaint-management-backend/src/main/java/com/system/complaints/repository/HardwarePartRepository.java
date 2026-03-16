package com.system.complaints.repository;

import com.system.complaints.dto.PartInfoDTO;
import com.system.complaints.model.HardwarePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HardwarePartRepository extends JpaRepository<HardwarePart, Long> {

    // Existing
    List<HardwarePart> findByComplaintLogId(Long complaintLogId);
    @Query("SELECT new com.system.complaints.dto.PartInfoDTO(" +
            "h.hardwareName, c.branchCode, c.bankName, c.branchName) " +
            "FROM HardwarePart h " +
            "JOIN h.complaintLog c")
    List<PartInfoDTO> fetchPartDetails();

}
