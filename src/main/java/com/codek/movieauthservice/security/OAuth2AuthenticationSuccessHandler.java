package com.codek.movieauthservice.security;

import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.service.UserService;
import com.codek.movieauthservice.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Called by Spring Security after a successful Google OAuth2 login.
 *
 * Generates a JWT pair (access + refresh) and redirects the user to the
 * configured frontend callback URL with the tokens as query parameters:
 *
 *   http://localhost:3000/oauth2/callback?token=xxx&refreshToken=yyy
 *
 * The frontend stores the tokens in localStorage / memory and uses them
 * for subsequent API calls exactly like a normal login response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userService.getUserByEmail(email);

        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getUsername(), user.getRole().toString(), user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getUsername(), user.getId());

        // Clean up the OAuth2 state cookie before redirecting
        cookieRepository.removeAuthorizationRequest(request, response);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        log.info("OAuth2 login success for {}, redirecting to frontend", email);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
