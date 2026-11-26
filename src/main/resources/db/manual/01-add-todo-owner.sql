-- Run once against an existing todos table, with the application stopped.
-- Legacy rows remain inaccessible until their owner is explicitly assigned.
ALTER TABLE todos
    ADD COLUMN user_id BIGINT NULL,
    ADD INDEX idx_todos_user_id (user_id),
    ADD CONSTRAINT fk_todos_user FOREIGN KEY (user_id) REFERENCES users(id);
-- Assign verified owners explicitly, for example:
-- UPDATE todos SET user_id = 123 WHERE id IN (1, 2, 3);
-- Verify all assignments before running 02-require-todo-owner.sql.
SELECT id FROM todos WHERE user_id IS NULL;