package com.system.complaints.repository;

import com.system.complaints.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    // existing method - untouched
    Visitor findByName(String name);

    // NEW method (case-insensitive)
    Optional<Visitor> findFirstByNameIgnoreCase(String name);
}
