package com.system.complaints.repository;

import com.system.complaints.model.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DispatchStatusRepository extends JpaRepository<DispatchStatus, Long> {
}
