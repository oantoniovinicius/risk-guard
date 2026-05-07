CREATE TABLE admin_settings (
    id UUID PRIMARY KEY,
    medium_risk_threshold NUMERIC(5,4) NOT NULL,
    high_risk_threshold NUMERIC(5,4) NOT NULL,
    business_start_time TIME NOT NULL,
    business_end_time TIME NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
