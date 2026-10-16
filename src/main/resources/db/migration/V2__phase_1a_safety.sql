DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM payment_webhook_events WHERE event_id IS NOT NULL GROUP BY provider, event_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Cannot add webhook uniqueness: duplicate provider/event_id rows exist; run the dev preflight and resolve them first';
    END IF;
    IF EXISTS (SELECT 1 FROM autopay_executions WHERE idempotency_key IS NOT NULL
               GROUP BY autopay_authorization_id, idempotency_key HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Cannot add autopay execution uniqueness: duplicate mandate/idempotency rows exist';
    END IF;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS role varchar(20) DEFAULT 'USER';
UPDATE users SET role = 'USER' WHERE role IS NULL;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER';
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_pin_attempts integer DEFAULT 0;
UPDATE users SET failed_pin_attempts = 0 WHERE failed_pin_attempts IS NULL;
ALTER TABLE users ALTER COLUMN failed_pin_attempts SET DEFAULT 0;
ALTER TABLE users ALTER COLUMN failed_pin_attempts SET NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS pin_locked_until timestamp;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM payment_webhook_events WHERE event_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot make payment_webhook_events.event_id non-null: null event_id rows exist';
    END IF;
END $$;
ALTER TABLE payment_webhook_events ALTER COLUMN event_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_webhook_provider_event') THEN
        ALTER TABLE payment_webhook_events ADD CONSTRAINT uk_webhook_provider_event UNIQUE (provider, event_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_accounts_user_status_primary ON accounts(user_id, status, is_primary);
CREATE INDEX IF NOT EXISTS idx_transactions_account_created_desc ON transactions(account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_account_status_created_desc ON transactions(account_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_user_status_created_desc ON payments(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_payment_created_desc ON payment_attempts(payment_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reconciliation_reference_status ON reconciliation_records(payment_reference, status);
CREATE INDEX IF NOT EXISTS idx_security_audit_user_created_desc ON security_audit_logs(user_id, created_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_autopay_execution_authorization_idempotency') THEN
        ALTER TABLE autopay_executions ADD CONSTRAINT uk_autopay_execution_authorization_idempotency
            UNIQUE (autopay_authorization_id, idempotency_key);
    END IF;
END $$;
