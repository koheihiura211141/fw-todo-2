(() => {
    const table = document.querySelector('.todo-table');
    const filters = document.getElementById('list-filters');
    const error = document.getElementById('todo-sort-error');
    let loading = false;

    table.addEventListener('click', async event => {
        const link = event.target.closest('.column-sort');
        if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        if (loading) return;
        loading = true;
        table.setAttribute('aria-busy', 'true');
        error.hidden = true;
        try {
            const response = await fetch(link.href, { credentials: 'same-origin' });
            if (!response.ok || response.redirected) throw new Error('sort');
            const page = new DOMParser().parseFromString(await response.text(), 'text/html');
            const rows = page.getElementById('todo-list');
            const nextLinks = page.querySelectorAll('.column-sort');
            const links = table.querySelectorAll('.column-sort');
            if (!rows || nextLinks.length !== links.length) throw new Error('sort');

            const x = window.scrollX;
            const y = window.scrollY;
            const top = link.getBoundingClientRect().top;
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
            for (const name of ['sortBy', 'order']) {
                filters.elements[name].value = page.querySelector(`#list-filters input[name="${name}"]`).value;
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
            window.scrollTo({ left: x, top: y + link.getBoundingClientRect().top - top, behavior: 'instant' });
        } catch {
            error.textContent = '並び替えできませんでした。もう一度お試しください。';
            error.hidden = false;
        } finally {
            loading = false;
            table.removeAttribute('aria-busy');
        }
    });
})();
