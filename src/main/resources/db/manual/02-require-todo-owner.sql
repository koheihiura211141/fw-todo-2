-- Run after assigning an owner to every legacy row.
-- Strict mode prevents NULL values being silently converted.
SET SESSION sql_mode = CONCAT_WS(',', @@sql_mode, 'STRICT_ALL_TABLES');
ALTER TABLE todos MODIFY COLUMN user_id BIGINT NOT NULL;