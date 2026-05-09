ALTER TABLE user_credentials
    ADD COLUMN failed_pin_attempts INT NOT NULL DEFAULT 0;
