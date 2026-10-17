-- Esquema inicial de InventIQ (tarea A1).
-- Derivado del modelo de la Entrega 1 y del reparto de la Entrega 2.
-- PENDIENTE: alinear nombres y columnas con ACUERDOS.md §3 antes de fusionar.
-- Una vez fusionada, esta migración no se edita: los cambios van en una migración nueva.

CREATE TABLE app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,          -- ADMIN | CASHIER
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE supplier (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    whatsapp        VARCHAR(20),
    lead_time_days  INT           NOT NULL,
    min_order_type  VARCHAR(10),                  -- AMOUNT | UNITS (MOQ)
    min_order_value DECIMAL(12,2),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_supplier_lead_time CHECK (lead_time_days >= 0)
);

CREATE TABLE product (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode       VARCHAR(50)   NOT NULL UNIQUE,
    name          VARCHAR(150)  NOT NULL,
    sale_price    DECIMAL(12,2) NOT NULL,
    cost          DECIMAL(12,2) NOT NULL,
    perishable    BOOLEAN       NOT NULL DEFAULT FALSE,
    supplier_id   BIGINT,
    safety_stock  INT           NOT NULL DEFAULT 0,
    reorder_point INT,                            -- calculado por el motor (FR-03)
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    version       BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id)
);

CREATE TABLE purchase_order (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id  BIGINT,                          -- NULL = pedido "por revisar" (US-01 esc. 2)
    status       VARCHAR(20)   NOT NULL,          -- DRAFT | APPROVED | SENT | PARTIALLY_RECEIVED | RECEIVED
    needs_review BOOLEAN       NOT NULL DEFAULT FALSE,
    total        DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL,
    approved_at  TIMESTAMP,
    sent_at      TIMESTAMP,
    received_at  TIMESTAMP,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id)
);

CREATE TABLE order_line (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT        NOT NULL,
    product_id        BIGINT        NOT NULL,
    quantity_ordered  INT           NOT NULL,
    quantity_received INT           NOT NULL DEFAULT 0,
    unit_cost         DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_line_order   FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id),
    CONSTRAINT fk_line_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_line_quantities CHECK (quantity_ordered > 0 AND quantity_received >= 0)
);

CREATE TABLE batch (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id         BIGINT    NOT NULL,
    quantity_received  INT       NOT NULL,
    quantity_available INT       NOT NULL,
    expiration_date    DATE,                      -- NULL para productos no perecederos
    received_at        TIMESTAMP NOT NULL,
    order_line_id      BIGINT,                    -- NULL si el lote no viene de un pedido
    version            BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_batch_line    FOREIGN KEY (order_line_id) REFERENCES order_line (id),
    CONSTRAINT ck_batch_available CHECK (quantity_available >= 0 AND quantity_available <= quantity_received)
);

-- Consulta FIFO: lotes disponibles de un producto, el que vence primero y luego el recibido primero.
CREATE INDEX ix_batch_fifo ON batch (product_id, expiration_date, received_at);

CREATE TABLE sale (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cashier_id BIGINT        NOT NULL,
    created_at TIMESTAMP     NOT NULL,
    total      DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) REFERENCES app_user (id)
);

-- Una línea por lote descontado (FifoAllocator).
CREATE TABLE sale_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id    BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    batch_id   BIGINT        NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_item_sale    FOREIGN KEY (sale_id) REFERENCES sale (id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_item_batch   FOREIGN KEY (batch_id) REFERENCES batch (id),
    CONSTRAINT ck_item_quantity CHECK (quantity > 0)
);

CREATE INDEX ix_sale_created_at ON sale (created_at);

-- Fila única con los parámetros operativos (FR-08).
CREATE TABLE settings (
    id                BIGINT PRIMARY KEY,
    weekly_budget     DECIMAL(12,2),
    sales_window_days INT NOT NULL DEFAULT 30,
    admin_whatsapp    VARCHAR(20),
    CONSTRAINT ck_settings_single_row CHECK (id = 1)
);

INSERT INTO settings (id, sales_window_days) VALUES (1, 30);

CREATE TABLE audit_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    action     VARCHAR(40)  NOT NULL,             -- SETTINGS_CHANGED, STOCK_ADJUSTED, ...
    details    VARCHAR(1000),
    username   VARCHAR(50),                       -- NULL si la decisión fue automática
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE alert (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    resolved   BOOLEAN      NOT NULL DEFAULT FALSE
);
