package com.system.complaints.repository;

import com.system.complaints.model.ReceivingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceivingStatusRepository extends JpaRepository<ReceivingStatus, Long> {
}
