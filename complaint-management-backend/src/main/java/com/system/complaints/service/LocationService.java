package com.system.complaints.service;

import com.system.complaints.model.Location;
import com.system.complaints.model.Visitor;
import com.system.complaints.repository.LocationRepository;
import com.system.complaints.repository.VisitorRepository;
import com.system.complaints.scheduler.LocationBatchProcessor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final VisitorRepository visitorRepository;
    private final LocationBatchProcessor locationBatchProcessor;

    public LocationService(LocationRepository locationRepository,
                           VisitorRepository visitorRepository,
                           LocationBatchProcessor locationBatchProcessor) {
        this.locationRepository = locationRepository;
        this.visitorRepository = visitorRepository;
        this.locationBatchProcessor = locationBatchProcessor;
    }

    // Add a new location for a visitor
    public void addLocation(Long visitorId, Location location) {
        // Validate visitor existence
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found with ID: " + visitorId));

        // Set visitor and timestamp
        location.setVisitor(visitor);
        location.setTimestamp(LocalDateTime.now());

        // Add to batch processor for optimized saving
        locationBatchProcessor.addLocation(location);
    }

    // Save a single location (directly, not batched)
    public void saveLocation(Location location) {
        locationRepository.save(location);
    }

    // Get the most recent location for a specific visitor
    public Location getMostRecentLocation(Long visitorId) {
        return locationRepository.findTopByVisitorIdOrderByTimestampDesc(visitorId)
                .orElseThrow(() -> new RuntimeException("No location found for visitor ID: " + visitorId));
    }

    // Get locations for a visitor within a specific time range (paginated)
    public List<Location> getLocationsByVisitorAndTimeRange(Long visitorId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        // Validate visitor existence
        visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found with ID: " + visitorId));

        return locationRepository.findLocationsByVisitorIdAndTimestampBetween(visitorId, start, end, pageable);
    }

    // Get the most recent locations for all visitors
    public List<Location> getMostRecentLocationsForAllVisitors() {
        return locationRepository.findLatestLocationsForAllVisitors();
    }

    // Search for the most recent locations filtered by visitor name
    public List<Location> getMostRecentLocationsByVisitorName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name parameter cannot be null or empty");
        }
        return locationRepository.findLatestLocationsByVisitorName(name);
    }

    // Count the total number of locations for a specific visitor
    public long countLocationsByVisitor(Long visitorId) {
        // Validate visitor existence
        visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found with ID: " + visitorId));

        return locationRepository.countLocationsByVisitorId(visitorId);
    }
}
