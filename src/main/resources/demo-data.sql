INSERT INTO merchants (id, name, contact_email, default_currency) VALUES
('10000000-0000-0000-0000-000000000001', 'Demo Shop', 'shop@example.com', 'USD'),
('10000000-0000-0000-0000-000000000002', 'Demo Books', 'books@example.com', 'UAH');

INSERT INTO merchant_providers (merchant_id, provider_id) VALUES
('10000000-0000-0000-0000-000000000001', 'stripe'),
('10000000-0000-0000-0000-000000000001', 'liqpay'),
('10000000-0000-0000-0000-000000000002', 'liqpay');

INSERT INTO payments (id, version, status, amount, currency, merchant_reference, merchant_id, provider_id, created_at) VALUES
('20000000-0000-0000-0000-000000000001', 0, 'PROCESSING', 19.99, 'USD', 'demo-order-1', '10000000-0000-0000-0000-000000000001', 'stripe', '2026-01-01T10:00:00Z'),
('20000000-0000-0000-0000-000000000002', 0, 'SUCCEEDED', 250.00, 'UAH', 'demo-order-2', '10000000-0000-0000-0000-000000000002', 'liqpay', '2026-01-01T11:00:00Z'),
('20000000-0000-0000-0000-000000000003', 0, 'PROCESSING', 5.00, 'EUR', 'demo-without-attempts', NULL, 'stripe', '2026-01-01T12:00:00Z');

INSERT INTO payment_attempts (id, payment_id, provider_id, attempt_number, result, created_at) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'liqpay', 1, 'FAILED', '2026-01-01T10:00:01Z'),
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', 'stripe', 2, 'SUBMITTED', '2026-01-01T10:00:02Z'),
('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'liqpay', 1, 'SUCCEEDED', '2026-01-01T11:00:01Z');
