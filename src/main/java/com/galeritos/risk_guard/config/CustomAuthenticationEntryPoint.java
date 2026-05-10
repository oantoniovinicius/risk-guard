package com.galeritos.risk_guard.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galeritos.risk_guard.shared.infrastructure.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String message;
        if (exception instanceof DisabledException) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            message = "Account is blocked.";
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            message = "Authentication is required to access this resource.";
        }

        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message));
    }
}
