-- Existing databases: run once before starting the updated application.
ALTER TABLE todos ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED';
UPDATE todos SET status = CASE WHEN completed THEN 'DONE' ELSE 'NOT_STARTED' END;