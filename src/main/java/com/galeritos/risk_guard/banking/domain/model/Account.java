package com.galeritos.risk_guard.banking.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.galeritos.risk_guard.banking.domain.exception.InsufficientBalanceException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal reservedBalance;

    protected Account() {
    }

    public void reserve(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        balance = balance.subtract(amount);
        reservedBalance = reservedBalance.add(amount);
    }

    public Account(UUID id, UUID userId, BigDecimal balance, BigDecimal reservedBalance) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.reservedBalance = reservedBalance;
    }
}
