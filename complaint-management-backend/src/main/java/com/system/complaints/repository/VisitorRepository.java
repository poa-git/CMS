package com.system.complaints.repository;

import com.system.complaints.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {
    Visitor findByName(String name);
}
