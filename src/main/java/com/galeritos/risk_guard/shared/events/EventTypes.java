package com.galeritos.risk_guard.shared.events;

public final class EventTypes {
    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String TRANSACTION_ANALYZED = "transaction.analyzed";
    public static final String TRANSACTION_AWAITING_CUSTOMER = "transaction.awaiting_customer";
    public static final String TRANSACTION_AWAITING_ANALYST = "transaction.awaiting_analyst";
    public static final String TRANSACTION_APPROVED = "transaction.approved";
    public static final String TRANSACTION_DENIED = "transaction.denied";
    public static final String TRANSACTION_FRAUD_CONFIRMED = "transaction.fraud_confirmed";

    private EventTypes() {
    }
}
