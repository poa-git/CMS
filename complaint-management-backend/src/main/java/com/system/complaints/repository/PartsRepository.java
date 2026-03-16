package com.system.complaints.repository;

import com.system.complaints.model.Parts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartsRepository extends JpaRepository<Parts, Long> {
    // Additional custom query methods can be defined here if needed
}
