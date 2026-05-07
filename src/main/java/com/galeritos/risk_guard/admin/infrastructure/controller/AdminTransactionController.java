package com.galeritos.risk_guard.admin.infrastructure.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.admin.application.usecase.GetTransactionDetailUseCase;
import com.galeritos.risk_guard.admin.application.usecase.ListTransactionsUseCase;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminTransactionPageResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.dto.AdminTransactionResponse;
import com.galeritos.risk_guard.admin.infrastructure.controller.mapper.AdminTransactionMapper;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.domain.model.enums.TransactionStatus;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/admin/transactions")
public class AdminTransactionController {

    private final ListTransactionsUseCase listTransactionsUseCase;
    private final GetTransactionDetailUseCase getTransactionDetailUseCase;

    public AdminTransactionController(
            ListTransactionsUseCase listTransactionsUseCase,
            GetTransactionDetailUseCase getTransactionDetailUseCase) {
        this.listTransactionsUseCase = listTransactionsUseCase;
        this.getTransactionDetailUseCase = getTransactionDetailUseCase;
    }

    @Operation(summary = "List all transactions with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction list returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTransactionPageResponse> listTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Transaction> result = listTransactionsUseCase.execute(status, riskLevel, from, to, page, size);
        return ResponseEntity.ok(new AdminTransactionPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(AdminTransactionMapper::toResponse).toList()));
    }

    @Operation(summary = "Get transaction detail by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction returned"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTransactionResponse> getTransactionDetail(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(AdminTransactionMapper.toResponse(getTransactionDetailUseCase.execute(transactionId)));
    }
}
