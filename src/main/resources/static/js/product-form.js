// Formulario de producto: margen en vivo y ayuda sobre la fecha de vencimiento del stock inicial.
// Solo ayuda a llenar; las reglas las valida el servidor.
(function () {
    'use strict';

    var form = document.querySelector('form.form');
    if (!form) {
        return;
    }
    var price = form.querySelector('[name="salePrice"]');
    var cost = form.querySelector('[name="cost"]');
    var preview = form.querySelector('[data-margin-preview]');
    var money = new Intl.NumberFormat('es-CO', {maximumFractionDigits: 0});

    function updateMargin() {
        if (!price || !cost || !preview) {
            return;
        }
        var p = parseFloat(price.value);
        var c = parseFloat(cost.value);
        if (!(p > 0) || !(c > 0)) {
            preview.textContent = 'Escribe el precio y el costo para ver el margen.';
            preview.removeAttribute('data-state');
            return;
        }
        var percent = (p - c) / p * 100;
        preview.textContent = 'Margen ' + percent.toFixed(1).replace('.', ',') + ' % · $ ' + money.format(p - c)
            + ' por unidad';
        preview.setAttribute('data-state', p < c ? 'negative' : 'ok');
    }

    if (price && cost) {
        price.addEventListener('input', updateMargin);
        cost.addEventListener('input', updateMargin);
        updateMargin();
    }

    var perishable = form.querySelector('[data-perishable]');
    var expirationHint = form.querySelector('[data-expiration-hint]');

    function updateExpirationHint() {
        if (!perishable || !expirationHint) {
            return;
        }
        expirationHint.textContent = perishable.checked
            ? 'Obligatoria si escribes una cantidad: el producto es perecedero.'
            : 'No aplica: el producto no es perecedero. Déjala vacía.';
    }

    if (perishable) {
        perishable.addEventListener('change', updateExpirationHint);
        updateExpirationHint();
    }
})();
