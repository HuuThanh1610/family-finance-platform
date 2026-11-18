package com.familyfinance.profile.controller;

import com.familyfinance.profile.dto.UserProfileResponse;
import com.familyfinance.profile.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-profiles")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserProfileResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserProfileResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
