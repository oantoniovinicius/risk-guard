package com.galeritos.risk_guard.identity.infrastructure.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.galeritos.risk_guard.banking.application.usecase.ListUserTransfersUseCase;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.UserTransferPageResponse;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.UserTransferResponse;
import com.galeritos.risk_guard.banking.infrastructure.persistence.repository.AccountRepository;
import com.galeritos.risk_guard.identity.application.security.AuthenticatedUser;
import com.galeritos.risk_guard.identity.application.security.CurrentUserProvider;
import com.galeritos.risk_guard.identity.application.usecase.CreatePinUseCase;
import com.galeritos.risk_guard.identity.application.usecase.GetUserDetailUseCase;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.CreatePinRequest;
import com.galeritos.risk_guard.identity.infrastructure.controller.dto.UserProfileResponse;

@Tag(name = "User", description = "Authenticated user operations")
@RestController
public class UserController {
    private final CreatePinUseCase createPinUseCase;
    private final CurrentUserProvider currentUserProvider;
    private final GetUserDetailUseCase getUserDetailUseCase;
    private final AccountRepository accountRepository;
    private final ListUserTransfersUseCase listUserTransfersUseCase;

    public UserController(CreatePinUseCase createPinUseCase, CurrentUserProvider currentUserProvider,
            GetUserDetailUseCase getUserDetailUseCase, AccountRepository accountRepository,
            ListUserTransfersUseCase listUserTransfersUseCase) {
        this.createPinUseCase = createPinUseCase;
        this.currentUserProvider = currentUserProvider;
        this.getUserDetailUseCase = getUserDetailUseCase;
        this.accountRepository = accountRepository;
        this.listUserTransfersUseCase = listUserTransfersUseCase;
    }

    @Operation(summary = "Get authenticated user profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/user/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        AuthenticatedUser authenticated = currentUserProvider.getAuthenticatedUser();
        User user = getUserDetailUseCase.execute(authenticated.userId());
        java.util.UUID accountId = accountRepository.findByUserId(user.getId())
                .map(account -> account.getId())
                .orElse(null);

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDocument(),
                user.getRole(),
                user.getStatus(),
                accountId,
                user.getCreatedAt()));
    }

    @Operation(summary = "List authenticated user's transfers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfers returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/user/transfers")
    public ResponseEntity<UserTransferPageResponse> listTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuthenticatedUser authenticated = currentUserProvider.getAuthenticatedUser();
        Page<Transaction> result = listUserTransfersUseCase.execute(authenticated.userId(), page, size);
        return ResponseEntity.ok(new UserTransferPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(t -> new UserTransferResponse(
                        t.getId(),
                        t.getSenderId(),
                        t.getReceiverId(),
                        t.getAmount(),
                        t.getStatus().name(),
                        t.getRiskLevel() != null ? t.getRiskLevel().name() : null,
                        t.getCreatedAt())).toList()));
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
