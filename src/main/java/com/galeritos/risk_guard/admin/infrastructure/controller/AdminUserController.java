package com.galeritos.risk_guard.admin.infrastructure.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.admin.application.usecase.ChangeUserRoleUseCase;
import com.galeritos.risk_guard.admin.application.usecase.GetAdminDecisionHistoryUseCase;
import com.galeritos.risk_guard.admin.application.usecase.GetUserTransactionsUseCase;
import com.galeritos.risk_guard.admin.application.usecase.ListUsersUseCase;
import com.galeritos.risk_guard.admin.domain.model.AdminDecisionHistory;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminDecisionHistoryResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminRoleUpdateRequest;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminTransactionPageResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserDetailResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserPageResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserRoleResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserStatusResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminUserSummaryResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.mapper.AdminDecisionHistoryMapper;
import com.galeritos.risk_guard.admin.infrastructure.controller.mapper.AdminTransactionMapper;
import com.galeritos.risk_guard.admin.infrastructure.controller.mapper.AdminUserMapper;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.identity.application.usecase.ApproveUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.BlockUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.DenyUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.GetUserDetailUseCase;
import com.galeritos.risk_guard.identity.application.usecase.ListPendingUsersUseCase;
import com.galeritos.risk_guard.identity.application.usecase.SuspendUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.ResetPinLockUseCase;
import com.galeritos.risk_guard.identity.application.usecase.ResetPinUseCase;
import com.galeritos.risk_guard.identity.application.usecase.UnblockUserUseCase;
import com.galeritos.risk_guard.identity.application.usecase.UnsuspendUserUseCase;
import com.galeritos.risk_guard.identity.domain.model.User;
import com.galeritos.risk_guard.identity.domain.model.enums.Role;
import com.galeritos.risk_guard.identity.domain.model.enums.UserStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Admin - Users", description = "User moderation, role management, and decision history")
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
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserTransactionsUseCase getUserTransactionsUseCase;
    private final ResetPinLockUseCase resetPinLockUseCase;
    private final ResetPinUseCase resetPinUseCase;

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
            GetUserDetailUseCase getUserDetailUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserTransactionsUseCase getUserTransactionsUseCase,
            ResetPinLockUseCase resetPinLockUseCase,
            ResetPinUseCase resetPinUseCase) {
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
        this.listUsersUseCase = listUsersUseCase;
        this.getUserTransactionsUseCase = getUserTransactionsUseCase;
        this.resetPinLockUseCase = resetPinLockUseCase;
        this.resetPinUseCase = resetPinUseCase;
    }

    @Operation(summary = "List pending users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending users returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserSummaryResponse>> listPendingUsers() {
        return ResponseEntity.ok(listPendingUsersUseCase.execute().stream()
                .map(AdminUserMapper::toSummary)
                .toList());
    }

    @Operation(summary = "List all users with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserPageResponse> listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> result = listUsersUseCase.execute(status, role, page, size);
        return ResponseEntity.ok(new AdminUserPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(AdminUserMapper::toSummary).toList()));
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
        return ResponseEntity.ok(AdminUserMapper.toDetail(getUserDetailUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toStatus(approveUserUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toStatus(denyUserUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toStatus(suspendUserUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toStatus(blockUserUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toStatus(unsuspendUserUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toStatus(unblockUserUseCase.execute(userId)));
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
        return ResponseEntity.ok(AdminUserMapper.toRole(changeUserRoleUseCase.execute(userId, request.role())));
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
        return ResponseEntity.ok(
                AdminDecisionHistoryMapper.toPageResponse(getAdminDecisionHistoryUseCase.execute(userId, page, size)));
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
        return ResponseEntity.ok(
                AdminDecisionHistoryMapper.toPageResponse(getAdminDecisionHistoryUseCase.execute(null, page, size)));
    }

    @Operation(summary = "Reset PIN lock for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "PIN lock reset"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/reset-pin-lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPinLock(@PathVariable UUID userId) {
        resetPinLockUseCase.execute(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reset PIN for a user (clears PIN so the user can define a new one)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "PIN reset — user can now set a new PIN via POST /user/pin"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/{userId}/reset-pin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPin(@PathVariable UUID userId) {
        resetPinUseCase.execute(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get paginated transaction history for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history returned"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{userId}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTransactionPageResponse> getUserTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Transaction> result = getUserTransactionsUseCase.execute(userId, page, size);
        return ResponseEntity.ok(new AdminTransactionPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(AdminTransactionMapper::toResponse).toList()));
    }
}
