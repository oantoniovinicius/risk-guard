package com.galeritos.risk_guard.admin.infrastructure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.admin.application.usecase.GetAdminSettingsUseCase;
import com.galeritos.risk_guard.admin.application.usecase.UpdateAdminSettingsUseCase;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminSettingsResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.UpdateAdminSettingsRequest;
import com.galeritos.risk_guard.admin.infrastructure.controller.mapper.AdminSettingsMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/parameters")
public class AdminParametersController {

    private final GetAdminSettingsUseCase getAdminSettingsUseCase;
    private final UpdateAdminSettingsUseCase updateAdminSettingsUseCase;

    public AdminParametersController(
            GetAdminSettingsUseCase getAdminSettingsUseCase,
            UpdateAdminSettingsUseCase updateAdminSettingsUseCase) {
        this.getAdminSettingsUseCase = getAdminSettingsUseCase;
        this.updateAdminSettingsUseCase = updateAdminSettingsUseCase;
    }

    @Operation(summary = "Get admin risk parameters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin settings returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSettingsResponse> getSettings() {
        return ResponseEntity.ok(AdminSettingsMapper.toResponse(getAdminSettingsUseCase.execute()));
    }

    @Operation(summary = "Patch admin risk parameters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin settings updated"),
            @ApiResponse(responseCode = "400", description = "Invalid settings"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSettingsResponse> patchSettings(@Valid @RequestBody UpdateAdminSettingsRequest request) {
        return ResponseEntity.ok(AdminSettingsMapper.toResponse(updateAdminSettingsUseCase.execute(
                request.mediumRiskThreshold(),
                request.highRiskThreshold())));
    }
}
