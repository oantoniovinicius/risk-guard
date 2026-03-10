CREATE TABLE accounts (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL UNIQUE,

    balance NUMERIC(15,2) NOT NULL,
    reserved_balance NUMERIC(15,2) NOT NULL,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);