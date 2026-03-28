package com.galeritos.risk_guard.banking.infrastructure.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.banking.application.usecase.CreateTransferUseCase;
import com.galeritos.risk_guard.banking.application.usecase.HandleAnalystDecisionUseCase;
import com.galeritos.risk_guard.banking.application.usecase.HandleCustomerConfirmationUseCase;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.AnalystDecisionRequest;
import com.galeritos.risk_guard.banking.application.usecase.dto.CreateTransferCommand;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CustomerConfirmationRequest;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferRequest;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferResponse;
import com.galeritos.risk_guard.identity.application.usecase.TransferAccessGuardUseCase;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final CreateTransferUseCase createTransferUseCase;
    private final HandleCustomerConfirmationUseCase handleCustomerConfirmationUseCase;
    private final HandleAnalystDecisionUseCase handleAnalystDecisionUseCase;
    private final TransferAccessGuardUseCase transferAccessGuardUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase,
            HandleCustomerConfirmationUseCase handleCustomerConfirmationUseCase,
            HandleAnalystDecisionUseCase handleAnalystDecisionUseCase,
            TransferAccessGuardUseCase transferAccessGuardUseCase) {
        this.createTransferUseCase = createTransferUseCase;
        this.handleCustomerConfirmationUseCase = handleCustomerConfirmationUseCase;
        this.handleAnalystDecisionUseCase = handleAnalystDecisionUseCase;
        this.transferAccessGuardUseCase = transferAccessGuardUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateTransferResponse> createTransfer(@RequestBody CreateTransferRequest request) {
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

    @PostMapping("/{transactionId}/customer-confirmation")
    public ResponseEntity<Void> confirmTransactionByCustomer(
            @PathVariable UUID transactionId,
            @RequestBody CustomerConfirmationRequest request) {
        transferAccessGuardUseCase.assertCanConfirmCustomerDecision(transactionId);
        handleCustomerConfirmationUseCase.execute(transactionId, request.decision());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{transactionId}/analyst-decision")
    public ResponseEntity<Void> decideTransactionAsAnalyst(
            @PathVariable UUID transactionId,
            @RequestBody AnalystDecisionRequest request) {
        transferAccessGuardUseCase.assertCanActAsAnalyst();
        handleAnalystDecisionUseCase.execute(transactionId, request.decision());
        return ResponseEntity.noContent().build();
    }
}
