package com.familyfinance.budget.controller;

import com.familyfinance.budget.dto.*;
import com.familyfinance.budget.service.AllowancePolicyService;
import com.familyfinance.budget.service.AllowanceRequestService;
import com.familyfinance.budget.support.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/allowances")
public class AllowanceController {

    private final AllowancePolicyService policyService;
    private final AllowanceRequestService requestService;
    private final CurrentUserResolver currentUserResolver;

    public AllowanceController(
            AllowancePolicyService policyService,
            AllowanceRequestService requestService,
            CurrentUserResolver currentUserResolver) {
        this.policyService = policyService;
        this.requestService = requestService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/policies")
    public List<AllowancePolicyResponse> listPolicies(@RequestParam(required = false) UUID familyId) {
        return policyService.findByFamily(familyId);
    }

    @PostMapping("/policies")
    @ResponseStatus(HttpStatus.CREATED)
    public AllowancePolicyResponse createPolicy(@Valid @RequestBody CreateAllowancePolicyRequest request) {
        return policyService.create(request);
    }

    @GetMapping("/policies/{id}")
    public AllowancePolicyResponse getPolicy(@PathVariable UUID id) {
        return policyService.findById(id);
    }

    @GetMapping("/requests")
    public List<AllowanceRequestResponse> listRequests(@RequestParam(required = false) UUID familyId) {
        return requestService.findByFamily(familyId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AllowanceRequestResponse createRequest(@Valid @RequestBody CreateAllowanceRequest request) {
        return requestService.create(request);
    }

    @GetMapping("/requests/{id}")
    public AllowanceRequestResponse getRequest(@PathVariable UUID id) {
        return requestService.findById(id);
    }

    @PostMapping("/requests/{id}/approve")
    public AllowanceRequestResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveAllowanceRequest body,
            HttpServletRequest httpRequest) {
        UUID actor = currentUserResolver.requireUserProfileId(httpRequest);
        return requestService.approve(id, body, actor);
    }

    @PostMapping("/requests/{id}/reject")
    public AllowanceRequestResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectAllowanceRequest body) {
        return requestService.reject(id, body);
    }

    @PostMapping("/requests/{id}/cancel")
    public AllowanceRequestResponse cancel(@PathVariable UUID id) {
        return requestService.cancel(id);
    }

    @PostMapping("/requests/{id}/disbursed")
    public AllowanceRequestResponse markDisbursed(@PathVariable UUID id) {
        return requestService.markDisbursed(id);
    }

    @PostMapping("/requests/{id}/disbursement-failed")
    public AllowanceRequestResponse markDisbursementFailed(@PathVariable UUID id) {
        return requestService.markDisbursementFailed(id);
    }
}