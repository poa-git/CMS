package com.system.complaints.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.system.complaints.model.Location;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // Find the most recent location for a specific visitor
    @Query("SELECT l FROM Location l WHERE l.visitor.id = :visitorId ORDER BY l.timestamp DESC")
    Optional<Location> findTopByVisitorIdOrderByTimestampDesc(@Param("visitorId") Long visitorId);

    // Find all locations for a specific visitor within a time range with pagination
    @Query("SELECT l FROM Location l WHERE l.visitor.id = :visitorId AND l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    List<Location> findLocationsByVisitorIdAndTimestampBetween(
            @Param("visitorId") Long visitorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    // Get the most recent location for all visitors
    @Query("SELECT l FROM Location l JOIN FETCH l.visitor " +
            "WHERE (l.visitor.id, l.timestamp) IN " +
            "(SELECT subL.visitor.id, MAX(subL.timestamp) FROM Location subL GROUP BY subL.visitor.id)")
    List<Location> findLatestLocationsForAllVisitors();

    // Find the most recent location for all visitors filtered by visitor name
    @Query("SELECT l FROM Location l " +
            "WHERE l.timestamp = (SELECT MAX(subL.timestamp) FROM Location subL WHERE subL.visitor.id = l.visitor.id) " +
            "AND LOWER(l.visitor.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Location> findLatestLocationsByVisitorName(@Param("name") String name);

    // Find all locations for a specific visitor sorted by timestamp with pagination
    @Query("SELECT l FROM Location l WHERE l.visitor.id = :visitorId ORDER BY l.timestamp DESC")
    List<Location> findLocationsByVisitorIdSortedByTimestamp(@Param("visitorId") Long visitorId, Pageable pageable);

    // Count locations for a specific visitor
    @Query("SELECT COUNT(l) FROM Location l WHERE l.visitor.id = :visitorId")
    long countLocationsByVisitorId(@Param("visitorId") Long visitorId);

    // Find all locations recorded within a global time range for all visitors
    @Query("SELECT l FROM Location l WHERE l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    List<Location> findLocationsByTimestampBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);
}
