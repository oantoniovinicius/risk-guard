CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    
    sender_user_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,

    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',

    status VARCHAR(30) NOT NULL,

    risk_score NUMERIC(5,2),
    risk_level VARCHAR(20),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    FOREIGN KEY (sender_user_id) REFERENCES users(id),
    FOREIGN KEY (recipient_user_id) REFERENCES users(id)
);