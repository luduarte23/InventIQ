// Gráficas de Administración → Historial (Chart.js 4, servido desde el JAR).
// Los datos llegan del servidor en window.inventiqHistory; aquí solo se dibujan.
// Reglas de diseño: un eje por gráfica (nunca dos), una serie con un color (la del título), leyenda
// solo con 2 o más series, marcas finas, rejilla tenue, tooltip en cada gráfica y "Ver tabla"
// para leer todos los valores sin pasar el cursor.
(function () {
    'use strict';

    var data = window.inventiqHistory;
    if (!data || typeof Chart === 'undefined') {
        return;
    }

    var css = getComputedStyle(document.documentElement);

    function token(name) {
        return css.getPropertyValue(name).trim();
    }

    var color = {
        series1: token('--series-1'),
        series2: token('--series-2'),
        series1Hover: '#1c5cab',
        ordinal: [token('--ordinal-1'), token('--ordinal-2'), token('--ordinal-3')],
        grid: token('--chart-grid'),
        axis: token('--chart-axis'),
        muted: token('--chart-muted'),
        ink: token('--ink'),
        ink2: token('--ink-2'),
        surface: token('--surface'),
        border: token('--border')
    };

    var MONTHS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
    var pesos = new Intl.NumberFormat('es-CO', {maximumFractionDigits: 0});
    var decimal = new Intl.NumberFormat('es-CO', {maximumFractionDigits: 1});

    function money(value) {
        return '$ ' + pesos.format(value);
    }

    function compactMoney(value) {
        var absolute = Math.abs(value);
        if (absolute >= 1e6) {
            return '$ ' + decimal.format(value / 1e6) + ' M';
        }
        if (absolute >= 1e4) {
            return '$ ' + pesos.format(value / 1e3) + ' mil';
        }
        return money(value);
    }

    function plain(value) {
        return pesos.format(value);
    }

    function unitsText(value) {
        return pesos.format(value) + (value === 1 ? ' unidad' : ' unidades');
    }

    function ticketsText(value) {
        return pesos.format(value) + (value === 1 ? ' venta' : ' ventas');
    }

    function percent(value) {
        return decimal.format(value) + ' %';
    }

    function capitalize(text) {
        return text.charAt(0).toUpperCase() + text.slice(1);
    }

    function withAlpha(hex, alpha) {
        var n = parseInt(hex.replace('#', ''), 16);
        return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + alpha + ')';
    }

    function dayLabel(epochDay) {
        var date = new Date(Math.round(epochDay) * 86400000);
        return date.getUTCDate() + ' ' + MONTHS[date.getUTCMonth()] + ' ' + String(date.getUTCFullYear()).slice(2);
    }

    // ---- Apariencia común ----------------------------------------------------------------
    Chart.defaults.font.family = token('--font') || 'system-ui, sans-serif';
    Chart.defaults.font.size = 12;
    Chart.defaults.color = color.muted;
    Chart.defaults.borderColor = color.grid;
    Chart.defaults.plugins.legend.display = false;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        Chart.defaults.animation = false;
    }
    var tooltip = Chart.defaults.plugins.tooltip;
    tooltip.backgroundColor = color.surface;
    tooltip.titleColor = color.ink2;
    tooltip.bodyColor = color.ink;
    tooltip.footerColor = color.ink2;
    tooltip.borderColor = color.border;
    tooltip.borderWidth = 1;
    tooltip.padding = 10;
    tooltip.cornerRadius = 8;
    tooltip.boxPadding = 6;
    tooltip.usePointStyle = true;
    tooltip.titleFont = {weight: '500'};
    tooltip.bodyFont = {weight: '600'};
    tooltip.footerFont = {weight: '500'};

    // Línea vertical que sigue el cursor en las gráficas de línea: se apunta a una fecha, no a la línea.
    var crosshair = {
        id: 'crosshair',
        afterDatasetsDraw: function (chart) {
            var active = chart.tooltip && chart.tooltip.getActiveElements();
            if (!active || !active.length) {
                return;
            }
            var x = active[0].element.x;
            var area = chart.chartArea;
            var ctx = chart.ctx;
            ctx.save();
            ctx.beginPath();
            ctx.moveTo(x, area.top);
            ctx.lineTo(x, area.bottom);
            ctx.lineWidth = 1;
            ctx.strokeStyle = color.axis;
            ctx.stroke();
            ctx.restore();
        }
    };

    // Etiquetas directas al final de cada línea (gráfica de precios). Si quedan muy cerca, se
    // separan y se unen a su línea con un trazo corto.
    var endLabels = {
        id: 'endLabels',
        afterDatasetsDraw: function (chart) {
            var ctx = chart.ctx;
            var labels = [];
            chart.data.datasets.forEach(function (dataset, index) {
                var meta = chart.getDatasetMeta(index);
                if (meta.hidden || !meta.data.length) {
                    return;
                }
                var point = meta.data[meta.data.length - 1];
                var value = dataset.data[dataset.data.length - 1];
                labels.push({x: point.x, y: point.y, labelY: point.y, color: dataset.borderColor,
                    text: dataset.label + ' ' + money(value.y)});
            });
            labels.sort(function (a, b) { return a.y - b.y; });
            for (var i = 1; i < labels.length; i++) {
                var gap = labels[i].labelY - labels[i - 1].labelY;
                if (gap < 18) {
                    labels[i - 1].labelY -= (18 - gap) / 2;
                    labels[i].labelY += (18 - gap) / 2;
                }
            }
            ctx.save();
            ctx.font = '600 12px ' + Chart.defaults.font.family;
            ctx.textBaseline = 'middle';
            labels.forEach(function (label) {
                ctx.strokeStyle = label.color;
                ctx.lineWidth = 2;
                ctx.beginPath();
                ctx.moveTo(label.x + 6, label.y);
                ctx.lineTo(label.x + 16, label.labelY);
                ctx.stroke();
                ctx.fillStyle = color.ink;
                ctx.fillText(label.text, label.x + 20, label.labelY);
            });
            ctx.restore();
        }
    };

    // Nombre junto a las tres burbujas con más ingresos (el resto, en el tooltip y en la tabla).
    var topBubbleLabels = {
        id: 'topBubbleLabels',
        afterDatasetsDraw: function (chart) {
            var meta = chart.getDatasetMeta(0);
            var points = chart.data.datasets[0].data;
            var order = points.map(function (p, i) { return i; })
                .sort(function (a, b) { return points[b].revenue - points[a].revenue; })
                .slice(0, 3);
            var ctx = chart.ctx;
            ctx.save();
            ctx.font = '500 12px ' + Chart.defaults.font.family;
            ctx.fillStyle = color.ink2;
            ctx.textBaseline = 'middle';
            order.forEach(function (index) {
                var element = meta.data[index];
                if (element) {
                    ctx.fillText(points[index].name, element.x + points[index].r + 6, element.y);
                }
            });
            ctx.restore();
        }
    };

    // ---- Vista de tabla (el gemelo accesible de cada gráfica) -------------------------------
    function renderTable(container, columns, rows) {
        container.textContent = '';
        var table = document.createElement('table');
        var head = table.createTHead().insertRow();
        columns.forEach(function (column) {
            var th = document.createElement('th');
            th.textContent = column.label;
            if (column.num) {
                th.className = 'num';
            }
            head.appendChild(th);
        });
        var body = table.createTBody();
        rows.forEach(function (row) {
            var tr = body.insertRow();
            row.forEach(function (cell, index) {
                var td = tr.insertCell();
                td.textContent = cell;
                if (columns[index].num) {
                    td.className = 'num';
                }
            });
        });
        container.appendChild(table);
    }

    var registry = {};

    function refreshTable(entry) {
        if (entry && entry.toggle && entry.toggle.getAttribute('aria-pressed') === 'true') {
            var view = entry.table();
            renderTable(entry.tableBox, view.columns, view.rows);
        }
    }

    function setupCard(name, build) {
        var card = document.querySelector('[data-chart="' + name + '"]');
        if (!card) {
            return;
        }
        var entry = build(card, card.querySelector('canvas'));
        if (!entry) {
            return;
        }
        entry.toggle = card.querySelector('[data-table-toggle]');
        entry.tableBox = card.querySelector('.chart-table');
        var body = card.querySelector('.chart-body');
        if (entry.toggle && entry.tableBox) {
            entry.toggle.addEventListener('click', function () {
                var showTable = entry.toggle.getAttribute('aria-pressed') !== 'true';
                entry.toggle.setAttribute('aria-pressed', String(showTable));
                entry.toggle.lastChild.nodeValue = showTable ? 'Ver gráfica' : 'Ver tabla';
                body.hidden = showTable;
                entry.tableBox.hidden = !showTable;
                refreshTable(entry);
            });
        }
        registry[name] = entry;
    }

    function setupMetricToggle(card, onChange) {
        var buttons = Array.prototype.slice.call(card.querySelectorAll('[data-metric]'));
        buttons.forEach(function (button) {
            button.addEventListener('click', function () {
                buttons.forEach(function (other) {
                    other.setAttribute('aria-pressed', String(other === button));
                });
                onChange(button.getAttribute('data-metric'));
            });
        });
    }

    var lineKey = function () {
        return {pointStyle: 'line', rotation: 0};
    };

    function horizontalBar(canvas, labels, values, format, axisFormat) {
        return new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: color.series1,
                    hoverBackgroundColor: color.series1Hover,
                    maxBarThickness: 24,
                    borderRadius: {topLeft: 0, bottomLeft: 0, topRight: 4, bottomRight: 4},
                    borderSkipped: false
                }]
            },
            options: {
                indexAxis: 'y',
                maintainAspectRatio: false,
                interaction: {mode: 'index', axis: 'y', intersect: false},
                scales: {
                    x: {
                        beginAtZero: true,
                        grid: {color: color.grid},
                        border: {display: false},
                        ticks: {callback: axisFormat, maxTicksLimit: 5}
                    },
                    y: {grid: {display: false}, border: {color: color.axis}, ticks: {color: color.ink2}}
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function (item) {
                                return ' ' + format(item.parsed.x);
                            }
                        }
                    }
                }
            }
        });
    }

    // ---- 1. Tendencia diaria (línea con área; métrica intercambiable) ----------------------
    setupCard('trend', function (card, canvas) {
        var daily = data.daily;
        var metrics = {
            revenue: {label: 'Ingresos', title: 'Ingresos por día', values: daily.revenue, format: money, axis: compactMoney},
            units: {label: 'Unidades', title: 'Unidades vendidas por día', values: daily.units, format: unitsText, axis: plain},
            tickets: {label: 'Tickets', title: 'Ventas (tickets) por día', values: daily.tickets, format: ticketsText, axis: plain}
        };
        var current = metrics.revenue;
        var chart = new Chart(canvas, {
            type: 'line',
            data: {
                labels: daily.labels,
                datasets: [{
                    label: current.label,
                    data: current.values,
                    borderColor: color.series1,
                    backgroundColor: withAlpha(color.series1, 0.1),
                    fill: 'origin',
                    borderWidth: 2,
                    borderJoinStyle: 'round',
                    borderCapStyle: 'round',
                    tension: 0.25,
                    pointRadius: 0,
                    pointHoverRadius: 5,
                    pointHoverBackgroundColor: color.series1,
                    pointHoverBorderColor: color.surface,
                    pointHoverBorderWidth: 2,
                    pointHitRadius: 14
                }]
            },
            options: {
                maintainAspectRatio: false,
                interaction: {mode: 'index', intersect: false},
                scales: {
                    x: {grid: {display: false}, border: {color: color.axis}, ticks: {maxRotation: 0, autoSkipPadding: 16}},
                    y: {
                        beginAtZero: true,
                        grid: {color: color.grid},
                        border: {display: false},
                        ticks: {callback: function (value) { return current.axis(value); }, maxTicksLimit: 6}
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            title: function (items) { return daily.dates[items[0].dataIndex]; },
                            label: function (item) { return ' ' + current.format(item.parsed.y); },
                            labelPointStyle: lineKey
                        }
                    }
                }
            },
            plugins: [crosshair]
        });
        setupMetricToggle(card, function (metric) {
            current = metrics[metric];
            chart.data.datasets[0].label = current.label;
            chart.data.datasets[0].data = current.values;
            chart.update();
            card.querySelector('[data-chart-title]').textContent = current.title;
        });
        return {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Fecha'}, {label: 'Ingresos', num: true}, {label: 'Unidades', num: true},
                        {label: 'Tickets', num: true}],
                    rows: daily.dates.map(function (date, i) {
                        return [date, money(daily.revenue[i]), plain(daily.units[i]), plain(daily.tickets[i])];
                    })
                };
            }
        };
    });

    // ---- 2. Ingresos por tipo (barras horizontales) ---------------------------------------
    setupCard('categories', function (card, canvas) {
        var categories = data.categories;
        var chart = horizontalBar(canvas, categories.labels, categories.revenue, money, compactMoney);
        return {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Tipo'}, {label: 'Ingresos', num: true}, {label: 'Unidades', num: true}],
                    rows: categories.labels.map(function (label, i) {
                        return [label, money(categories.revenue[i]), plain(categories.units[i])];
                    })
                };
            }
        };
    });

    // ---- 3. Productos más vendidos (barras horizontales; ingresos o unidades) --------------
    setupCard('products', function (card, canvas) {
        var sets = {
            revenue: {source: data.topByRevenue, label: 'Ingresos', title: 'Productos más vendidos por ingresos',
                format: money, axis: compactMoney},
            units: {source: data.topByUnits, label: 'Unidades', title: 'Productos más vendidos por unidades',
                format: unitsText, axis: plain}
        };
        var current = sets.revenue;
        var chart = horizontalBar(canvas, current.source.labels, current.source.values,
            function (value) { return current.format(value); },
            function (value) { return current.axis(value); });
        var entry = {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Producto'}, {label: current.label, num: true}],
                    rows: current.source.labels.map(function (label, i) {
                        return [label, current.format(current.source.values[i])];
                    })
                };
            }
        };
        setupMetricToggle(card, function (metric) {
            current = sets[metric];
            chart.data.labels = current.source.labels;
            chart.data.datasets[0].data = current.source.values;
            chart.update();
            card.querySelector('[data-chart-title]').textContent = current.title;
            refreshTable(entry);
        });
        return entry;
    });

    // ---- 4. Día de la semana (columnas) ---------------------------------------------------
    setupCard('weekdays', function (card, canvas) {
        var weekdays = data.weekdays;
        var chart = new Chart(canvas, {
            type: 'bar',
            data: {
                labels: weekdays.labels,
                datasets: [{
                    data: weekdays.average,
                    backgroundColor: color.series1,
                    hoverBackgroundColor: color.series1Hover,
                    maxBarThickness: 24,
                    borderRadius: {topLeft: 4, topRight: 4, bottomLeft: 0, bottomRight: 0},
                    borderSkipped: false
                }]
            },
            options: {
                maintainAspectRatio: false,
                interaction: {mode: 'index', intersect: false},
                scales: {
                    x: {grid: {display: false}, border: {color: color.axis}, ticks: {color: color.ink2}},
                    y: {
                        beginAtZero: true,
                        grid: {color: color.grid},
                        border: {display: false},
                        ticks: {callback: compactMoney, maxTicksLimit: 5}
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            title: function (items) { return capitalize(weekdays.names[items[0].dataIndex]); },
                            label: function (item) { return ' ' + money(item.parsed.y) + ' en promedio'; },
                            afterLabel: function (item) {
                                return ' ' + ticketsText(weekdays.tickets[item.dataIndex]) + ' en el período';
                            }
                        }
                    }
                }
            }
        });
        return {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Día'}, {label: 'Ingresos promedio', num: true}, {label: 'Ventas en el período', num: true}],
                    rows: weekdays.names.map(function (name, i) {
                        return [capitalize(name), money(weekdays.average[i]), plain(weekdays.tickets[i])];
                    })
                };
            }
        };
    });

    // ---- 5. Franja del día (dona: parte de un todo con 3 partes ordenadas) -----------------
    setupCard('timeBands', function (card, canvas) {
        var bands = data.timeBands;
        var total = bands.revenue.reduce(function (sum, value) { return sum + Number(value); }, 0);

        function share(value) {
            return total > 0 ? Number(value) / total * 100 : 0;
        }

        var chart = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: bands.labels,
                datasets: [{
                    data: bands.revenue,
                    backgroundColor: color.ordinal,
                    borderColor: color.surface,
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                maintainAspectRatio: false,
                cutout: '62%',
                plugins: {
                    legend: {
                        display: true,
                        position: 'right',
                        labels: {
                            color: color.ink2,
                            usePointStyle: true,
                            pointStyle: 'rectRounded',
                            padding: 14,
                            generateLabels: function (c) {
                                var base = Chart.overrides.doughnut.plugins.legend.labels.generateLabels(c);
                                return base.map(function (label, i) {
                                    label.text = bands.labels[i] + ' · ' + percent(share(bands.revenue[i]));
                                    return label;
                                });
                            }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (item) {
                                return ' ' + money(item.parsed) + ' · ' + percent(share(item.parsed));
                            },
                            afterLabel: function (item) {
                                return ' ' + capitalize(bands.ranges[item.dataIndex]) + ' · '
                                    + ticketsText(bands.tickets[item.dataIndex]);
                            }
                        }
                    }
                }
            }
        });
        return {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Franja'}, {label: 'Horario'}, {label: 'Ingresos', num: true},
                        {label: 'Participación', num: true}, {label: 'Ventas', num: true}],
                    rows: bands.labels.map(function (label, i) {
                        return [label, bands.ranges[i], money(bands.revenue[i]), percent(share(bands.revenue[i])),
                            plain(bands.tickets[i])];
                    })
                };
            }
        };
    });

    // ---- 6. Margen y rotación (burbujas, una sola serie) -----------------------------------
    setupCard('performance', function (card, canvas) {
        var points = data.performance.map(function (p) {
            return {name: p.name, category: p.category, x: Number(p.unitsPerDay), y: Number(p.margin),
                revenue: Number(p.revenue)};
        });
        var maxRevenue = Math.max.apply(null, points.map(function (p) { return p.revenue; }).concat([1]));
        points.forEach(function (p) {
            p.r = 5 + 13 * Math.sqrt(p.revenue / maxRevenue);
        });
        var chart = new Chart(canvas, {
            type: 'bubble',
            data: {
                datasets: [{
                    label: 'Productos',
                    data: points,
                    backgroundColor: withAlpha(color.series1, 0.55),
                    borderColor: color.surface,
                    borderWidth: 2,
                    hoverBackgroundColor: color.series1,
                    hoverBorderColor: color.surface,
                    hoverBorderWidth: 2
                }]
            },
            options: {
                maintainAspectRatio: false,
                layout: {padding: {right: 24}},
                interaction: {mode: 'nearest', intersect: false},
                scales: {
                    x: {
                        beginAtZero: true,
                        title: {display: true, text: 'Unidades vendidas por día (rotación)', color: color.ink2},
                        grid: {color: color.grid},
                        border: {color: color.axis}
                    },
                    y: {
                        title: {display: true, text: 'Margen bruto', color: color.ink2},
                        grid: {color: color.grid},
                        border: {display: false},
                        ticks: {callback: function (value) { return value + ' %'; }}
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            title: function (items) { return items[0].raw.name; },
                            afterTitle: function (items) { return items[0].raw.category; },
                            label: function (item) {
                                var p = item.raw;
                                return [' Margen: ' + percent(p.y), ' Rotación: ' + decimal.format(p.x) + ' unidades/día',
                                    ' Ingresos: ' + money(p.revenue)];
                            }
                        }
                    }
                }
            },
            plugins: [topBubbleLabels]
        });
        return {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Producto'}, {label: 'Tipo'}, {label: 'Unidades por día', num: true},
                        {label: 'Margen', num: true}, {label: 'Ingresos', num: true}],
                    rows: points.slice().sort(function (a, b) { return b.revenue - a.revenue; }).map(function (p) {
                        return [p.name, p.category, decimal.format(p.x), percent(p.y), money(p.revenue)];
                    })
                };
            }
        };
    });

    // ---- 7. Precios de compra y venta (línea escalonada, 2 series) -------------------------
    setupCard('prices', function (card, canvas) {
        var products = data.prices;
        if (!products.length) {
            return null;
        }
        var byId = {};
        products.forEach(function (product) { byId[String(product.id)] = product; });
        var select = document.querySelector('[data-price-product]');
        var current = byId[String(data.selectedProduct)] || products[0];
        var title = card.querySelector('[data-chart-title]');

        function series(product, label, key, seriesColor) {
            var last = product.points.length - 1;
            return {
                label: label,
                data: product.points.map(function (point) {
                    return {x: point.x, y: Number(point[key]), label: point.label, change: point.change};
                }),
                borderColor: seriesColor,
                backgroundColor: seriesColor,
                stepped: 'after',
                borderWidth: 2,
                pointRadius: function (ctx) { return ctx.dataIndex === last ? 0 : 4; },
                pointBackgroundColor: seriesColor,
                pointBorderColor: color.surface,
                pointBorderWidth: 2,
                pointHoverRadius: 6,
                pointHitRadius: 14
            };
        }

        function datasets(product) {
            return [series(product, 'Precio de venta', 'price', color.series1),
                series(product, 'Costo', 'cost', color.series2)];
        }

        var chart = new Chart(canvas, {
            type: 'line',
            data: {datasets: datasets(current)},
            options: {
                maintainAspectRatio: false,
                layout: {padding: {right: 190, top: 4}},
                interaction: {mode: 'index', intersect: false},
                scales: {
                    x: {
                        type: 'linear',
                        grid: {display: false},
                        border: {color: color.axis},
                        ticks: {callback: dayLabel, maxTicksLimit: 7}
                    },
                    y: {
                        grace: '15%',
                        grid: {color: color.grid},
                        border: {display: false},
                        ticks: {callback: compactMoney, maxTicksLimit: 6}
                    }
                },
                plugins: {
                    legend: {
                        display: true,
                        position: 'top',
                        align: 'start',
                        labels: {color: color.ink2, usePointStyle: true, pointStyle: 'line', boxWidth: 20, padding: 16}
                    },
                    tooltip: {
                        callbacks: {
                            title: function (items) {
                                var raw = items[0].raw;
                                return raw.change ? raw.label : raw.label + ' (hoy)';
                            },
                            label: function (item) { return ' ' + item.dataset.label + ': ' + money(item.parsed.y); },
                            labelPointStyle: lineKey,
                            footer: function (items) {
                                var price = items.filter(function (i) { return i.datasetIndex === 0; })[0];
                                var cost = items.filter(function (i) { return i.datasetIndex === 1; })[0];
                                if (!price || !cost || price.parsed.y <= 0) {
                                    return '';
                                }
                                var margin = price.parsed.y - cost.parsed.y;
                                return 'Margen: ' + percent(margin / price.parsed.y * 100) + ' · ' + money(margin)
                                    + ' por unidad';
                            }
                        }
                    }
                }
            },
            plugins: [crosshair, endLabels]
        });

        var entry = {
            chart: chart,
            table: function () {
                return {
                    columns: [{label: 'Desde'}, {label: 'Precio de venta', num: true}, {label: 'Costo', num: true},
                        {label: 'Margen', num: true}],
                    rows: current.points.filter(function (point) { return point.change; }).map(function (point) {
                        var price = Number(point.price);
                        var cost = Number(point.cost);
                        return [point.label, money(price), money(cost),
                            price > 0 ? percent((price - cost) / price * 100) : '—'];
                    })
                };
            }
        };

        function show(product) {
            current = product;
            chart.data.datasets = datasets(product);
            chart.update();
            title.textContent = 'Precio de venta y costo: ' + product.name;
            if (select) {
                select.value = String(product.id);
            }
            refreshTable(entry);
        }

        title.textContent = 'Precio de venta y costo: ' + current.name;
        if (select) {
            select.addEventListener('change', function () {
                show(byId[select.value]);
                try {
                    var url = new URL(window.location.href);
                    url.searchParams.set('product', select.value);
                    window.history.replaceState(null, '', url);
                } catch (e) {
                    // Navegadores sin URL/History: el selector funciona igual.
                }
            });
        }
        // "Ver evolución" en la tabla de precios: cambia de producto sin recargar la página.
        document.querySelectorAll('[data-select-product]').forEach(function (link) {
            link.addEventListener('click', function (event) {
                var product = byId[link.getAttribute('data-select-product')];
                if (!product) {
                    return;
                }
                event.preventDefault();
                show(product);
                document.getElementById('evolucion').scrollIntoView({behavior: 'smooth', block: 'start'});
            });
        });
        return entry;
    });
})();
