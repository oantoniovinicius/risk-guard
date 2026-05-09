package com.galeritos.risk_guard.identity.infrastructure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.application.usecase.CreatePinUseCase;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.CreatePinRequest;

@Tag(name = "User", description = "Authenticated user operations")
@RestController
public class UserController {
    private final CreatePinUseCase createPinUseCase;
    private final CurrentUserProvider currentUserProvider;

    public UserController(CreatePinUseCase createPinUseCase, CurrentUserProvider currentUserProvider) {
        this.createPinUseCase = createPinUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "Define PIN for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "PIN defined"),
        @ApiResponse(responseCode = "400", description = "Weak PIN or PIN already defined"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/user/pin")
    public ResponseEntity<Void> createPin(@Valid @RequestBody CreatePinRequest request) {
        AuthenticatedUser user = currentUserProvider.getAuthenticatedUser();
        createPinUseCase.execute(user.userId(), request.pin());
        return ResponseEntity.noContent().build();
    }
}
