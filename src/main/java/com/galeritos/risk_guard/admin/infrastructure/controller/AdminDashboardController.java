package com.galeritos.risk_guard.admin.infrastructure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.admin.application.usecase.GetAdminStatsUseCase;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminStatsResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.mapper.AdminStatsMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin - Stats", description = "Aggregate metrics for the admin dashboard")
@RestController
@RequestMapping("/admin/stats")
public class AdminDashboardController {

    private final GetAdminStatsUseCase getAdminStatsUseCase;

    public AdminDashboardController(GetAdminStatsUseCase getAdminStatsUseCase) {
        this.getAdminStatsUseCase = getAdminStatsUseCase;
    }

    @Operation(summary = "Get admin dashboard aggregate stats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(AdminStatsMapper.toResponse(getAdminStatsUseCase.execute()));
    }
}
