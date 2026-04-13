package com.finwise.repository;

import com.finwise.model.FraudAlert;
import com.finwise.model.Transaction;
import com.finwise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByUserAndDismissedFalseOrderByCreatedAtDesc(User user);
    List<FraudAlert> findByTransaction(Transaction transaction);
    long countByUserAndDismissedFalse(User user);
}
