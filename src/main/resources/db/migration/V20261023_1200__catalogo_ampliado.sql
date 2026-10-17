-- Catálogo ampliado: tipo, marca, contenido neto, descripción y venta pausada, más el historial
-- de precios de compra (costo) y de venta.
-- Un producto con la venta pausada sigue en el inventario con su stock, pero el POS no lo vende.

ALTER TABLE product ADD COLUMN category VARCHAR(30) DEFAULT 'OTROS' NOT NULL;
ALTER TABLE product ADD COLUMN brand VARCHAR(80);
ALTER TABLE product ADD COLUMN net_content DECIMAL(10,2);
ALTER TABLE product ADD COLUMN content_unit VARCHAR(5);       -- G | KG | ML | L | UND
ALTER TABLE product ADD COLUMN description VARCHAR(500);
ALTER TABLE product ADD COLUMN sale_enabled BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE product ADD CONSTRAINT ck_product_net_content
    CHECK ((net_content IS NULL AND content_unit IS NULL) OR (net_content > 0 AND content_unit IS NOT NULL));

-- Cada fila es el precio de venta y el costo vigentes desde changed_at hasta el siguiente cambio
-- del mismo producto.
CREATE TABLE price_history (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT        NOT NULL,
    sale_price DECIMAL(12,2) NOT NULL,
    cost       DECIMAL(12,2) NOT NULL,
    changed_at TIMESTAMP     NOT NULL,
    CONSTRAINT fk_price_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE INDEX ix_price_history_product ON price_history (product_id, changed_at);

-- Los productos que ya existían empiezan su historial con el precio actual.
INSERT INTO price_history (product_id, sale_price, cost, changed_at)
SELECT id, sale_price, cost, CURRENT_TIMESTAMP FROM product;
