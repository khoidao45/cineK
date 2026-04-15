package com.codek.movieauthservice.security;

import com.codek.movieauthservice.entity.Role;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Processes the Google user-info payload after the OAuth2 token exchange.
 *
 * On each successful Google login:
 *  - If the email already exists in the DB → update name/avatar from Google and reuse the account.
 *  - If not → create a new User (provider=GOOGLE, emailVerified=true, no password).
 *
 * Returns the standard DefaultOAuth2User so Spring Security can continue the flow.
 * OAuth2AuthenticationSuccessHandler then generates the JWT from the email claim.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email   = oAuth2User.getAttribute("email");
        String name    = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> syncFromGoogle(existing, name, picture),
                ()       -> createGoogleUser(email, name, picture)
        );

        return oAuth2User;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void syncFromGoogle(User user, String name, String picture) {
        boolean dirty = false;
        if (name != null && !name.equals(user.getFullName())) {
            user.setFullName(name);
            dirty = true;
        }
        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            dirty = true;
        }
        // Same email as Google → treat as verified (fixes LOCAL+unverified then "Login with Google")
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            dirty = true;
            log.info("Marked email verified via Google OAuth for: {}", user.getEmail());
        }
        if (dirty) {
            userRepository.save(user);
            log.debug("Synced Google profile for: {}", user.getEmail());
        }
    }

    private void createGoogleUser(String email, String name, String picture) {
        String username = uniqueUsernameFrom(email);
        User user = User.builder()
                .username(username)
                .email(email)
                .password(null)               // no password for OAuth2 users
                .fullName(name != null ? name : username)
                .avatarUrl(picture != null ? picture : "")
                .role(Role.USER)
                .active(true)
                .emailVerified(true)          // Google already verified the email
                .verificationToken(null)
                .provider("GOOGLE")
                .build();
        userRepository.save(user);
        log.info("Created new Google user: {}", email);
    }

    private String uniqueUsernameFrom(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }
}
