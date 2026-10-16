// Comportamiento común de todas las páginas: barra lateral retráctil y mensajes.
(function () {
    'use strict';

    var root = document.documentElement;
    var toggle = document.querySelector('[data-sidebar-toggle]');
    var backdrop = document.querySelector('[data-sidebar-close]');
    var mobile = window.matchMedia('(max-width: 900px)');
    var STORAGE_KEY = 'inventiq.sidebar';

    function remember(state) {
        try {
            localStorage.setItem(STORAGE_KEY, state);
        } catch (e) {
            // Sin almacenamiento (modo privado o bloqueado): la barra funciona igual, sin recordar.
        }
    }

    function syncExpanded() {
        if (!toggle) {
            return;
        }
        var expanded = mobile.matches
            ? root.classList.contains('sidebar-open')
            : !root.classList.contains('sidebar-collapsed');
        toggle.setAttribute('aria-expanded', String(expanded));
    }

    if (toggle) {
        toggle.addEventListener('click', function () {
            if (mobile.matches) {
                root.classList.toggle('sidebar-open');
            } else {
                var collapsed = root.classList.toggle('sidebar-collapsed');
                remember(collapsed ? 'collapsed' : 'expanded');
            }
            syncExpanded();
        });
    }

    function closeMobileMenu() {
        root.classList.remove('sidebar-open');
        syncExpanded();
    }

    if (backdrop) {
        backdrop.addEventListener('click', closeMobileMenu);
    }

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && root.classList.contains('sidebar-open')) {
            closeMobileMenu();
        }
    });

    if (mobile.addEventListener) {
        mobile.addEventListener('change', function () {
            root.classList.remove('sidebar-open');
            syncExpanded();
        });
    }
    syncExpanded();

    // Botón para cerrar los mensajes de éxito o error.
    document.querySelectorAll('[data-flash-close]').forEach(function (button) {
        button.addEventListener('click', function () {
            var flash = button.closest('.flash');
            if (flash) {
                flash.remove();
            }
        });
    });
})();
