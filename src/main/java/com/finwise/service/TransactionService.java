package com.finwise.service;

import com.finwise.dto.TransactionRequest;
import com.finwise.dto.TransactionResponse;
import com.finwise.model.Transaction;
import com.finwise.model.User;
import com.finwise.repository.TransactionRepository;
import com.finwise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final FraudDetectionService fraudDetectionService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              FraudDetectionService fraudDetectionService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    public TransactionResponse addTransaction(TransactionRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Transaction transaction = new Transaction();
        transaction.setTitle(request.getTitle());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setType(request.getType());
        transaction.setDate(request.getDate());

        transaction.setUser(user);

        Transaction saved = transactionRepository.save(transaction);
        fraudDetectionService.analyseTransaction(saved);

        return mapToResponse(saved);
    }

    public List<TransactionResponse> getAllTransactions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

//        List<Transaction> transactions =
//                transactionRepository.findTop10ByUserOrderByDateDesc(user);
        List<Transaction> transactions =
                transactionRepository.findByUserOrderByDateDesc(user);

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }



    public void deleteTransaction(Long id, String email) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found."));

        if (!transaction.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized request made.");
        }

        transactionRepository.delete(transaction);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getTitle(),
                t.getAmount(),
                t.getCategory(),
                t.getType(),
                t.getDate()
        );
    }
}
