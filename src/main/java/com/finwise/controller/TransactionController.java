package com.finwise.controller;

import com.finwise.dto.TransactionRequest;
import com.finwise.dto.TransactionResponse;
import com.finwise.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> addTransaction(
            @RequestBody TransactionRequest request) {

        String email = getCurrentUserEmail();
        TransactionResponse response = transactionService.addTransaction(request, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        String email = getCurrentUserEmail();

        List<TransactionResponse> transactions = transactionService.getAllTransactions(email);
        return ResponseEntity.ok(transactions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTransaction(
            @PathVariable Long id) {
        String email = getCurrentUserEmail();

        transactionService.deleteTransaction(id, email);
        return ResponseEntity.ok("Transaction deleted successfully.");
    }


}
