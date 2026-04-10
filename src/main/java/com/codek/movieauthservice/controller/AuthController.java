package com.codek.movieauthservice.controller;

import com.codek.movieauthservice.dto.AuthRequest;
import com.codek.movieauthservice.dto.AuthResponse;
import com.codek.movieauthservice.dto.RegisterRequest;
import com.codek.movieauthservice.security.TokenBlacklistService;
import com.codek.movieauthservice.service.AuthService;
import com.codek.movieauthservice.service.EmailService;
import com.codek.movieauthservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    // ── Register / Login ──────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Returns a message only — no tokens until email is verified
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Email verification ────────────────────────────────────────────────────

    /**
     * User clicks the link in their inbox:
     * GET /api/auth/verify-email?token=uuid
     *
     * On success → 200 OK with a confirmation message.
     * Redirect to frontend login page is optional (see commented line).
     */
    @GetMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam String token) {
        userService.verifyEmail(token);
        // Optional: response.sendRedirect("http://localhost:3000/login?verified=true");
        return ResponseEntity.ok(AuthResponse.builder()
                .message("Email verified successfully! You can now log in.")
                .build());
    }

    /**
     * POST /api/auth/resend-verification
     * Body: { "email": "user@example.com" }
     *
     * Generates a new token and resends the verification email.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(@RequestParam String email) {
        String newToken = userService.regenerateVerificationToken(email);
        String username = userService.getUserByEmail(email).getUsername();
        emailService.sendVerificationEmail(email, username, newToken);
        return ResponseEntity.ok(AuthResponse.builder()
                .message("Verification email resent. Please check your inbox.")
                .build());
    }

    // ── Token operations ──────────────────────────────────────────────────────

    @PostMapping("/validate-token")
    public ResponseEntity<AuthResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.validateToken(token);
        return ResponseEntity.ok(AuthResponse.builder().message("Token is valid.").build());
    }

    @GetMapping("/validate")
    public ResponseEntity<Long> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(authService.getUserIdFromToken(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        String refreshToken = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        tokenBlacklistService.blacklist(token);
        return ResponseEntity.ok("Logged out successfully!");
    }
}
