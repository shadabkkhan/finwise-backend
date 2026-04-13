package com.finwise.service;

import com.finwise.dto.FraudAlertResponse;
import com.finwise.model.FraudAlert;
import com.finwise.model.User;
import com.finwise.repository.FraudAlertRepository;
import com.finwise.repository.UserRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FraudAlertService {

    private  final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;

    @Autowired
    public FraudAlertService(FraudAlertRepository fraudAlertRepository, UserRepository userRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
        this.userRepository = userRepository;
    }

    public @Nullable List<FraudAlertResponse> getActiveAlerts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found "));

        List<FraudAlert> alerts = fraudAlertRepository.findByUserAndDismissedFalseOrderByCreatedAtDesc(user);

        return alerts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public @Nullable Long getAlertsCount(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return fraudAlertRepository.countByUserAndDismissedFalse(user);
    }

    public void dismissAlert(Long id, String email) {
        FraudAlert fraudAlert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!fraudAlert.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        fraudAlert.setDismissed(true);
        fraudAlertRepository.save(fraudAlert);
    }

    private FraudAlertResponse mapToResponse(FraudAlert alert) {
        return new FraudAlertResponse(
                alert.getId(),
                alert.getReason(),
                alert.getSeverity(),
                alert.isDismissed(),
                alert.getCreatedAt(),
                alert.getTransaction().getTitle(),
                alert.getTransaction().getAmount()
        );
    }
}
