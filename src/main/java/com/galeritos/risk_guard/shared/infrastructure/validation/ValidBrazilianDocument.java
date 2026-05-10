package com.galeritos.risk_guard.shared.infrastructure.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrazilianDocumentValidator.class)
public @interface ValidBrazilianDocument {
    String message() default "invalid CPF or CNPJ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
