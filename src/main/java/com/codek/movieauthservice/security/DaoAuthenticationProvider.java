package com.codek.movieauthservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Custom authentication provider for username/password login.
 *
 * Check order (fail-fast, clearest error message first):
 *  1. Load user from DB
 *  2. Account locked by admin (active = false)
 *  3. Email not yet verified
 *  4. OAuth2-only account trying to use password login
 *  5. Wrong password
 */
@Component
@RequiredArgsConstructor
public class DaoAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        CustomUserDetailsService.CustomUserDetails details =
                (CustomUserDetailsService.CustomUserDetails) userDetails;

        // 1. Admin lock
        if (!details.isAccountNonLocked()) {
            throw new BadCredentialsException("Your account has been locked. Please contact support.");
        }

        // 2. Email not verified (emailVerified == false; null = backward-compat = allowed)
        if (Boolean.FALSE.equals(details.getUser().getEmailVerified())) {
            throw new BadCredentialsException(
                    "Please verify your email before logging in. Check your inbox.");
        }

        // 3. OAuth2-only account (no password set)
        if (details.getPassword() == null) {
            throw new BadCredentialsException(
                    "This account uses Google Sign-In. Please click 'Login with Google'.");
        }

        // 4. Wrong password
        if (!passwordEncoder.matches(password, details.getPassword())) {
            throw new BadCredentialsException("Incorrect username or password.");
        }

        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
