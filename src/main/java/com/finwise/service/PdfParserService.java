package com.finwise.service;

import com.finwise.dto.TransactionRequest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// @Service registers this as a Spring-managed Bean
@Service

// Lombok generates constructor for dependency injection
public class PdfParserService {

    // ─── MAIN METHOD: PARSE PDF ───────────────────────────────────
    // Takes the uploaded PDF file and returns a list of TransactionRequests
    // that can be saved directly to MySQL
    // file = the PDF uploaded by the user from React
    public List<TransactionRequest> parseBankStatement(MultipartFile file)
            throws IOException {

        // Step 1: Load the PDF file into PDFBox
        // PDDocument is PDFBox's representation of a PDF file
        // We load it from the InputStream of the uploaded file
        PDDocument document = Loader.loadPDF(file.getBytes());

        // Step 2: Extract all text from the PDF
        // PDFTextStripper reads every page and converts it to plain text
        // Think of it like copying all text from a PDF into a String
        PDFTextStripper stripper = new PDFTextStripper();
        String rawText = stripper.getText(document);

        // Step 3: Close the document to free memory
        // Always close PDDocument after use — it holds file handles
        document.close();

        // Step 4: Print the raw extracted text to IntelliJ console
        // This helps us see what the PDF text looks like during development
        // We use this to refine our parsing patterns
        System.out.println("=== PDF RAW TEXT START ===");
        System.out.println(rawText);
        System.out.println("=== PDF RAW TEXT END ===");

        // Step 5: Parse the raw text into transaction objects
        return parseTransactions(rawText);
    }

    // ─── PARSE TRANSACTIONS FROM RAW TEXT ────────────────────────
    // Takes the raw text extracted from the PDF
    // and finds transaction patterns using regex
    private List<TransactionRequest> parseTransactions(String text) {

        // List to collect all found transactions
        List<TransactionRequest> transactions = new ArrayList<>();

        // Split the text into individual lines
        // Each line in a bank statement usually represents one transaction
        String[] lines = text.split("\n");

        // ─── REGEX PATTERNS ───────────────────────────────────────
        // Regex (Regular Expression) is a pattern matching tool
        // We use it to find dates and amounts in the text

        // DATE PATTERN: matches dates like "15/03/2024" or "15-03-2024"
        // \\d{2} = exactly 2 digits
        // [/\\-] = either / or - character
        // So this matches: 15/03/2024 or 15-03-2024
        Pattern datePattern = Pattern.compile("(\\d{2}[/\\-]\\d{2}[/\\-]\\d{4})");

        // AMOUNT PATTERN: matches amounts like "1,500.00" or "25000.50" or "500"
        // \\d{1,3} = 1 to 3 digits (before comma groups)
        // (,\\d{3})* = zero or more groups of comma + 3 digits (for thousands)
        // (\\.\\d{2})? = optional decimal part with exactly 2 decimal places
        Pattern amountPattern = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?)");

        // Process each line looking for transaction data
        for (String line : lines) {

            // Skip empty lines and very short lines
            // Lines with less than 10 characters can't contain transaction data
            if (line.trim().length() < 10) continue;

            // Skip common header/footer lines that aren't transactions
            // These are typical words found in bank statement headers
            if (line.contains("Statement") ||
                    line.contains("Account") ||
                    line.contains("Balance") ||
                    line.contains("Opening") ||
                    line.contains("Closing") ||
                    line.contains("Date") && line.contains("Description")) {
                continue;
            }

            // Try to find a date in this line
            Matcher dateMatcher = datePattern.matcher(line);

            // If the line contains a date — it's likely a transaction line
            if (dateMatcher.find()) {

                // Extract the date string found e.g. "15/03/2024"
                String dateStr = dateMatcher.group(1);

                // Try to find an amount in the same line
                Matcher amountMatcher = amountPattern.matcher(line);

                // Find all amounts in the line — bank statements often have
                // debit amount, credit amount, and balance on the same line
                List<String> amounts = new ArrayList<>();
                while (amountMatcher.find()) {
                    String amt = amountMatcher.group(1);
                    // Only include amounts that look like money (have decimal or are large)
                    // This filters out things like "15" from "15/03/2024"
                    if (amt.contains(".") || amt.length() > 3) {
                        amounts.add(amt);
                    }
                }

                // If we found at least one amount — create a transaction
                if (!amounts.isEmpty()) {

                    // Use the first amount found as the transaction amount
                    String amountStr = amounts.get(0)
                            .replace(",", ""); // remove commas: "1,500.00" → "1500.00"

                    try {
                        // Convert amount string to a double number
                        double amount = Double.parseDouble(amountStr);

                        // Skip amounts that are too small (likely not real transactions)
                        if (amount < 1) continue;

                        // Extract the description from the line
                        // Remove the date and amount to get the description
                        String description = line
                                .replaceAll("\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}", "")
                                .replaceAll("\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?", "")
                                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                                .trim()
                                .replaceAll("\\s+", " "); // remove extra spaces

                        // If description is empty after cleaning, use a default
                        if (description.isEmpty()) {
                            description = "Bank Transaction";
                        }

                        // Limit description to 100 characters
                        if (description.length() > 100) {
                            description = description.substring(0, 100);
                        }

                        // Parse the date string into LocalDateTime
                        LocalDateTime transactionDate = parseDate(dateStr);

                        // Determine if this is INCOME or EXPENSE
                        // We look for keywords in the line to classify
                        String type = classifyTransaction(line, description);

                        // Determine category based on description keywords
                        String category = categoriseTransaction(description);

                        // Create the TransactionRequest object
                        // This is what TransactionService.addTransaction() expects
                        TransactionRequest request = new TransactionRequest();
                        request.setTitle(description);
                        request.setAmount(amount);
                        request.setCategory(category);
                        request.setType(type);
                        request.setDate(transactionDate);

                        // Add to our list
                        transactions.add(request);

                    } catch (NumberFormatException e) {
                        // Amount couldn't be parsed — skip this line
                        System.out.println("Skipping line — couldn't parse amount: " + line);
                    }
                }
            }
        }

