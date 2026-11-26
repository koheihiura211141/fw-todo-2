(() => {
    const table = document.querySelector('.todo-table');
    const filters = document.getElementById('list-filters');
    const error = document.getElementById('todo-sort-error');
    let loading = false;

    table.addEventListener('click', event => {
        const link = event.target.closest('.column-sort');
        if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        updateList(link.href, link, '並び替え');
    });

    filters.addEventListener('submit', event => {
        event.preventDefault();
        const url = new URL(filters.action);
        url.search = new URLSearchParams(new FormData(filters)).toString();
        updateList(url.href, filters, '絞り込み');
    });

    filters.addEventListener('click', event => {
        const link = event.target.closest('.filter-reset');
        if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        updateList(link.href, filters, '絞り込み');
    });

    async function updateList(url, anchor, action) {
        if (loading) return;
        loading = true;
        table.setAttribute('aria-busy', 'true');
        error.hidden = true;
        try {
            const response = await fetch(url, { credentials: 'same-origin' });
            if (!response.ok || response.redirected) throw new Error('sort');
            const page = new DOMParser().parseFromString(await response.text(), 'text/html');
            const rows = page.getElementById('todo-list');
            const nextLinks = page.querySelectorAll('.column-sort');
            const links = table.querySelectorAll('.column-sort');
            if (!rows || nextLinks.length !== links.length) throw new Error('sort');

            const x = window.scrollX;
            const y = window.scrollY;
            const top = anchor.getBoundingClientRect().top;
            const selected = new Set(Array.from(table.querySelectorAll('.todo-select:checked'), box => box.value));
            rows.querySelectorAll('.todo-select').forEach(box => { box.checked = selected.has(box.value); });
            // Keep the tbody and header controls so their listeners and focus survive.
            document.getElementById('todo-list').replaceChildren(...rows.childNodes);
            links.forEach((control, index) => {
                const next = nextLinks[index];
                control.href = next.href;
                control.className = next.className;
                control.setAttribute('aria-label', next.getAttribute('aria-label'));
                control.closest('th').setAttribute('aria-sort', next.closest('th').getAttribute('aria-sort'));
            });
            for (const control of page.getElementById('list-filters').elements) {
                if (control.name && filters.elements[control.name]) {
                    filters.elements[control.name].value = control.value;
                }
            }
            for (const selector of ['.page-summary', '.pagination', '.filter-reset', '#todo-empty']) {
                const current = document.querySelector(selector);
                const next = page.querySelector(selector);
                if (current && next) current.replaceWith(next);
                else if (current) current.remove();
                else if (next) {
                    if (selector === '.filter-reset') filters.append(next);
                    else if (selector === '#todo-empty') table.before(next);
                    else table.parentElement.append(next);
                }
            }
            history.replaceState(history.state, '', response.url);
            document.dispatchEvent(new Event('todo:added'));
            window.scrollTo({ left: x, top: y + anchor.getBoundingClientRect().top - top, behavior: 'instant' });
        } catch {
            error.textContent = `${action}できませんでした。もう一度お試しください。`;
            error.hidden = false;
        } finally {
            loading = false;
            table.removeAttribute('aria-busy');
        }
    }
})();
