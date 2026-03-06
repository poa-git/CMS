package com.system.complaints.controller;

import com.system.complaints.model.*;
import com.system.complaints.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.system.complaints.repository.UserRepository;
import com.system.complaints.model.AppUser;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/data")
public class DataController {

    @Autowired
    private BankService bankService;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private CityService cityService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private ComplaintTypeService complaintTypeService;

    @Autowired
    private StaffRemarkService staffRemarkService;

    @Autowired
    private DispatchStatusService dispatchStatusService;

    @Autowired
    private PartsService partsService;

    @Autowired
    private ReceivingStatusService receivingStatusService;

    @Autowired
    private HardwareStaffService hardwareStaffService;

    @Autowired
    private HardwareStatusService hardwareStatusService;

    @Autowired
    private LabStatusService labStatusService;

    @Autowired
    private BranchService branchService; // Inject BranchService

    @Autowired
    private UserRepository userRepository;


    @GetMapping("/banks")
    public List<Bank> getAllBanks() {
        return bankService.getAllBanks();
    }

    @GetMapping("/visitors")
    public List<Visitor> getAllVisitors() {
        return visitorService.getAllVisitors();
    }

    @GetMapping("/cities")
    public List<City> getAllCities() {
        return cityService.getAllCities();
    }

    @GetMapping("/complaint-types")
    public List<ComplaintType> getAllComplaintTypes() {
        return complaintTypeService.getAllComplaintTypes();
    }

    @GetMapping("/statuses")
    public List<Status> getAllStatuses() {
        return statusService.getAllStatuses();
    }

    @GetMapping("/staff-remarks")
    public List<StaffRemark> getAllStaffRemarks() {
        return staffRemarkService.getAllRemarks();
    }

        @GetMapping("/dispatch-status")
    public List<DispatchStatus> getAllDispatchStatuses() {
        return dispatchStatusService.getAllDispatchStatuses();
    }

    @GetMapping("/receiving-status")
    public List<ReceivingStatus> getAllReceivingStatuses() {
        return receivingStatusService.getAllReceivingStatuses();
    }

    @GetMapping("/hardware-staff")
    public List<HardwareStaff> getAllHardwareStaff() {
        return hardwareStaffService.getAllHardwareStaff();
    }

    @GetMapping("/parts")
    public List<Parts> getAllParts() {
        return partsService.getAllParts();
    }

    @GetMapping("/hardware-status")
    public List<HardwareStatus> getAllHardwareStatuses() {
        return hardwareStatusService.getAllHardwareStatuses();
    }

    @GetMapping("/lab-status")
    public List<LabStatus> getAllLabStatuses() {
        return labStatusService.getAllLabStatuses();
    }

    // New endpoint to fetch all branches
    @GetMapping("/branches")
    public List<Branch> getAllBranches() {
        return branchService.getAllBranches();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("visitor_id", user.getVisitor() != null ? user.getVisitor().getId() : null);
            map.put("platformType", user.getPlatformType());
            map.put("user_type", user.getUserType());
            map.put("role", user.getRole().getName()); // Only the role name
            return map;
        }).collect(Collectors.toList());
    }
    @GetMapping("/lab-engineers")
    public List<Map<String, Object>> getLabEngineers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getUserType() == UserType.LAB_USER)
                .map(user -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    return map;
                })
                .collect(Collectors.toList());
    }

}
