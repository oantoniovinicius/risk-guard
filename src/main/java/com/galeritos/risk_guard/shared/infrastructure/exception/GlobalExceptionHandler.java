package com.galeritos.risk_guard.shared.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.galeritos.risk_guard.banking.domain.exception.AccountNotFoundException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidAnalystDecisionStateException;
import com.galeritos.risk_guard.banking.domain.exception.InsufficientBalanceException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidCustomerConfirmationStateException;
import com.galeritos.risk_guard.banking.domain.exception.InvalidTransferException;
import com.galeritos.risk_guard.banking.domain.exception.TransactionNotFoundException;
import com.galeritos.risk_guard.identity.domain.exception.DocumentAlreadyInUseException;
import com.galeritos.risk_guard.identity.domain.exception.EmailAlreadyInUseException;
import com.galeritos.risk_guard.identity.domain.exception.ForbiddenTransferOperationException;
import com.galeritos.risk_guard.identity.domain.exception.InvalidCredentialsException;
import com.galeritos.risk_guard.identity.domain.exception.InvalidRegistrationRoleException;
import com.galeritos.risk_guard.identity.domain.exception.InvalidUserStatusTransitionException;
import com.galeritos.risk_guard.identity.domain.exception.UnauthenticatedAccessException;
import com.galeritos.risk_guard.identity.domain.exception.UserCredentialDisabledException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotActiveException;
import com.galeritos.risk_guard.identity.domain.exception.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<?> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<?> handleInvalidTransfer(InvalidTransferException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<?> handleBalance(InsufficientBalanceException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<?> handleTransactionNotFound(TransactionNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCustomerConfirmationStateException.class)
    public ResponseEntity<?> handleInvalidCustomerState(InvalidCustomerConfirmationStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAnalystDecisionStateException.class)
    public ResponseEntity<?> handleInvalidAnalystState(InvalidAnalystDecisionStateException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<?> handleEmailConflict(EmailAlreadyInUseException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DocumentAlreadyInUseException.class)
    public ResponseEntity<?> handleDocumentConflict(DocumentAlreadyInUseException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({ InvalidCredentialsException.class, UserCredentialDisabledException.class })
    public ResponseEntity<?> handleInvalidCredentials(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthenticatedAccessException.class)
    public ResponseEntity<?> handleUnauthenticated(UnauthenticatedAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({ ForbiddenTransferOperationException.class, InvalidRegistrationRoleException.class })
    public ResponseEntity<?> handleForbiddenTransfer(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({ UserNotActiveException.class, InvalidUserStatusTransitionException.class })
    public ResponseEntity<?> handleUserStatusConflict(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Access denied."));
    }
}
