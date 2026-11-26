(() => {
    const mobile = window.matchMedia('(max-width: 760px)');
    const panels = document.querySelectorAll('.list-panel');
    function updatePanels() {
        panels.forEach(panel => { panel.open = !mobile.matches; });
        document.querySelectorAll('.todo-operation-details').forEach(details => {
            details.open = !mobile.matches;
        });
    }
    updatePanels();
    mobile.addEventListener('change', updatePanels);

    // Apply labels to both server-rendered rows and rows added/replaced via AJAX.
    function labelCards() {
        const labels = ['やること', 'ジャンル', '優先度', '期限', '状態', 'メモ', '操作', '選択'];
        document.querySelectorAll('#todo-list tr').forEach(row => {
            Array.from(row.cells).forEach((cell, index) => {
                cell.dataset.label = labels[index];
            });
            const operations = row.querySelector('.todo-operations');
            if (operations && !operations.querySelector('details')) {
                const details = document.createElement('details');
                details.className = 'todo-operation-details';
                details.open = !mobile.matches;
                const summary = document.createElement('summary');
                summary.textContent = '編集・削除';
                details.append(summary, ...operations.childNodes);
                operations.append(details);
            }
        });
    }
    labelCards();
    document.addEventListener('todo:added', labelCards);
})();
