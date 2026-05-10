package com.galeritos.risk_guard.banking.infrastructure.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.banking.application.usecase.CreateTransferUseCase;
import com.galeritos.risk_guard.banking.application.usecase.GetTransactionStatusUseCase;
import com.galeritos.risk_guard.banking.application.usecase.HandleAnalystDecisionUseCase;
import com.galeritos.risk_guard.banking.application.usecase.HandleCustomerConfirmationUseCase;
import com.galeritos.risk_guard.banking.application.usecase.OpenDisputeUseCase;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.AnalystDecisionRequest;
import com.galeritos.risk_guard.banking.application.usecase.dto.CreateTransferCommand;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CustomerConfirmationRequest;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferRequest;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferResponse;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.TransactionStatusResponse;
import com.galeritos.risk_guard.identity.application.usecase.TransferAccessGuardUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Transfers", description = "Transfer creation, status, and decision endpoints")
@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final CreateTransferUseCase createTransferUseCase;
    private final GetTransactionStatusUseCase getTransactionStatusUseCase;
    private final HandleCustomerConfirmationUseCase handleCustomerConfirmationUseCase;
    private final HandleAnalystDecisionUseCase handleAnalystDecisionUseCase;
    private final OpenDisputeUseCase openDisputeUseCase;
    private final TransferAccessGuardUseCase transferAccessGuardUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase,
            GetTransactionStatusUseCase getTransactionStatusUseCase,
            HandleCustomerConfirmationUseCase handleCustomerConfirmationUseCase,
            HandleAnalystDecisionUseCase handleAnalystDecisionUseCase,
            OpenDisputeUseCase openDisputeUseCase,
            TransferAccessGuardUseCase transferAccessGuardUseCase) {
        this.createTransferUseCase = createTransferUseCase;
        this.getTransactionStatusUseCase = getTransactionStatusUseCase;
        this.handleCustomerConfirmationUseCase = handleCustomerConfirmationUseCase;
        this.handleAnalystDecisionUseCase = handleAnalystDecisionUseCase;
        this.openDisputeUseCase = openDisputeUseCase;
        this.transferAccessGuardUseCase = transferAccessGuardUseCase;
    }

    @Operation(summary = "Create a transfer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "User not allowed to transfer (not ACTIVE)")
    })
    @PostMapping
    public ResponseEntity<CreateTransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        transferAccessGuardUseCase.assertCanCreateTransfer(request.senderId());

        CreateTransferCommand command = new CreateTransferCommand(
                request.senderId(),
                request.receiverId(),
                request.amount());

        Transaction transaction = createTransferUseCase.execute(command);

        CreateTransferResponse response = new CreateTransferResponse(
                transaction.getId(),
                transaction.getSenderId(),
                transaction.getReceiverId(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get transaction status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<TransactionStatusResponse> getTransactionStatus(@PathVariable UUID transactionId) {
        Transaction transaction = getTransactionStatusUseCase.execute(transactionId);
        String riskLevel = transaction.getRiskLevel() == null ? null : transaction.getRiskLevel().name();
        TransactionStatusResponse response = new TransactionStatusResponse(
                transaction.getId(),
                transaction.getStatus().name(),
                transaction.getFinancialStatus().name(),
                riskLevel,
                transaction.getAmount(),
                transaction.getSenderId(),
                transaction.getReceiverId(),
                transaction.getCustomerDecisionDeadlineAt(),
                transaction.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit customer confirmation or fraud report")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Decision recorded"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Wrong PIN or PIN blocked"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Transaction not awaiting customer confirmation"),
            @ApiResponse(responseCode = "422", description = "Invalid PIN")
    })
    @PostMapping("/{transactionId}/customer-confirmation")
    public ResponseEntity<Void> confirmTransactionByCustomer(
            @PathVariable UUID transactionId,
            @Valid @RequestBody CustomerConfirmationRequest request) {
        transferAccessGuardUseCase.assertCanConfirmCustomerDecision(transactionId);
        handleCustomerConfirmationUseCase.execute(transactionId, request.decision(), request.pin());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Submit analyst decision on a transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Decision recorded"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden — requires ANALYST role"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Transaction not awaiting analyst decision")
    })
    @PostMapping("/{transactionId}/analyst-decision")
    public ResponseEntity<Void> decideTransactionAsAnalyst(
            @PathVariable UUID transactionId,
            @Valid @RequestBody AnalystDecisionRequest request) {
        transferAccessGuardUseCase.assertCanActAsAnalyst();
        handleAnalystDecisionUseCase.execute(transactionId, request.decision(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Open a dispute on an approved transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dispute opened"),
            @ApiResponse(responseCode = "403", description = "Forbidden — not the sender or user not ACTIVE"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Transaction is not in APPROVED status")
    })
    @PostMapping("/{transactionId}/dispute")
    public ResponseEntity<Void> openDispute(@PathVariable UUID transactionId) {
        transferAccessGuardUseCase.assertCanOpenDispute(transactionId);
        openDisputeUseCase.execute(transactionId);
        return ResponseEntity.noContent().build();
    }
}
