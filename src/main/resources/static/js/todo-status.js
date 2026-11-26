(() => {
    const error = document.getElementById('todo-status-error');
    window.createTodoStatusButton = todo => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'todo-status';
        button.dataset.id = todo.id;
        button.dataset.completed = String(Boolean(todo.completed));
        button.setAttribute('aria-pressed', button.dataset.completed);
        button.innerHTML = '<svg class="completion-mark" viewBox="0 0 32 32" aria-hidden="true"><path pathLength="1" d="M6 17 L13 25 L27 7"/></svg><span class="completion-label"></span>';
        button.querySelector('.completion-label').textContent = todo.completed ? '完了' : '未完了';
        return button;
    };

    document.getElementById('todo-list').addEventListener('click', async event => {
        const button = event.target.closest('.todo-status');
        if (!button || button.disabled) return;
        const completed = button.dataset.completed !== 'true';
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        error.hidden = true;
        try {
            const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
            const token = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            if (token && header) headers[header.content] = token.content;
            const response = await fetch(`/api/todos/${encodeURIComponent(button.dataset.id)}/completed`, {
                method: 'PATCH', credentials: 'same-origin', headers,
                body: JSON.stringify({ completed })
            });
            if (!response.ok) throw new Error('save');
            const todo = await response.json();
            button.dataset.completed = String(Boolean(todo.completed));
            button.setAttribute('aria-pressed', button.dataset.completed);
            button.querySelector('.completion-label').textContent = todo.completed ? '完了' : '未完了';
            if (todo.completed && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                const animations = Array.from(button.querySelectorAll('path'), path =>
                    path.animate([{ strokeDashoffset: 1 }, { strokeDashoffset: 0 }], {
                        duration: 840, easing: 'ease-in-out', fill: 'both'
                    }));
                await Promise.all(animations.map(animation => animation.finished));
                animations.forEach(animation => animation.cancel());
            }
        } catch {
            error.textContent = '状態を更新できませんでした。ページを再読み込みして状態を確認してください。';
            error.hidden = false;
        } finally {
            button.disabled = false;
            button.removeAttribute('aria-busy');
        }
    });
})();
