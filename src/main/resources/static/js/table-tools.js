// Tabla de Inventario: buscador, filtro por estado y filas de lotes que se despliegan.
// Todo pasa en el navegador sobre la tabla que ya llegó del servidor; sin JavaScript se ve todo.
(function () {
    'use strict';

    var bar = document.querySelector('[data-filter-table]');
    var table = bar && document.getElementById(bar.getAttribute('data-filter-table'));
    if (!table) {
        return;
    }
    var rows = Array.prototype.slice.call(table.querySelectorAll('tbody tr[data-row]'));
    var text = bar.querySelector('[data-filter-text]');
    var status = bar.querySelector('[data-filter-key]');
    var count = bar.querySelector('[data-filter-count]');
    var empty = document.querySelector('[data-filter-empty="' + table.id + '"]');

    function normalize(value) {
        return (value || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim();
    }

    function details(row) {
        var id = row.getAttribute('data-details');
        return id ? document.getElementById(id) : null;
    }

    // Los lotes empiezan cerrados y se ven solo si su producto está visible y desplegado.
    function sync(row) {
        var lots = details(row);
        if (lots) {
            lots.hidden = row.hidden || row.getAttribute('data-expanded') !== 'true';
        }
    }

    function apply() {
        var query = normalize(text.value);
        var visible = 0;
        rows.forEach(function (row) {
            var matchesText = normalize(row.getAttribute('data-search')).indexOf(query) !== -1;
            var matchesStatus = !status.value
                || (row.getAttribute('data-status') || '').split(' ').indexOf(status.value) !== -1;
            row.hidden = !(matchesText && matchesStatus);
            sync(row);
            visible += row.hidden ? 0 : 1;
        });
        count.textContent = visible + (visible === 1 ? ' producto' : ' productos');
        empty.hidden = visible !== 0;
    }

    rows.forEach(function (row) {
        var button = row.querySelector('[data-toggle-details]');
        if (button) {
            button.addEventListener('click', function () {
                var expanded = row.getAttribute('data-expanded') !== 'true';
                row.setAttribute('data-expanded', String(expanded));
                button.setAttribute('aria-expanded', String(expanded));
                sync(row);
            });
        }
    });
    text.addEventListener('input', apply);
    status.addEventListener('change', apply);
    apply();
})();
