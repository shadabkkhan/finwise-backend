package com.finwise.controller;

import com.finwise.dto.ChatRequest;
import com.finwise.dto.ChatResponse;
import com.finwise.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

// @RestController = handles HTTP requests and returns JSON
@RestController

// All endpoints in this controller start with /api/chat
@RequestMapping("/api/chat")

// Allow React on port 5173 to call this endpoint
@CrossOrigin(origins = "http://localhost:5173")

// Lombok generates constructor for ChatService injection
public class ChatController {

    // Spring injects ChatService here via the constructor Lombok generates
    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ─── HELPER: GET CURRENT USER EMAIL ──────────────────────────
    // Reads the logged-in user's email from the JWT token
    // JwtAuthFilter already validated the token and stored the email
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();
    }

    // ─── CHAT ENDPOINT ────────────────────────────────────────────
    // POST http://localhost:8080/api/chat
    // React sends: { message: "...", history: [...] }
    // Spring returns: { reply: "..." }
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {

        // Get the logged-in user's email from JWT
        String email = getCurrentUserEmail();

        // Pass to service — service fetches transactions, calls Gemini, returns reply
        ChatResponse response = chatService.chat(request, email);

        // Return 200 OK with the AI reply
        return ResponseEntity.ok(response);
    }
}