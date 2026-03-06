package com.system.complaints.repository;

import com.system.complaints.model.LabStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabStatusRepository extends JpaRepository<LabStatus, Long> {
}
