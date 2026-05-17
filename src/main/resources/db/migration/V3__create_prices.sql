CREATE TABLE price_histories (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    price NUMERIC(19, 4) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    source VARCHAR(100) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_price_histories_asset FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE CASCADE,
    CONSTRAINT ck_price_histories_price_positive CHECK (price > 0)
);

CREATE INDEX idx_price_histories_asset_timestamp
    ON price_histories (asset_id, timestamp);

CREATE TABLE latest_prices (
    asset_id BIGINT PRIMARY KEY,
    price NUMERIC(19, 4) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    source VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_latest_prices_asset FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE CASCADE,
    CONSTRAINT ck_latest_prices_price_positive CHECK (price > 0)
);
