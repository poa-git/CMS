package com.system.complaints.scheduler;

import com.system.complaints.model.Location;
import com.system.complaints.repository.LocationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LocationBatchProcessor {

    private final List<Location> locationBuffer = new ArrayList<>();
    private final int BATCH_SIZE = 50; // Adjust batch size based on system capacity
    private final LocationRepository locationRepository;

    public LocationBatchProcessor(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public synchronized void addLocation(Location location) {
        locationBuffer.add(location);
        if (locationBuffer.size() >= BATCH_SIZE) {
            flushLocations();
        }
    }

    @Scheduled(fixedRate = 5000) // Flush every 5 seconds
    public synchronized void flushLocations() {
        if (!locationBuffer.isEmpty()) {
            locationRepository.saveAll(locationBuffer);
            locationBuffer.clear();
        }
    }
}
