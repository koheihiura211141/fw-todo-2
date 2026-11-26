CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS sample_todo_initializations (
    user_id BIGINT NOT NULL PRIMARY KEY,
    initialized_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sample_todo_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS todos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255),
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_todos_user_id (user_id),
    CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- CREATE TABLE IF NOT EXISTS does not upgrade existing databases.
-- Keep this idempotent so existing installations and new databases both start.
SET @todo_status_migration = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE todos ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''NOT_STARTED''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'todos' AND column_name = 'status'
);
PREPARE todo_status_statement FROM @todo_status_migration;
EXECUTE todo_status_statement;
DEALLOCATE PREPARE todo_status_statement;
UPDATE todos SET status = 'DONE' WHERE completed = TRUE AND status <> 'DONE';