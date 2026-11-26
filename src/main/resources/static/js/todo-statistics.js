(() => {
    const section = document.getElementById('todo-statistics');
    if (!section) return;
    const status = document.getElementById('statistics-status');
    const charts = new Map();
    let revision = 0;
    const centerTotal = {
        id: 'centerTotal',
        afterDraw(chart) {
            const {ctx, chartArea} = chart;
            if (!chartArea) return;
            const x = (chartArea.left + chartArea.right) / 2;
            const y = (chartArea.top + chartArea.bottom) / 2;
            const total = chart.data.datasets[0].data.reduce((sum, n) => sum + n, 0);
            ctx.save();
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillStyle = '#17304a';
            ctx.font = 'bold 28px sans-serif';
            ctx.fillText(total.toLocaleString(), x, y - 10);
            ctx.font = 'bold 11px sans-serif';
            ctx.fillText('タスク総数', x, y + 15);
            ctx.restore();
        }
    };
    const segmentLabels = {
        id: 'segmentLabels',
        afterDatasetsDraw(chart) {
            const ctx = chart.ctx;
            const doughnut = chart.config.type === 'doughnut';
            const splitLabels = {
                'プログラミング': ['プログラミング'],
                'マーケティング': ['マーケ', 'ティング'],
                '就職活動': ['就職', '活動']
            };
            ctx.save();
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            chart.getDatasetMeta(0).data.forEach((arc, index) => {
                const value = chart.data.datasets[0].data[index];
                if (!value || !chart.getDataVisibility(index)) return;
                const {x, y, startAngle, endAngle, innerRadius, outerRadius} = arc.getProps(
                    ['x', 'y', 'startAngle', 'endAngle', 'innerRadius', 'outerRadius'], true);
                if (endAngle - startAngle < 0.23) return;
                const angle = (startAngle + endAngle) / 2;
                const radius = doughnut ? (innerRadius + outerRadius) / 2 : outerRadius * 0.60;
                const tx = x + Math.cos(angle) * radius;
                const ty = y + Math.sin(angle) * radius;
                const label = chart.data.labels[index];
                const lines = doughnut ? (splitLabels[label] || [label]) : [label];
                const size = doughnut ? 12 : 15;
                const lineHeight = size + 2;
                const top = ty - lines.length * lineHeight / 2;
                ctx.fillStyle = doughnut && label === 'マーケティング' ? '#17384a' : '#fff';
                ctx.font = `700 ${size}px sans-serif`;
                lines.forEach((line, i) => ctx.fillText(line, tx, top + i * lineHeight, doughnut ? 86 : 100));
                ctx.font = `700 ${doughnut ? 18 : 24}px sans-serif`;
                ctx.fillText(value.toLocaleString(), tx, top + lines.length * lineHeight + 4);
            });
            ctx.restore();
        }
    };
    function render(kind, rows) {
        const canvas = document.getElementById(`${kind}-chart`);
        const summary = rows.map(row => `${row.label}: ${row.count}件`).join('、') || 'タスクはありません';

        canvas.setAttribute('aria-label', summary);
        charts.get(kind)?.destroy();
        const categoryColors = {
            'その他': '#ff555b', 'デザイン': '#39c94a', 'プログラミング': '#9460e8',
            'マーケティング': '#ffc426', '就職活動': '#08bbd4', '資格': '#ed59ad'
        };
        const colors = kind === 'priority' ? ['#ff655f', '#ffc526', '#13b2aa'] :
            rows.map((row, i) => categoryColors[row.label] || `hsl(${(i * 137.5) % 360}, 60%, 58%)`);
        const badges = document.getElementById(`${kind}-summary`);
        badges.replaceChildren();
        rows.forEach((row, index) => {
            const badge = document.createElement('li');
            badge.className = 'statistics-badge';
            badge.style.setProperty('--badge-color', colors[index]);
            const dot = document.createElement('span');
            dot.className = 'statistics-badge-dot';
            dot.setAttribute('aria-hidden', 'true');
            const label = document.createElement('span');
            label.className = 'statistics-badge-label';
            label.textContent = row.label;
            const count = document.createElement('strong');
            count.className = 'statistics-badge-count';
            count.textContent = `${row.count.toLocaleString()}件`;
            badge.append(dot, label, count);
            badges.append(badge);
        });
        charts.set(kind, new Chart(canvas, {
            type: kind === 'category' ? 'doughnut' : 'pie',
            data: {labels: rows.map(row => row.label), datasets: [{
                data: rows.map(row => row.count), backgroundColor: colors, borderWidth: 2
            }]},
            options: {
                responsive: true, maintainAspectRatio: false,
                ...(kind === 'category' ? {cutout: '42%'} : {}),
                plugins: {legend: {display: false}}
            },
            plugins: kind === 'category' ? [centerTotal, segmentLabels] : [segmentLabels]
        }));
    }
    async function load() {
        const current = ++revision;
        section.setAttribute('aria-busy', 'true');
        status.textContent = 'グラフを読み込み中…';
        try {
            if (typeof Chart === 'undefined') throw new Error('Chart unavailable');
            const results = await Promise.all([section.dataset.categoryUrl, section.dataset.priorityUrl].map(async url => {
                const response = await fetch(url, {headers: {Accept: 'application/json'}, credentials: 'same-origin'});
                if (!response.ok) throw new Error('Statistics request failed');
                return response.json();
            }));
            if (current !== revision) return;
            render('category', results[0]);
            render('priority', results[1]);
            status.textContent = results[0].some(row => row.count > 0) ? '' : 'タスクはありません';
        } catch {
            if (current !== revision) return;
            charts.forEach(chart => chart.destroy());
            charts.clear();
            for (const kind of ['category', 'priority']) document.getElementById(`${kind}-summary`).textContent = '';
            status.textContent = 'グラフを取得できませんでした。ページを再読み込みしてください。';
        } finally {
            if (current === revision) section.removeAttribute('aria-busy');
        }
    }
    document.addEventListener('todo:added', load);
    load();
})();
