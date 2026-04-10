package com.codek.movieauthservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    // Nullable: OAuth2 users have no password
    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean active;

    // false until user clicks the verification link in their email.
    // null for rows created before this column was added (treated as verified, backward-compat).
    @Column(nullable = true)
    private Boolean emailVerified;

    // One-time token mailed to the user. Cleared after successful verification.
    @Column(nullable = true, unique = true)
    private String verificationToken;

    // "LOCAL" (username+password) or "GOOGLE" (OAuth2). Used to give the correct
    // error message when an OAuth2 user tries to log in with a password.
    @Column(nullable = true)
    @Builder.Default
    private String provider = "LOCAL";

    /** Constructor used by UserService for local registration. */
    public User(String username, String email, String password,
                String fullName, String avatarUrl, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.active = true;
        this.emailVerified = false;
        this.provider = "LOCAL";
    }
}
