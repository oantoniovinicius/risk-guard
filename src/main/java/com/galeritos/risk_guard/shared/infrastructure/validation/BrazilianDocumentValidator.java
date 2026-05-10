package com.galeritos.risk_guard.shared.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BrazilianDocumentValidator implements ConstraintValidator<ValidBrazilianDocument, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null/blank handled by @NotBlank
        }
        if (!value.matches("\\d+")) {
            return false;
        }
        return switch (value.length()) {
            case 11 -> isValidCpf(value);
            case 14 -> isValidCnpj(value);
            default -> false;
        };
    }

    private boolean isValidCpf(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        return cpfCheckDigit(cpf, 9) && cpfCheckDigit(cpf, 10);
    }

    private boolean cpfCheckDigit(String cpf, int position) {
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += (cpf.charAt(i) - '0') * (position + 1 - i);
        }
        int remainder = sum % 11;
        int expected = remainder < 2 ? 0 : 11 - remainder;
        return (cpf.charAt(position) - '0') == expected;
    }

    private boolean isValidCnpj(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return cnpjCheckDigit(cnpj, weights1, 12) && cnpjCheckDigit(cnpj, weights2, 13);
    }

    private boolean cnpjCheckDigit(String cnpj, int[] weights, int position) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (cnpj.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        int expected = remainder < 2 ? 0 : 11 - remainder;
        return (cnpj.charAt(position) - '0') == expected;
    }
}
