package com.finwise.service;

import com.finwise.dto.ChatRequest;
import com.finwise.dto.ChatResponse;
import com.finwise.model.Transaction;
import com.finwise.model.User;
import com.finwise.repository.TransactionRepository;
import com.finwise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @Service registers this as a Spring-managed Bean
@Service

// Lombok generates constructor — Spring injects all dependencies
public class ChatService {

    // Used to fetch the user's transactions to include as AI context
    private final TransactionRepository transactionRepository;

    // Used to find the User object by email from JWT
    private final UserRepository userRepository;

    // WebClient is Spring's modern HTTP client for making API calls
    // We use it to call the Gemini REST API
    // We build it fresh here — no need to inject it
    private WebClient webClient = WebClient.builder().build();

    // Read the Gemini API key from application.properties
    // @Value injects the value of gemini.api.key into this field
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // Read the Gemini API URL from application.properties
    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public ChatService(TransactionRepository transactionRepository,
                       UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ─── MAIN METHOD: HANDLE CHAT MESSAGE ────────────────────────
    // Called when user sends a message in the chat widget
    // email = logged-in user's email from JWT token
    // request = contains the user's message + conversation history
    @SuppressWarnings("unchecked")
    public ChatResponse chat(ChatRequest request, String email) {

        // Find the user by email from JWT
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch all transactions for context
        List<Transaction> transactions =
                transactionRepository.findByUserOrderByDateDesc(user);

        // Build system prompt with transaction data
        String systemPrompt = buildSystemPrompt(user, transactions);

        // Build Gemini request body
        Map<String, Object> requestBody = buildGeminiRequest(systemPrompt, request);

        try {
            // Call Gemini API once
            Map<String, Object> geminiResponse = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Extract and return the reply
            String reply = extractReply(geminiResponse);
            return new ChatResponse(reply);

        } catch (Exception e) {

            // Log the actual error in IntelliJ console for debugging
            System.out.println("Gemini API error: " + e.getMessage());

            // Check if it's a rate limit error (429)
            // Return a friendly message instead of crashing
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                return new ChatResponse(
                        "I'm receiving too many requests right now. " +
                                "Please wait a moment and try again!"
                );
            }

            // Generic fallback for any other error
            return new ChatResponse(
                    "I'm having trouble connecting right now. Please try again in a moment."
            );
        }
    }

    // ─── BUILD SYSTEM PROMPT ──────────────────────────────────────
    // Creates the instruction prompt that tells Gemini who it is
    // and provides the user's complete financial data as context
    private String buildSystemPrompt(User user, List<Transaction> transactions) {

        // Start building the prompt string
        StringBuilder prompt = new StringBuilder();

        // Tell Gemini its role and personality
        prompt.append("You are FinWise AI, a friendly and knowledgeable personal finance advisor. ");
        prompt.append("You are helping ").append(user.getName()).append(" manage their finances. ");
        prompt.append("Always give specific, actionable advice based on their actual transaction data. ");
        prompt.append("Keep responses concise and friendly. Use Indian Rupee (₹) for amounts. ");
        prompt.append("If asked about spending, always calculate from the transaction data provided.\n\n");

        // Include the user's transaction data
        prompt.append("Here is ").append(user.getName()).append("'s complete transaction history:\n");

        // If no transactions yet, tell Gemini so it doesn't make up data
        if (transactions.isEmpty()) {
            prompt.append("No transactions recorded yet.\n");
        } else {
            // Format each transaction as a readable line
            // e.g. "- [EXPENSE] Groceries: ₹2500 (Food) on 2024-03-15"
            for (Transaction t : transactions) {
                prompt.append("- [")
                        .append(t.getType())          // INCOME or EXPENSE
                        .append("] ")
                        .append(t.getTitle())          // e.g. "Groceries"
                        .append(": ₹")
                        .append(t.getAmount().longValue()) // e.g. 2500
                        .append(" (")
                        .append(t.getCategory())       // e.g. "Food"
                        .append(") on ")
                        .append(t.getDate().toLocalDate()) // e.g. 2024-03-15
                        .append("\n");
            }
        }

        prompt.append("\nNow answer the user's question based on this data.");

        return prompt.toString();
    }

