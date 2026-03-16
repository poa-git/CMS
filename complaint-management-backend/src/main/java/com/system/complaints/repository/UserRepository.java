package com.system.complaints.repository;

import com.system.complaints.model.AppUser;
import com.system.complaints.model.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    List<AppUser> findByPlatformType(PlatformType platformType); // Query by platform type

    // Explicit, but not necessary
    List<AppUser> findAll();
}
