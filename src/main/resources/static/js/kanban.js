(() => {
    const board = document.getElementById('kanban-board');
    if (!board) return;
    const error = document.getElementById('kanban-error');
    const message = document.getElementById('kanban-message');
    let dragged = null;
    // Serialize moves so rollback positions remain stable, even across cards.
    let saving = false;
    const clearHighlights = () => board.querySelectorAll('.drag-over').forEach(el => el.classList.remove('drag-over'));
    async function move(card, column) {
        const select = card.querySelector('select');
        const previousStatus = card.dataset.status;
        select.value = previousStatus;
        if (saving || previousStatus === column.dataset.status) return;
        const parent = card.parentNode;
        const next = card.nextSibling;
        const status = column.dataset.status;
        saving = true;
        error.hidden = true;
        message.textContent = '';
        card.setAttribute('aria-busy', 'true');
        board.querySelectorAll('select').forEach(el => { el.disabled = true; });
        column.querySelector('.kanban-cards').append(card);
        select.value = status;
        try {
            const token = document.querySelector('meta[name="_csrf"]');
            const header = document.querySelector('meta[name="_csrf_header"]');
            if (!token?.content || !header?.content) throw new Error('Missing CSRF token');
            const response = await fetch(card.dataset.url, {
                method: 'PATCH', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json', [header.content]: token.content },
                body: JSON.stringify({ status })
            });
            if (!response.ok || response.redirected) throw new Error('Update failed');
            const todo = await response.json();
            if (todo.status !== status) throw new Error('Unexpected status');
            card.dataset.status = status;
            message.textContent = 'ステータスを更新しました。';
        } catch {
            parent.insertBefore(card, next);
            select.value = previousStatus;
            error.textContent = '更新できなかったためカードを元に戻しました。通信エラーの場合は再読み込みして保存状態を確認してください。';
            error.hidden = false;
        } finally {
            saving = false;
            card.removeAttribute('aria-busy');
            board.querySelectorAll('select').forEach(el => { el.disabled = false; });
        }
    }
    board.addEventListener('dragstart', event => {
        const card = event.target.closest('.kanban-card');
        if (!card || saving || event.target.closest('select')) { event.preventDefault(); return; }
        dragged = card;
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', card.dataset.id);
        card.classList.add('dragging');
    });
    board.addEventListener('dragover', event => {
        const column = event.target.closest('.kanban-column');
        if (!dragged || saving || !column) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
        clearHighlights();
        column.classList.add('drag-over');
    });
    board.addEventListener('dragleave', event => {
        const column = event.target.closest('.kanban-column');
        if (column && !column.contains(event.relatedTarget)) column.classList.remove('drag-over');
    });
    board.addEventListener('drop', event => {
        const column = event.target.closest('.kanban-column');
        if (!dragged || !column) return;
        event.preventDefault();
        clearHighlights();
        void move(dragged, column);
    });
    board.addEventListener('dragend', () => {
        dragged?.classList.remove('dragging');
        dragged = null;
        clearHighlights();
    });
    board.addEventListener('change', event => {
        if (!event.target.matches('.kanban-status')) return;
        const column = board.querySelector(`[data-status="${event.target.value}"].kanban-column`);
        if (column) void move(event.target.closest('.kanban-card'), column);
    });
})();