package com.finwise.controller;

import com.finwise.dto.PdfUploadResponse;
import com.finwise.service.PdfUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// @RestController = handles HTTP requests and returns JSON
@RestController

// All endpoints start with /api/pdf
@RequestMapping("/api/pdf")

// Allow React on port 5173 to call these endpoints
@CrossOrigin(origins = "http://localhost:5173")

// Lombok generates constructor for PdfUploadService injection
public class PdfUploadController {

    // Spring injects PdfUploadService here
    private final PdfUploadService pdfUploadService;

    @Autowired
    public PdfUploadController (PdfUploadService pdfUploadService) {
        this.pdfUploadService = pdfUploadService;
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

    // ─── PDF UPLOAD ENDPOINT ──────────────────────────────────────
    // POST http://localhost:8080/api/pdf/upload
    // React sends the PDF as multipart/form-data
    // @RequestParam("file") tells Spring to look for a form field named "file"
    // MultipartFile is Spring's representation of an uploaded file
    @PostMapping("/upload")
    public ResponseEntity<PdfUploadResponse> uploadPdf(
            @RequestParam("file") MultipartFile file) {

        try {
            // Get the logged-in user's email from JWT
            String email = getCurrentUserEmail();

            // Process the PDF — parse and save all transactions
            PdfUploadResponse response = pdfUploadService.processPdf(file, email);

            // Return 200 OK with the upload summary
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Return 400 Bad Request with the error message
            // e.g. "Only PDF files are supported"
            return ResponseEntity.badRequest()
                    .body(new PdfUploadResponse(
                            0,
                            null,
                            "Error processing PDF: " + e.getMessage()
                    ));
        }
    }
}