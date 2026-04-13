package com.finwise.dto;

import java.util.List;

// This DTO is what we send back to React after processing the PDF
// It tells the user how many transactions were found and imported
public class PdfUploadResponse {

    // Total number of transactions successfully extracted from the PDF
    // e.g. "15 transactions imported"
    private int transactionsImported;

    // The list of imported transactions — shown in the UI after upload
    // React uses this to immediately update the transaction list
    // without needing to make another GET request
    private List<TransactionResponse> transactions;

    // A human-readable message shown to the user
    // e.g. "Successfully imported 15 transactions from your bank statement"
    private String message;

    public PdfUploadResponse() {

    }

    public PdfUploadResponse(int transactionsImported, List<TransactionResponse> transactions, String message) {
        this.transactionsImported = transactionsImported;
        this.transactions = transactions;
        this.message = message;
    }

    public int getTransactionsImported() {
        return transactionsImported;
    }

    public void setTransactionsImported(int transactionsImported) {
        this.transactionsImported = transactionsImported;
    }

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionResponse> transactions) {
        this.transactions = transactions;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}