package com.familyfinance.analytics.controller;

import com.familyfinance.analytics.dto.AnalyticsReportResponse;
import com.familyfinance.analytics.service.AnalyticsReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsReportV1Controller {

    private final AnalyticsReportService reportService;

    public AnalyticsReportV1Controller(AnalyticsReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public AnalyticsReportResponse reports(@RequestParam UUID familyId) {
        return reportService.buildReport(familyId);
    }
}