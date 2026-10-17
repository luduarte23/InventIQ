// Tablas de Inventario y Productos: buscador, filtros por tipo y estado, y filas de lotes que se
// despliegan. Todo pasa en el navegador sobre la tabla que ya llegó del servidor.
(function () {
    'use strict';

    function normalize(text) {
        return (text || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    }

    function detailsOf(row) {
        var id = row.getAttribute('data-details');
        return id ? document.getElementById(id) : null;
    }

    function setExpanded(row, expanded) {
        var details = detailsOf(row);
        var button = row.querySelector('[data-toggle-details]');
        if (!details || !button) {
            return;
        }
        row.setAttribute('data-expanded', String(expanded));
        button.setAttribute('aria-expanded', String(expanded));
        details.hidden = !expanded || row.hidden;
    }

    // Filas de lotes: sin JavaScript se ven todas; con JavaScript empiezan cerradas.
    document.querySelectorAll('tr[data-row]').forEach(function (row) {
        setExpanded(row, false);
        var button = row.querySelector('[data-toggle-details]');
        if (button) {
            button.addEventListener('click', function () {
                setExpanded(row, row.getAttribute('data-expanded') !== 'true');
            });
        }
    });

    document.querySelectorAll('[data-filter-table]').forEach(function (bar) {
        var table = document.getElementById(bar.getAttribute('data-filter-table'));
        if (!table) {
            return;
        }
        var rows = Array.prototype.slice.call(table.querySelectorAll('tbody tr[data-row]'));
        var text = bar.querySelector('[data-filter-text]');
        var selects = Array.prototype.slice.call(bar.querySelectorAll('[data-filter-key]'));
        var count = bar.querySelector('[data-filter-count]');
        var empty = document.querySelector('[data-filter-empty="' + table.id + '"]');

        function matches(row) {
            var query = normalize(text ? text.value : '').trim();
            if (query && normalize(row.getAttribute('data-search')).indexOf(query) === -1) {
                return false;
            }
            return selects.every(function (select) {
                if (!select.value) {
                    return true;
                }
                var tokens = (row.getAttribute('data-' + select.getAttribute('data-filter-key')) || '').split(' ');
                return tokens.indexOf(select.value) !== -1;
            });
        }

        function apply() {
            var visible = 0;
            rows.forEach(function (row) {
                var show = matches(row);
                row.hidden = !show;
                var details = detailsOf(row);
                if (details) {
                    details.hidden = !show || row.getAttribute('data-expanded') !== 'true';
                }
                if (show) {
                    visible++;
                }
            });
            if (count) {
                count.textContent = visible + (visible === 1 ? ' producto' : ' productos');
            }
            if (empty) {
                empty.hidden = visible !== 0;
            }
            return visible;
        }

        if (text) {
            text.addEventListener('input', apply);
        }
        selects.forEach(function (select) {
            select.addEventListener('change', apply);
        });

        // Desde Productos → "Lotes" se llega con el código ya escrito: si queda un solo
        // producto, sus lotes se abren solos.
        if (apply() === 1 && text && text.value) {
            rows.filter(function (row) { return !row.hidden; }).forEach(function (row) {
                setExpanded(row, true);
                row.scrollIntoView({block: 'center'});
            });
        }
    });
})();
