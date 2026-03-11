CREATE TABLE risk_analysis (
    id UUID PRIMARY KEY,

    transaction_id UUID NOT NULL UNIQUE,
    score NUMERIC(5,2) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,

    explanation VARCHAR(40) NOT NULL,

    model_version VARCHAR(20) NOT NULL,

    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);