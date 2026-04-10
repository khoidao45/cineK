package com.codek.movieauthservice.security;

import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CustomUserDetails(user);
    }

    public static class CustomUserDetails implements UserDetails {

        private final User user;

        public CustomUserDetails(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toString()));
        }

        @Override
        public String getPassword() {
            return user.getPassword(); // null for OAuth2 users — handled in DaoAuthenticationProvider
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() { return true; }

        @Override
        public boolean isCredentialsNonExpired() { return true; }

        /**
         * Returns false when the admin has explicitly locked the account (active = false).
         * Email-not-verified is a separate check in DaoAuthenticationProvider, not here,
         * so that JwtAuthenticationFilter still works for token-based requests from
         * already-verified users even if this method returns true.
         */
        @Override
        public boolean isAccountNonLocked() {
            return Boolean.TRUE.equals(user.getActive());
        }

        /**
         * Combines the admin-lock flag AND email-verification status.
         * null emailVerified is treated as verified for backward compatibility
         * (rows created before the column was added).
         */
        @Override
        public boolean isEnabled() {
            return Boolean.TRUE.equals(user.getActive())
                    && !Boolean.FALSE.equals(user.getEmailVerified());
        }

        public User getUser() { return user; }

        public Long getUserId() { return user.getId(); }
    }
}
