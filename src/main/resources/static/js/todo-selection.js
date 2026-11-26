(() => {
    const form = document.getElementById('bulk-delete-form');
    const selectAll = document.getElementById('select-all');
    const checkboxes = Array.from(document.querySelectorAll('.todo-select'));
    const deleteButton = document.getElementById('bulk-delete-button');
    const selectionCount = document.getElementById('selection-count');

    function updateSelection() {
        const count = checkboxes.filter(checkbox => checkbox.checked).length;
        selectAll.checked = count > 0 && count === checkboxes.length;
        selectAll.indeterminate = count > 0 && count < checkboxes.length;
        deleteButton.disabled = count === 0;
        selectionCount.textContent = `${count}件選択中`;
        return count;
    }

    selectAll.addEventListener('change', () => {
        checkboxes.forEach(checkbox => { checkbox.checked = selectAll.checked; });
        updateSelection();
    });
    checkboxes.forEach(checkbox => checkbox.addEventListener('change', updateSelection));
    form.addEventListener('submit', event => {
        const count = updateSelection();
        if (count === 0 || !window.confirm(`選択した${count}件を削除しますか？`)) {
            event.preventDefault();
        }
    });
    window.addEventListener('pageshow', updateSelection);
    updateSelection();
})();
