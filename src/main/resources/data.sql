INSERT INTO payment_providers (id, name, priority)
SELECT 'stripe', 'Stripe', 1 WHERE NOT EXISTS (SELECT 1 FROM payment_providers WHERE id = 'stripe');
INSERT INTO payment_providers (id, name, priority)
SELECT 'liqpay', 'LiqPay', 2 WHERE NOT EXISTS (SELECT 1 FROM payment_providers WHERE id = 'liqpay');
