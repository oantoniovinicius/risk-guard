package com.galeritos.risk_guard.analyst.infrastructure.controller;

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

import com.galeritos.risk_guard.analyst.application.usecase.GetAnalystTransactionDetailUseCase;
import com.galeritos.risk_guard.analyst.application.usecase.ListAnalystQueueUseCase;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystTransactionDetailResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.dto.AnalystTransactionPageResponse;
import com.galeritos.risk_guard.analyst.infrastructure.controller.mapper.AnalystTransactionMapper;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.shared.enums.RiskLevel;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/analyst/transactions")
public class AnalystTransactionController {

    private final ListAnalystQueueUseCase listAnalystQueueUseCase;
    private final GetAnalystTransactionDetailUseCase getAnalystTransactionDetailUseCase;

    public AnalystTransactionController(
            ListAnalystQueueUseCase listAnalystQueueUseCase,
            GetAnalystTransactionDetailUseCase getAnalystTransactionDetailUseCase) {
        this.listAnalystQueueUseCase = listAnalystQueueUseCase;
        this.getAnalystTransactionDetailUseCase = getAnalystTransactionDetailUseCase;
    }

    @Operation(summary = "List analyst review queue",
            description = "Returns paginated AWAITING_ANALYST transactions sorted oldest-first. Supports optional filters by risk level and date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Queue returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<AnalystTransactionPageResponse> listQueue(
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Transaction> result = listAnalystQueueUseCase.execute(riskLevel, from, to, page, size);
        return ResponseEntity.ok(new AnalystTransactionPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(AnalystTransactionMapper::toSummary).toList()));
    }

    @Operation(summary = "Get full transaction detail for analyst review",
            description = "Returns transaction data, sender profile, risk analysis, and full decision history assembled in one response.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction detail returned"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasRole('ANALYST')")
    public ResponseEntity<AnalystTransactionDetailResponse> getDetail(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(AnalystTransactionMapper.toDetail(
                getAnalystTransactionDetailUseCase.execute(transactionId)));
    }
}
