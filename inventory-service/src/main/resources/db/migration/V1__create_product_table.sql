CREATE TABLE IF NOT EXISTS t_product (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50)    NOT NULL UNIQUE,
    name        VARCHAR(150)   NOT NULL,
    description TEXT,
    category    VARCHAR(100),
    price       NUMERIC(12, 2) NOT NULL,
    stock       INTEGER,
    enabled     BOOLEAN
);