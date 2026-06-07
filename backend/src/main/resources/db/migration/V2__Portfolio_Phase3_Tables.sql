-- Phase 3 portfolio decision tables.
-- V1 grew during local development, so this migration makes existing databases catch up.

CREATE TABLE IF NOT EXISTS portfolio_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_name TEXT NOT NULL,
    account_type TEXT NOT NULL,
    base_currency TEXT NOT NULL DEFAULT 'CAD',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS portfolio_holdings (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES portfolio_accounts(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id),
    symbol TEXT NOT NULL,
    quantity NUMERIC(18, 6),
    avg_cost NUMERIC(18, 4),
    market_value_cad NUMERIC(18, 2),
    portfolio_weight NUMERIC(8, 4),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS investment_theses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    thesis_text TEXT NOT NULL,
    buy_reason TEXT,
    expected_time_horizon_months INT,
    buy_triggers JSONB,
    sell_triggers JSONB,
    invalidation_conditions JSONB,
    status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS stock_scores (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    fundamental_score INT,
    catalyst_score INT,
    estimate_revision_score INT,
    valuation_score INT,
    risk_score INT,
    momentum_score INT,
    source_quality_score INT,
    thesis_score INT,
    final_score INT,
    score_details JSONB,
    calculated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_stock_scores_company_id ON stock_scores(company_id);
CREATE INDEX IF NOT EXISTS idx_stock_scores_calculated_at ON stock_scores(calculated_at DESC);

CREATE TABLE IF NOT EXISTS recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id),
    action TEXT NOT NULL,
    confidence_score INT,
    reason TEXT NOT NULL,
    buy_trigger TEXT,
    sell_trigger TEXT,
    suggested_position_size_pct NUMERIC(6, 3),
    etf_comparison TEXT,
    risk_notes TEXT,
    source_event_ids JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recommendations_user_id ON recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_company_id ON recommendations(company_id);

CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_type TEXT NOT NULL,
    title TEXT NOT NULL,
    report_markdown TEXT NOT NULL,
    report_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reports_user_id ON reports(user_id);
CREATE INDEX IF NOT EXISTS idx_reports_created_at ON reports(created_at DESC);

CREATE TABLE IF NOT EXISTS app_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    setting_key TEXT NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, setting_key)
);
