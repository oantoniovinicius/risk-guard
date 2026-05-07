CREATE TABLE transaction_decision_history (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    analyst_id UUID NOT NULL,
    decision VARCHAR(50) NOT NULL,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_txn_decision_history_transaction_id ON transaction_decision_history (transaction_id);
