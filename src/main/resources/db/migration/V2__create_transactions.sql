CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,

    amount NUMERIC(15,2) NOT NULL,

    status VARCHAR(30) NOT NULL,
    financial_status VARCHAR(30) NOT NULL,

    risk_level VARCHAR(20),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);