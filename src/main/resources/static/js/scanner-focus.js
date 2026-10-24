// Pantalla de ventas: mantiene el foco en el campo del código para que el cajero
// trabaje solo con el lector y el teclado (B6).
(function () {
    'use strict';

    var input = document.querySelector('[data-scanner-input]');
    if (!input) {
        return;
    }

    function focusScanner() {
        input.focus();
    }

    // Un clic en una zona vacía de la página devuelve el foco al campo del código.
    document.addEventListener('click', function (event) {
        if (!event.target.closest('input, button, select, textarea, a')) {
            focusScanner();
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'F2') {
            event.preventDefault();
            var closeForm = document.getElementById('close-sale-form');
            var closeButton = closeForm && closeForm.querySelector('button');
            if (closeButton && !closeButton.disabled) {
                closeForm.requestSubmit();
            }
        } else if (event.key === 'Escape') {
            input.value = '';
            focusScanner();
        }
    });
})();
