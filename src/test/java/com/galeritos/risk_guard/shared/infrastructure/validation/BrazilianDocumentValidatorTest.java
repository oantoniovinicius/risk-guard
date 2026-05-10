package com.galeritos.risk_guard.shared.infrastructure.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BrazilianDocumentValidatorTest {

    private BrazilianDocumentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BrazilianDocumentValidator();
    }

    // --- null / blank passthrough ---

    @Test
    void shouldReturnTrueForNull() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldReturnTrueForBlank() {
        assertTrue(validator.isValid("  ", null));
    }

    // --- CPF ---

    @Test
    void shouldAcceptValidCpf() {
        assertTrue(validator.isValid("52998224725", null));
    }

    @Test
    void shouldRejectCpfWithAllSameDigits() {
        assertFalse(validator.isValid("11111111111", null));
    }

    @Test
    void shouldRejectCpfWithWrongFirstCheckDigit() {
        assertFalse(validator.isValid("52998224735", null)); // digit 9 changed
    }

    @Test
    void shouldRejectCpfWithWrongSecondCheckDigit() {
        assertFalse(validator.isValid("52998224726", null)); // last digit changed
    }

    @Test
    void shouldRejectCpfWithNonDigitChars() {
        assertFalse(validator.isValid("529.982.247-25", null));
    }

    // --- CNPJ ---

    @Test
    void shouldAcceptValidCnpj() {
        assertTrue(validator.isValid("11444777000161", null));
    }

    @Test
    void shouldRejectCnpjWithAllSameDigits() {
        assertFalse(validator.isValid("11111111111111", null));
    }

    @Test
    void shouldRejectCnpjWithWrongFirstCheckDigit() {
        assertFalse(validator.isValid("11444777000171", null)); // digit 12 changed
    }

    @Test
    void shouldRejectCnpjWithWrongSecondCheckDigit() {
        assertFalse(validator.isValid("11444777000162", null)); // last digit changed
    }

    @Test
    void shouldRejectCnpjWithNonDigitChars() {
        assertFalse(validator.isValid("11.444.777/0001-61", null));
    }

    // --- wrong length ---

    @Test
    void shouldRejectDocumentWithWrongLength() {
        assertFalse(validator.isValid("1234567890", null));   // 10 digits
        assertFalse(validator.isValid("123456789012345", null)); // 15 digits
    }
}
