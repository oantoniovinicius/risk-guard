package com.galeritos.risk_guard.admin.infrastructure.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.galeritos.risk_guard.admin.application.usecase.ChangeUserRoleUseCase;
import com.galeritos.risk_guard.admin.application.usecase.GetAdminDecisionHistoryUseCase;
import com.galeritos.risk_guard.admin.domain.model.AdminDecisionHistory;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminDecisionHistoryItemResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminDecisionHistoryResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserDetailResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminRoleUpdateRequest;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserRoleResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserSummaryResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserStatusResponse;
import com.galeritos.risk_guard.identity.application.usecase.ApproveUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.BlockUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.DenyUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.GetUserDetailUseCase;
import com.galeritos.risk_guard.identity.application.usecase.ListPendingUsersUseCase;
import com.galeritos.risk_guard.identity.application.usecase.SuspendUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.UnblockUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.UnsuspendUserUseCase;
import com.galeritos.risk_guard.identity.domain.model.User;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    private final ApproveUserUseCase approveUserUseCase;
    private final DenyUserUseCase denyUserUseCase;
    private final SuspendUserUseCase suspendUserUseCase;
    private final BlockUserUseCase blockUserUseCase;
    private final UnsuspendUserUseCase unsuspendUserUseCase;
    private final UnblockUserUseCase unblockUserUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final GetAdminDecisionHistoryUseCase getAdminDecisionHistoryUseCase;
    private final ListPendingUsersUseCase listPendingUsersUseCase;
    private final GetUserDetailUseCase getUserDetailUseCase;

    public AdminUserController(
            ApproveUserUseCase approveUserUseCase,
            DenyUserUseCase denyUserUseCase,
            SuspendUserUseCase suspendUserUseCase,
            BlockUserUseCase blockUserUseCase,
            UnsuspendUserUseCase unsuspendUserUseCase,
            UnblockUserUseCase unblockUserUseCase,
            ChangeUserRoleUseCase changeUserRoleUseCase,
            GetAdminDecisionHistoryUseCase getAdminDecisionHistoryUseCase,
            ListPendingUsersUseCase listPendingUsersUseCase,
            GetUserDetailUseCase getUserDetailUseCase) {
        this.approveUserUseCase = approveUserUseCase;
        this.denyUserUseCase = denyUserUseCase;
        this.suspendUserUseCase = suspendUserUseCase;
        this.blockUserUseCase = blockUserUseCase;
        this.unsuspendUserUseCase = unsuspendUserUseCase;
        this.unblockUserUseCase = unblockUserUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
        this.getAdminDecisionHistoryUseCase = getAdminDecisionHistoryUseCase;
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

    @Operation(summary = "Block active user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User blocked"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserStatusResponse> blockUser(@PathVariable UUID userId) {
        User user = blockUserUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus()));
    }

    @Operation(summary = "Unsuspend suspended user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User unsuspended"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/unsuspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserStatusResponse> unsuspendUser(@PathVariable UUID userId) {
        User user = unsuspendUserUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus()));
    }

    @Operation(summary = "Unblock blocked user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User unblocked"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserStatusResponse> unblockUser(@PathVariable UUID userId) {
        User user = unblockUserUseCase.execute(userId);
        return ResponseEntity.ok(new AdminUserStatusResponse(user.getId(), user.getStatus()));
    }

    @Operation(summary = "Update user role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User role updated"),
            @ApiResponse(responseCode = "400", description = "Invalid role request"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserRoleResponse> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminRoleUpdateRequest request) {
        User user = changeUserRoleUseCase.execute(userId, request.role());
        return ResponseEntity.ok(new AdminUserRoleResponse(user.getId(), user.getRole()));
    }

    @Operation(summary = "Get decision history for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision history returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{userId}/decision-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDecisionHistoryResponse> getDecisionHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminDecisionHistory> historyPage = getAdminDecisionHistoryUseCase.execute(userId, page, size);
        return ResponseEntity.ok(new AdminDecisionHistoryResponse(
                historyPage.getNumber(),
                historyPage.getSize(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages(),
                historyPage.getContent().stream().map(this::toHistoryItem).toList()));
    }

    @Operation(summary = "Get global admin decision history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision history returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/decision-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDecisionHistoryResponse> getGlobalDecisionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminDecisionHistory> historyPage = getAdminDecisionHistoryUseCase.execute(null, page, size);
        return ResponseEntity.ok(new AdminDecisionHistoryResponse(
                historyPage.getNumber(),
                historyPage.getSize(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages(),
                historyPage.getContent().stream().map(this::toHistoryItem).toList()));
    }

    private AdminDecisionHistoryItemResponse toHistoryItem(AdminDecisionHistory item) {
        return new AdminDecisionHistoryItemResponse(
                item.getId(),
                item.getActorUserId(),
                item.getActorRole(),
                item.getTargetUserId(),
                item.getAction(),
                item.getFromStatus(),
                item.getToStatus(),
                item.getFromRole(),
                item.getToRole(),
                item.getReason(),
                item.getCreatedAt());
    }
}
