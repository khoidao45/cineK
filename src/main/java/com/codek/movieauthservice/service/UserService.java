package com.codek.movieauthservice.service;

import com.codek.movieauthservice.dto.RegisterRequest;
import com.codek.movieauthservice.dto.UpdateProfileRequest;
import com.codek.movieauthservice.dto.UserResponse;
import com.codek.movieauthservice.entity.Role;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.exception.UserException;
import com.codek.movieauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Creates a new LOCAL user. Sets emailVerified=false; caller must send the
     * verification email using the returned token (user.getVerificationToken()).
     */
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserException("Username already exists!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException("Email already exists!");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : "")
                .role(Role.USER)
                .active(true)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .provider("LOCAL")
                .build();

        return userRepository.save(user);
    }

    // ── Email verification ────────────────────────────────────────────────────

    /**
     * Marks the user as verified and clears the one-time token.
     * Throws UserException if the token is invalid or already used.
     */
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UserException("Invalid or already-used verification link."));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    /**
     * Generates a new verification token and returns it so the caller can
     * resend the verification email. Only works for LOCAL, not-yet-verified accounts.
     */
    public String regenerateVerificationToken(String email) {
        User user = getUserByEmail(email);

        if (user.getProvider() != null && !"LOCAL".equals(user.getProvider())) {
            throw new UserException("This account uses Google Sign-In and does not need email verification.");
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UserException("Email is already verified.");
        }

        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        userRepository.save(user);
        return newToken;
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found: " + username));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException("User not found with email: " + email));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with ID: " + id));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    public UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().toString())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .provider(user.getProvider())
                .build();
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        return userRepository.save(user);
    }

    public void validatePassword(User user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new UserException("Incorrect password.");
        }
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        validatePassword(user, oldPassword);
        if (newPassword.length() < 6) {
            throw new UserException("New password must be at least 6 characters.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deactivateAccount(Long userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    public void activateAccount(Long userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }
}
