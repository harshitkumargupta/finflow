ALTER TABLE autopay_executions ALTER COLUMN status TYPE varchar(30);
ALTER TABLE autopay_authorizations ALTER COLUMN last_payment_status TYPE varchar(30);
