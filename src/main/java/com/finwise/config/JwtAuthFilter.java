package com.finwise.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;

// @Component tells Spring to manage this class and auto-detect it
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // JwtService contains our token reading and validation logic
    private final JwtService jwtService;

    // Spring injects JwtService here via constructor
    @Autowired
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Runs automatically for every single HTTP request coming into the app
    // No @NonNull annotations — they are not needed for the filter to work
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Read the Authorization header
        // React sends: Authorization: Bearer eyJhbGci...
        final String authHeader = request.getHeader("Authorization");

        // If no token present, this is a public request (login/register)
        // Pass it through without any authentication check
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Remove "Bearer " prefix to get just the token string
        final String token = authHeader.substring(7);

        try {
            // Extract the email stored inside the JWT token
            // Replace extractEmail with whatever your JwtService method is called
            // Open JwtService.java and check the exact method name
            String email = jwtService.extractEmail(token);

            // If email was found and user not already authenticated
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // Check token hasn't expired or been tampered with
                if (jwtService.isTokenValid(token)) {

                    // Create an authentication object for this user
                    // Parameters: principal (email), credentials (null), authorities (empty)
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    email, null, new ArrayList<>()
                            );

                    // Attach extra request info like IP address
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Store in SecurityContext — Spring now knows this user is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            // Token expired or invalid — don't authenticate
            // Public routes still work fine, protected routes return 401
            System.out.println("JWT filter skipping invalid token: " + e.getMessage());
        }

        // Always pass the request along to the next filter
        filterChain.doFilter(request, response);
    }
}