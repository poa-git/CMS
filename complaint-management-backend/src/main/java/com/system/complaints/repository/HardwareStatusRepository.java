package com.system.complaints.repository;

import com.system.complaints.model.HardwareStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HardwareStatusRepository extends JpaRepository<HardwareStatus, Long> {
}
