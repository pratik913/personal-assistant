package com.personalassistant.security;

import com.personalassistant.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        System.out.println(
                "[JWT FILTER] "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
                        + " | Authorization: "
                        + (
                        authorizationHeader != null
                                ? "PRESENT"
                                : "MISSING"
                )
        );

        if (
                authorizationHeader == null
                        || !authorizationHeader
                        .startsWith("Bearer ")
        ) {

            System.out.println(
                    "[JWT FILTER] No Bearer token"
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authorizationHeader.substring(7);

        boolean valid =
                jwtService.isTokenValid(token);

        System.out.println(
                "[JWT FILTER] Token valid: "
                        + valid
        );

        if (!valid) {

            System.out.println(
                    "[JWT FILTER] REJECTING REQUEST - INVALID TOKEN"
            );

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json"
            );

            response.getWriter().write("""
            {
                "status": 401,
                "message": "Invalid or expired token"
            }
            """);

            return;
        }

        String userId =
                jwtService.extractUserId(token);

        System.out.println(
                "[JWT FILTER] Authenticated user: "
                        + userId
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        System.out.println(
                "[JWT FILTER] SecurityContext authentication set"
        );

        filterChain.doFilter(
                request,
                response
        );
    }
}