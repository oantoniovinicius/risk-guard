package com.galeritos.risk_guard.banking.infrastructure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.galeritos.risk_guard.banking.application.usecase.CreateTransferUseCase;
import com.galeritos.risk_guard.banking.application.usecase.dto.CreateTransferCommand;
import com.galeritos.risk_guard.banking.domain.model.Transaction;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferRequest;
import com.galeritos.risk_guard.banking.infrastructure.controller.dto.CreateTransferResponse;

@RestController
@RequestMapping("/transfers")
public class TransferController {
    private final CreateTransferUseCase createTransferUseCase;

    public TransferController(CreateTransferUseCase createTransferUseCase) {
        this.createTransferUseCase = createTransferUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateTransferResponse> createTransfer(@RequestBody CreateTransferRequest request) {
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
}
