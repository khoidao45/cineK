package com.codek.movieauthservice.controller;

import com.codek.movieauthservice.dto.ChangePasswordRequest;
import com.codek.movieauthservice.dto.UpdateProfileRequest;
import com.codek.movieauthservice.dto.UserResponse;
import com.codek.movieauthservice.security.CustomUserDetailsService;
import com.codek.movieauthservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        // User object is fresh — loaded during JWT filter for this request
        return ResponseEntity.ok(userService.convertToUserResponse(userDetails.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.convertToUserResponse(userService.getUserById(id)));
    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<Boolean> checkUsernameExists(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username).isPresent());
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        return ResponseEntity.ok(userService.findByEmail(email).isPresent());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                userService.convertToUserResponse(
                        userService.updateProfile(userDetails.getUserId(), request)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Mật khẩu xác nhận không khớp");
        }
        userService.changePassword(userDetails.getUserId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @PostMapping("/deactivate")
    public ResponseEntity<String> deactivateAccount(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        userService.deactivateAccount(userDetails.getUserId());
        return ResponseEntity.ok("Tai khoan da bi khoa!");
    }

    @PostMapping("/activate/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> activateAccount(@PathVariable Long userId) {
        userService.activateAccount(userId);
        return ResponseEntity.ok("Tai khoan da kich hoat!");
    }
}
