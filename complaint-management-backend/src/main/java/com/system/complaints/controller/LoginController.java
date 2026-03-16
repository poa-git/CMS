package com.system.complaints.controller;

import com.system.complaints.model.Visitor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.system.complaints.repository.UserRepository;
import com.system.complaints.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
public class LoginController {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/user/info")
    public Map<String, Object> getUserInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Platform-Type") String platformTypeHeader) {

        logger.info("Processing /user/info request for user: {}", userDetails.getUsername());

        Map<String, Object> response = new HashMap<>();
        AppUser appUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userDetails.getUsername());
                    return new RuntimeException("User not found");
                });

        if (!platformTypeHeader.equalsIgnoreCase(appUser.getPlatformType().toString())) {
            logger.error("Platform mismatch for user: {}. Expected: {}, Received: {}",
                    userDetails.getUsername(), appUser.getPlatformType(), platformTypeHeader);
            throw new RuntimeException("Access denied: Platform mismatch.");
        }
        Visitor visitor = appUser.getVisitor();
        Long visitorId = appUser.getVisitor() != null ? appUser.getVisitor().getId() : null;
        String city = visitor != null ? visitor.getCity() : null;
        String userType = appUser.getUserType() !=null? appUser.getUserType().toString():null;
        if (visitorId == null) {
            logger.warn("Visitor ID is null for user: {}", userDetails.getUsername());
        } else {
            logger.info("Visitor ID for user {}: {}", userDetails.getUsername(), visitorId);
        }

        response.put("message", "Welcome, " + userDetails.getUsername() + "! You have USER role access.");
        response.put("username", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()));
        response.put("platformType", appUser.getPlatformType());
        response.put("visitorId", visitorId);
        response.put("city", city);
        response.put("userType",userType);
        return response;
    }

    @GetMapping("/admin/info")
    public Map<String, Object> getAdminInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Platform-Type") String platformTypeHeader) {

        logger.info("Processing /admin/info request for user: {}", userDetails.getUsername());

        Map<String, Object> response = new HashMap<>();
        AppUser appUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userDetails.getUsername());
                    return new RuntimeException("User not found");
                });

        if (!platformTypeHeader.equalsIgnoreCase(appUser.getPlatformType().toString())) {
            logger.error("Platform mismatch for user: {}. Expected: {}, Received: {}",
                    userDetails.getUsername(), appUser.getPlatformType(), platformTypeHeader);
            throw new RuntimeException("Access denied: Platform mismatch.");
        }

        Long visitorId = appUser.getVisitor() != null ? appUser.getVisitor().getId() : null;
        if (visitorId == null) {
            logger.warn("Visitor ID is null for user: {}", userDetails.getUsername());
        } else {
            logger.info("Visitor ID for user {}: {}", userDetails.getUsername(), visitorId);
        }

        response.put("message", "Welcome, " + userDetails.getUsername() + "! You have ADMIN role access.");
        response.put("username", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList()));
        response.put("platformType", appUser.getPlatformType());
        response.put("visitorId", visitorId);
        return response;
    }
}
