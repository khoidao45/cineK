package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.AuthRequest;
import com.codek.movieauthservice.dto.AuthResponse;
import com.codek.movieauthservice.dto.RegisterRequest;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.exception.AuthException;
import com.codek.movieauthservice.security.CustomUserDetailsService;
import com.codek.movieauthservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Creates the account, sends a verification email, and returns a message
     * (no tokens — the user must verify their email before they can log in).
     */
    public AuthResponse register(RegisterRequest request) {
        User newUser = userService.registerUser(request);

        // Fire-and-forget: @Async in EmailService so this never blocks the response
        emailService.sendVerificationEmail(
                newUser.getEmail(),
                newUser.getUsername(),
                newUser.getVerificationToken()
        );

        return AuthResponse.builder()
                .message("Registration successful! Please check your email to verify your account.")
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword())
            );

            // principal is CustomUserDetails after our DaoAuthenticationProvider fix
            CustomUserDetailsService.CustomUserDetails userDetails =
                    (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();

            User user = userDetails.getUser();

            String accessToken  = jwtTokenProvider.generateAccessToken(
                    user.getUsername(), user.getRole().toString(), user.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getUsername(), user.getId());

            return AuthResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .type("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole().toString())
                    .message("Login successful!")
                    .build();

        } catch (BadCredentialsException e) {
            // Propagate DaoAuthenticationProvider's specific message (locked / unverified / wrong password)
            throw new AuthException(e.getMessage());
        } catch (AuthenticationException e) {
            throw new AuthException("Authentication failed: " + e.getMessage());
        }
    }

    // ── Token operations ──────────────────────────────────────────────────────

    public User validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthException("Invalid token!");
        }
        if (!jwtTokenProvider.isAccessToken(token)) {
            throw new AuthException("This is not an access token!");
        }
        return userService.getUserByUsername(jwtTokenProvider.getUsernameFromToken(token));
    }

    public Long getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid refresh token!");
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AuthException("This is not a refresh token!");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userService.getUserByUsername(username);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUsername(), user.getRole().toString(), user.getId());

        return AuthResponse.builder()
                .token(newAccessToken)
                .type("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().toString())
                .message("Token refreshed successfully!")
                .build();
    }
}
