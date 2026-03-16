package com.system.complaints.controller;

import com.system.complaints.model.Location;
import com.system.complaints.service.LocationService;
import com.system.complaints.service.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;
    private final RateLimiter rateLimiter;

    @Autowired
    public LocationController(LocationService locationService, RateLimiter rateLimiter) {
        this.locationService = locationService;
        this.rateLimiter = rateLimiter;
    }

    // Update location for a visitor
    @PostMapping("/update/{visitorId}")
    public ResponseEntity<?> updateLocation(@PathVariable Long visitorId, @RequestBody Location location) {
        try {
            // Rate limiting
            if (!rateLimiter.canUpdate(visitorId)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Rate limit exceeded for visitor ID: " + visitorId);
            }

            // Save location via service
            locationService.addLocation(visitorId, location);
            return ResponseEntity.ok("Location updated successfully for visitor ID: " + visitorId);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMessage());
        }
    }

    // Get most recent locations for all visitors
    @GetMapping("/recent")
    public ResponseEntity<?> getMostRecentLocations() {
        try {
            List<Location> recentLocations = locationService.getMostRecentLocationsForAllVisitors();
            return ResponseEntity.ok(recentLocations);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching recent locations: " + ex.getMessage());
        }
    }   

    // Get locations by visitor with pagination
    @GetMapping("/visitor/{visitorId}")
    public ResponseEntity<?> getLocationsByVisitor(
            @PathVariable Long visitorId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            List<Location> locations = locationService.getLocationsByVisitorAndTimeRange(visitorId, start, end, pageable);
            return ResponseEntity.ok(locations);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMessage());
        }
    }

    // Search for locations by visitor name
    @GetMapping("/search")
    public ResponseEntity<?> getMostRecentLocationsByVisitorName(@RequestParam String name) {
        try {
            List<Location> locations = locationService.getMostRecentLocationsByVisitorName(name);
            return ResponseEntity.ok(locations);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error searching locations: " + ex.getMessage());
        }
    }

    // Count locations for a specific visitor
    @GetMapping("/count/{visitorId}")
    public ResponseEntity<?> countLocationsByVisitor(@PathVariable Long visitorId) {
        try {
            long count = locationService.countLocationsByVisitor(visitorId);
            return ResponseEntity.ok(count);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + ex.getMessage());
        }
    }
}
