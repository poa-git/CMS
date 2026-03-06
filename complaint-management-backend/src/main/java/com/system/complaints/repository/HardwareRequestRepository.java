package com.system.complaints.repository;

import com.system.complaints.model.HardwareRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HardwareRequestRepository extends JpaRepository<HardwareRequest, Long> {
}
