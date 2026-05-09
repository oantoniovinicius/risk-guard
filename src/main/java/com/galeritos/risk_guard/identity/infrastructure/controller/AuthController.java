package com.galeritos.risk_guard.identity.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.identity.application.usecase.LoginUseCase;
import com.galeritos.risk_guard.identity.application.usecase.RegisterUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginCommand;
import com.galeritos.risk_guard.identity.application.usecase.dto.LoginResult;
import com.galeritos.risk_guard.identity.application.usecase.dto.RegisterUserCommand;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.LoginRequest;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.LoginResponse;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.RegisterRequest;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.RegisterResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Auth", description = "User registration and login")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered, status PENDING"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Email or document already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUserUseCase.execute(new RegisterUserCommand(
                request.name(),
                request.email(),
                request.document(),
                request.password()));

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login and obtain JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
        LoginResponse response = new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.userId(),
                result.email(),
                result.role(),
                result.status());
        return ResponseEntity.ok(response);
    }
}