        System.out.println("Total transactions parsed: " + transactions.size());
        return transactions;
    }

    // ─── PARSE DATE STRING TO LocalDateTime ──────────────────────
    // Tries multiple date formats because different banks format
    // dates differently in their statements
    private LocalDateTime parseDate(String dateStr) {

        // List of date formats to try in order
        // DD/MM/YYYY is most common in Indian bank statements
        String[] formats = {
                "dd/MM/yyyy",  // 15/03/2024 — most Indian banks
                "dd-MM-yyyy",  // 15-03-2024
                "MM/dd/yyyy",  // 03/15/2024 — some international banks
                "yyyy-MM-dd",  // 2024-03-15 — ISO format
        };

        for (String format : formats) {
            try {
                // Try to parse the date with this format
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                // LocalDateTime.parse needs a time part — we append midnight
                return LocalDateTime.parse(dateStr + " 00:00",
                        DateTimeFormatter.ofPattern(format + " HH:mm"));
            } catch (DateTimeParseException e) {
                // This format didn't work — try the next one
            }
        }

        // If no format worked, use current date/time as fallback
        return LocalDateTime.now();
    }

    // ─── CLASSIFY TRANSACTION AS INCOME OR EXPENSE ───────────────
    // Looks for keywords in the transaction line to determine type
    // Indian bank statements use specific terms for credits and debits
    private String classifyTransaction(String line, String description) {

        // Convert to uppercase for case-insensitive matching
        String upperLine = line.toUpperCase();
        String upperDesc = description.toUpperCase();

        // Keywords that indicate INCOME (money coming in)
        if (upperLine.contains("CREDIT") ||
                upperLine.contains("CR") ||
                upperDesc.contains("SALARY") ||
                upperDesc.contains("REFUND") ||
                upperDesc.contains("CASHBACK") ||
                upperDesc.contains("INTEREST") ||
                upperDesc.contains("DIVIDEND") ||
                upperDesc.contains("TRANSFER IN")) {
            return "INCOME";
        }

        // Everything else is treated as EXPENSE (money going out)
        // DR = Debit in Indian bank statements
        return "EXPENSE";
    }

    // ─── CATEGORISE TRANSACTION ───────────────────────────────────
    // Assigns a category to the transaction based on keywords
    // in the description — similar to how banking apps auto-categorise
    private String categoriseTransaction(String description) {

        // Convert to uppercase for case-insensitive matching
        String upper = description.toUpperCase();

        // Check each category's keywords
        if (upper.contains("SALARY") || upper.contains("PAYROLL")) {
            return "Salary";
        }
        if (upper.contains("ZOMATO") || upper.contains("SWIGGY") ||
                upper.contains("RESTAURANT") || upper.contains("FOOD") ||
                upper.contains("CAFE") || upper.contains("HOTEL")) {
            return "Food";
        }
        if (upper.contains("UBER") || upper.contains("OLA") ||
                upper.contains("METRO") || upper.contains("PETROL") ||
                upper.contains("FUEL") || upper.contains("TRANSPORT")) {
            return "Transport";
        }
        if (upper.contains("AMAZON") || upper.contains("FLIPKART") ||
                upper.contains("MYNTRA") || upper.contains("SHOPPING") ||
                upper.contains("MALL")) {
            return "Shopping";
        }
        if (upper.contains("NETFLIX") || upper.contains("HOTSTAR") ||
                upper.contains("SPOTIFY") || upper.contains("PRIME") ||
                upper.contains("ENTERTAINMENT")) {
            return "Entertainment";
        }
        if (upper.contains("HOSPITAL") || upper.contains("PHARMACY") ||
                upper.contains("MEDICAL") || upper.contains("DOCTOR") ||
                upper.contains("HEALTH")) {
            return "Health";
        }
        if (upper.contains("COLLEGE") || upper.contains("SCHOOL") ||
                upper.contains("COURSE") || upper.contains("EDUCATION") ||
                upper.contains("FEES")) {
            return "Education";
        }
        if (upper.contains("ELECTRICITY") || upper.contains("WATER") ||
                upper.contains("GAS") || upper.contains("BILL") ||
                upper.contains("RECHARGE")) {
            return "Bills";
        }

        // Default category if no keyword matched
        return "Other";
    }
}