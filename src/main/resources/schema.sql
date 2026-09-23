CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    merchant_reference VARCHAR(64),
    provider_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
