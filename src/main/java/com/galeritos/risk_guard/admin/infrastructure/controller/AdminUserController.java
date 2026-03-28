package com.galeritos.risk_guard.admin.infrastructure.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.admin.infrastructure.controller.dto.ApproveUserResponse;
import com.galeritos.risk_guard.identity.application.usecase.ApproveUserUseCase;
import com.galeritos.risk_guard.identity.domain.model.User;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    private final ApproveUserUseCase approveUserUseCase;

    public AdminUserController(ApproveUserUseCase approveUserUseCase) {
        this.approveUserUseCase = approveUserUseCase;
    }

    @PostMapping("/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApproveUserResponse> approveUser(@PathVariable UUID userId) {
        User user = approveUserUseCase.execute(userId);
        return ResponseEntity.ok(new ApproveUserResponse(user.getId(), user.getStatus()));
    }
}
