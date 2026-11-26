(() => {
    const form = document.getElementById('quick-todo-form');
    const input = document.getElementById('quick-todo-title');
    const button = form.querySelector('button');
    const error = document.getElementById('quick-todo-error');
    const status = document.getElementById('quick-todo-status');
    let sending = false;

    function showError(message) {
        error.textContent = message;
        error.hidden = false;
        input.setAttribute('aria-invalid', 'true');
    }

    input.addEventListener('input', () => {
        error.hidden = true;
        error.textContent = '';
        input.removeAttribute('aria-invalid');
        status.textContent = '';
    });

    function prependTodo(todo) {
        const row = document.createElement('tr');
        row.className = 'todo-slide-in';
        for (const value of [todo.title, todo.category, '中', '（入力なし）']) {
            const cell = document.createElement('td');
            cell.textContent = value;
            row.append(cell);
        }
        const statusCell = document.createElement('td');
        statusCell.append(window.createTodoStatusButton(todo));
        row.append(statusCell);
        const operations = document.createElement('td');
        operations.className = 'todo-operations';
        const base = new URL(form.action);
        base.pathname = base.pathname.replace(/\/api\/todo$/, '/todos/');
        for (const [action, label] of [['edit', '編集'], ['delete', '削除']]) {
            const link = document.createElement('a');
            link.className = 'link';
            link.href = `${base.pathname}${encodeURIComponent(todo.id)}/${action}`;
            link.textContent = label;
            operations.append(link, ' ');
        }
        const selection = document.createElement('td');
        selection.className = 'todo-selection';
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'todo-select';
        checkbox.name = 'ids';
        checkbox.value = todo.id;
        checkbox.setAttribute('form', 'bulk-delete-form');
        checkbox.setAttribute('aria-label', `${todo.title}を選択`);
        selection.append(checkbox);
        row.append(operations, selection);
        document.getElementById('todo-list').prepend(row);
        document.getElementById('todo-empty')?.remove();
        // The existing page totals describe the server-rendered search results.
        document.querySelector('.page-summary')?.setAttribute('hidden', '');
        document.dispatchEvent(new Event('todo:added'));
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (event.isComposing || sending) return;
        const title = input.value.trim();
        if (!title) return showError('タイトルを入力してください');
        if (title.length > 255) return showError('タイトルは255文字以内で入力してください');
        sending = true;
        button.disabled = true;
        input.readOnly = true;
        form.setAttribute('aria-busy', 'true');
        status.textContent = '';
        try {
            const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
            headers[document.querySelector('meta[name="_csrf_header"]').content] =
                document.querySelector('meta[name="_csrf"]').content;
            const response = await fetch(form.action, {
                method: 'POST', credentials: 'same-origin', headers,
                body: JSON.stringify({ title })
            });
            if (!response.ok) {
                const problem = await response.json().catch(() => ({}));
                showError(problem.errors?.map(item => item.message).join('、') ||
                    (response.status === 403 ? 'セッションが切れました。ページを再読み込みしてください。' :
                        '追加できませんでした。時間をおいて再度お試しください。'));
                return;
            }
            prependTodo(await response.json());
            input.value = '';
            error.hidden = true;
            input.removeAttribute('aria-invalid');
            status.textContent = 'タスクを追加しました';
        } catch {
            showError('通信に失敗しました。一覧を確認してから再度お試しください。');
        } finally {
            sending = false;
            button.disabled = false;
            input.readOnly = false;
            form.removeAttribute('aria-busy');
            input.focus();
        }
    });
    input.addEventListener('keydown', event => {
        if (event.key === 'Enter' && event.isComposing) event.preventDefault();
    });
})();
