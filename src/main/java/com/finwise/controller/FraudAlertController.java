package com.finwise.controller;

import com.finwise.dto.FraudAlertResponse;
import com.finwise.service.FraudAlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:5173")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @Autowired
    public FraudAlertController(FraudAlertService fraudAlertService) {
        this.fraudAlertService = fraudAlertService;
    }

    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();
    }

    @GetMapping
    public ResponseEntity<List<FraudAlertResponse>> getActiveAlerts() {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(fraudAlertService.getActiveAlerts(email));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getAlertCounts() {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(fraudAlertService.getAlertsCount(email));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<String> dismissAlerts(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        fraudAlertService.dismissAlert(id, email);
        return ResponseEntity.ok("Alert dismissed");
    }
}
