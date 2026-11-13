-- V2: fila única de configuración (ACUERDOS §3, Settings).
-- SettingsRepository.get() (§6.7) no devuelve Optional, así que la fila tiene que existir siempre.
-- Días: valores por defecto de §3. Presupuesto en 0 y número de WhatsApp de ejemplo:
-- el administrador los cambia en la pantalla de parámetros (FR-08) antes de usar el reabastecimiento.
INSERT INTO settings (id, weekly_budget, sales_window_days, review_period_days,
                      expiry_review_horizon_days, admin_whatsapp_number)
VALUES (1, 0.00, 30, 7, 30, '+570000000000');