    // ─── BUILD GEMINI API REQUEST BODY ────────────────────────────
    // Constructs the JSON structure Gemini expects
    // Gemini uses a "contents" array where each item has a "role" and "parts"
    // We include the system prompt + conversation history + current message
    private Map<String, Object> buildGeminiRequest(
            String systemPrompt, ChatRequest request) {

        // The "contents" array — holds all messages in the conversation
        List<Map<String, Object>> contents = new ArrayList<>();

        // First message: our system prompt sent as a "user" role message
        // Gemini doesn't have a dedicated system role like OpenAI
        // so we send the system prompt as the first user message
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "user");
        systemMessage.put("parts", List.of(Map.of("text", systemPrompt)));
        contents.add(systemMessage);

        // Add a "model" response to acknowledge the system prompt
        // This is a common pattern with Gemini to properly establish context
        Map<String, Object> systemAck = new HashMap<>();
        systemAck.put("role", "model");
        systemAck.put("parts", List.of(Map.of("text",
                "Understood! I'm FinWise AI, ready to help with financial advice.")));
        contents.add(systemAck);

        // Add the conversation history (previous messages)
        // This is what makes the chatbot remember previous turns
        if (request.getHistory() != null) {
            for (ChatRequest.ChatMessage historyMsg : request.getHistory()) {

                Map<String, Object> historyEntry = new HashMap<>();

                // Gemini uses "user" and "model" roles
                // Our frontend uses "user" and "assistant" — convert "assistant" to "model"
                String geminiRole = historyMsg.getRole().equals("assistant")
                        ? "model"   // Gemini's name for AI messages
                        : "user";   // stays "user"

                historyEntry.put("role", geminiRole);
                historyEntry.put("parts", List.of(Map.of("text", historyMsg.getContent())));
                contents.add(historyEntry);
            }
        }

        // Add the current user message (what they just typed)
        Map<String, Object> currentMessage = new HashMap<>();
        currentMessage.put("role", "user");
        currentMessage.put("parts", List.of(Map.of("text", request.getMessage())));
        contents.add(currentMessage);

        // Build the final request body Map
        // Spring's WebClient will convert this Map to JSON automatically
        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);

        // Generation config — controls how Gemini generates its response
        Map<String, Object> generationConfig = new HashMap<>();
        // Max tokens = maximum length of the AI's response
        // 1024 tokens ≈ about 750 words — enough for detailed financial advice
        generationConfig.put("maxOutputTokens", 1024);
        // Temperature controls creativity vs accuracy
        // 0.7 = balanced — not too creative, not too rigid
        // 0.0 = always the same answer, 1.0 = very creative/random
        generationConfig.put("temperature", 0.7);
        body.put("generationConfig", generationConfig);

        return body;
    }

    // ─── EXTRACT REPLY FROM GEMINI RESPONSE ──────────────────────
    // Gemini returns a deeply nested JSON structure
    // We navigate through it to extract just the text of the AI's reply
    // Structure: candidates[0].content.parts[0].text
    @SuppressWarnings("unchecked")
    private String extractReply(Map<String, Object> response) {
        try {
            // Get the "candidates" array from the top-level response
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");

            // Get the first candidate (Gemini always returns at least one)
            Map<String, Object> firstCandidate = candidates.get(0);

            // Get the "content" object inside the first candidate
            Map<String, Object> content =
                    (Map<String, Object>) firstCandidate.get("content");

            // Get the "parts" array inside content
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

            // Get the "text" from the first part — this is the AI's reply
            return parts.get(0).get("text").toString();

        } catch (Exception e) {
            // If anything goes wrong parsing the response, return a fallback message
            return "Sorry, I couldn't process your request. Please try again.";
        }
    }
}