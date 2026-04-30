package com.galeritos.risk_guard.admin.infrastructure.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserDetailResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserSummaryResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserStatusResponse;
import com.galeritos.risk_guard.identity.application.usecase.ApproveUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.DenyUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.GetUserDetailUseCase;
import com.galeritos.risk_guard.identity.application.usecase.ListPendingUsersUseCase;
import com.galeritos.risk_guard.identity.application.usecase.SuspendUserUseCase;
import com.galeritos.risk_guard.identity.domain.model.User;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    private final ApproveUserUseCase approveUserUseCase;
    private final DenyUserUseCase denyUserUseCase;
    private final SuspendUserUseCase suspendUserUseCase;
    private final ListPendingUsersUseCase listPendingUsersUseCase;
    private final GetUserDetailUseCase getUserDetailUseCase;

    public AdminUserController(
            ApproveUserUseCase approveUserUseCase,
            DenyUserUseCase denyUserUseCase,
            SuspendUserUseCase suspendUserUseCase,
            ListPendingUsersUseCase listPendingUsersUseCase,
            GetUserDetailUseCase getUserDetailUseCase) {
        this.approveUserUseCase = approveUserUseCase;
        this.denyUserUseCase = denyUserUseCase;
        this.suspendUserUseCase = suspendUserUseCase;
        this.listPendingUsersUseCase = listPendingUsersUseCase;
        this.getUserDetailUseCase = getUserDetailUseCase;
    }

    @Operation(summary = "List pending users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending users returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserSummaryResponse>> listPendingUsers() {
        List<AdminUserSummaryResponse> response = listPendingUsersUseCase.execute().stream()
                .map(user -> new AdminUserSummaryResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getDocument(),
                        user.getStatus(),
                        user.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user detail by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User detail returned"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable UUID userId) {
        User user = getUserDetailUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserDetailResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDocument(),
                user.getRole(),
                user.getStatus(),
                user.isSuspect(),
                user.getCreatedAt()));
    }

    @Operation(summary = "Approve pending user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User approved"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserStatusResponse> approveUser(@PathVariable UUID userId) {
        User user = approveUserUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus()));
    }

    @Operation(summary = "Deny onboarding for pending user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User denied"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserStatusResponse> denyUser(@PathVariable UUID userId) {
        User user = denyUserUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus()));
    }

    @Operation(summary = "Suspend active user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User suspended"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserStatusResponse> suspendUser(@PathVariable UUID userId) {
        User user = suspendUserUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus()));
    }
}
