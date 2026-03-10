CREATE TABLE risk_analysis (
    id UUID PRIMARY KEY,

    transaction_id UUID NOT NULL,
    risk_score NUMERIC(5,2),
    risk_level VARCHAR(20),

    decision VARCHAR(20),

    analyzed_at TIMESTAMP DEFAULT NOW(),

    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);