package com.amelosbc.taskmanager.infrastructure.security;

import com.amelosbc.taskmanager.application.UserService;
import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Profile({"dev", "test", "default"})
public class DevMockAuthFilter extends OncePerRequestFilter {

    public static final String MOCK_USER_HEADER = "X-Mock-User";
    private final UserService userService;

    public DevMockAuthFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String mockUserId = request.getHeader(MOCK_USER_HEADER);

        if (mockUserId != null && !mockUserId.trim().isEmpty()) {
            String cleanUserId = mockUserId.trim();
            String mockEmail = cleanUserId + "@taskmanager.local";
            String mockName = "Mock User (" + cleanUserId + ")";

            CurrentUserContext userContext = userService.syncAndGetUser(cleanUserId, mockEmail, mockName);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            var auth = new UsernamePasswordAuthenticationToken(userContext, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
