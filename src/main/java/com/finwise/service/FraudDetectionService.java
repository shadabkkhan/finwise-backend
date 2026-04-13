package com.finwise.service;

import com.finwise.model.FraudAlert;
import com.finwise.model.Transaction;
import com.finwise.repository.FraudAlertRepository;
import com.finwise.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;

    private final FraudAlertRepository fraudAlertRepository;

    public FraudDetectionService(
            TransactionRepository transactionRepository,
            FraudAlertRepository fraudAlertRepository) {
        this.transactionRepository = transactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public void analyseTransaction(Transaction transaction) {

        System.out.println("FRAUD CHECK RUNNING for: "
                + transaction.getTitle()
                + " amount: " + transaction.getAmount()
                + " type: " + transaction.getType());

        checkLargeAmount(transaction);
        checkLateNight(transaction);
        checkDuplicate(transaction);
        checkHighFrequency(transaction);
    }

    private void checkLargeAmount(Transaction transaction) {

        if (transaction.getType().equals("EXPENSE") && transaction.getAmount() > 100000) {
            createAlert(
                    transaction,
                    "Large expense detected: ₹" + transaction.getAmount().longValue()
                            + " — please verify this transaction",
                    "HIGH"
            );
        }
    }

    private void checkLateNight(Transaction transaction) {
        int hour = transaction.getDate().getHour();

        if (hour >= 0 && hour < 4) {

            createAlert(
                    transaction,
                    "Late night transaction at "
                            + transaction.getDate().getHour() + ":"
                            + String.format("%02d", transaction.getDate().getMinute())
                            + " — unusual activity hours",
                    "MEDIUM"
            );
        }
    }

    private void checkDuplicate(Transaction transaction) {
        LocalDateTime oneDayAgo = transaction.getDate().minusHours(24);

        List<Transaction> recentTransaction =
                transactionRepository.findTop10ByUserOrderByDateDesc(transaction.getUser());

        boolean isDuplicate = recentTransaction.stream()
                .filter(t ->
                        !t.getId().equals(transaction.getId())
                        && t.getTitle().equalsIgnoreCase(transaction.getTitle())
                        && t.getAmount().equals(transaction.getAmount())
                        && t.getDate().isAfter(oneDayAgo)
                )
                .findAny()
                .isPresent();

        if (isDuplicate) {
            createAlert(
                    transaction,
                    "Duplicate transaction detected: \""
                            + transaction.getTitle()
                            + "\" for ₹" + transaction.getAmount().longValue()
                            + " — same transaction within 24 hours",
                    "MEDIUM"
            );
        }
    }

    private void checkHighFrequency(Transaction transaction) {
        LocalDateTime oneHourAgo = transaction.getDate().minusHours(1);

        List<Transaction> recentTransactions =
                transactionRepository.findTop10ByUserOrderByDateDesc(transaction.getUser());

        long countInLastHour = recentTransactions.stream()
                .filter(t ->
                        !t.getId().equals(transaction.getId())
                && t.getDate().isAfter(oneHourAgo)
                )
                .count();

        if (countInLastHour >= 4) {
            createAlert(
                    transaction,
                    "High frequency alert: "
                            + (countInLastHour + 1)
                            + " transactions in the last hour — unusual activity",
                    // HIGH severity — this is a serious red flag
                    "HIGH"
            );
        }
    }

    private void createAlert(Transaction transaction, String reason, String severity) {
        FraudAlert alert = new FraudAlert();

        alert.setReason(reason);
        alert.setSeverity(severity);
        alert.setDismissed(false);
        alert.setCreatedAt(LocalDateTime.now());
        alert.setTransaction(transaction);

        alert.setUser(transaction.getUser());

        fraudAlertRepository.save(alert);
    }
}
