package com.finwise.service;

import com.finwise.dto.PdfUploadResponse;
import com.finwise.dto.TransactionRequest;
import com.finwise.dto.TransactionResponse;
import com.finwise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// @Service registers this as a Spring Bean
@Service

// Lombok generates constructor — injects all dependencies
public class PdfUploadService {

    // PdfParserService extracts transactions from the PDF text
    private final PdfParserService pdfParserService;

    // TransactionService saves each transaction to MySQL
    // and runs fraud detection on each one
    private final TransactionService transactionService;

    // Used to find the current user by email from JWT
    private final UserRepository userRepository;

    @Autowired
    public PdfUploadService(PdfParserService pdfParserService, TransactionService transactionService, UserRepository userRepository) {
        this.pdfParserService = pdfParserService;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    // ─── MAIN METHOD: PROCESS UPLOADED PDF ───────────────────────
    // Called when user uploads a PDF from the React dashboard
    // file = the PDF file uploaded by the user
    // email = the logged-in user's email from JWT token
    public PdfUploadResponse processPdf(MultipartFile file, String email)
            throws IOException {

        // Step 1: Validate the uploaded file
        // Check it's actually a PDF and not some other file type
        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        // Get the original filename — e.g. "statement.pdf"
        String filename = file.getOriginalFilename();

        // Check the file extension ends with .pdf (case insensitive)
        if (filename == null ||
                !filename.toLowerCase().endsWith(".pdf")) {
            throw new RuntimeException("Only PDF files are supported");
        }

        // Step 2: Parse the PDF and extract transaction data
        // PdfParserService reads the PDF text and returns TransactionRequest objects
        List<TransactionRequest> parsedTransactions =
                pdfParserService.parseBankStatement(file);

        // Step 3: Check if any transactions were found
        if (parsedTransactions.isEmpty()) {
            return new PdfUploadResponse(
                    0,
                    new ArrayList<>(),
                    "No transactions could be extracted from this PDF. " +
                            "Please make sure it's a valid bank statement."
            );
        }

        // Step 4: Save each parsed transaction to MySQL
        // We reuse the existing TransactionService.addTransaction() method
        // This means fraud detection runs automatically for each transaction!
        List<TransactionResponse> savedTransactions = new ArrayList<>();

        for (TransactionRequest request : parsedTransactions) {
            try {
                // addTransaction saves to MySQL and runs fraud detection
                TransactionResponse saved =
                        transactionService.addTransaction(request, email);
                savedTransactions.add(saved);
            } catch (Exception e) {
                // If one transaction fails to save, log it and continue
                // We don't want one bad transaction to stop all the others
                System.out.println("Failed to save transaction: "
                        + request.getTitle() + " — " + e.getMessage());
            }
        }

        // Step 5: Return the response with import summary
        return new PdfUploadResponse(
                savedTransactions.size(),
                savedTransactions,
                "Successfully imported " + savedTransactions.size()
                        + " transactions from your bank statement!"
        );
    }
}