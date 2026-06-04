-- Enable pgvector extension for embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    display_name TEXT,
    password_hash TEXT NOT NULL,
    base_currency TEXT NOT NULL DEFAULT 'CAD',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Companies table
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    ticker TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    cik TEXT,
    exchange TEXT DEFAULT 'TSX',
    country TEXT DEFAULT 'CA',
    sector TEXT,
    industry TEXT,
    currency TEXT DEFAULT 'CAD',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Themes (AI/Semi/Cybersecurity/etc)
CREATE TABLE themes (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Company-Theme association
CREATE TABLE company_themes (
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    PRIMARY KEY (company_id, theme_id)
);

-- Watchlist items
CREATE TABLE watchlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    priority TEXT NOT NULL DEFAULT 'normal',
    notes TEXT,
    added_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, company_id)
);

-- Data sources (SEC, Gmail, RSS, Manual, etc)
CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    source_type TEXT NOT NULL,
    name TEXT NOT NULL,
    base_url TEXT,
    trust_level TEXT NOT NULL DEFAULT 'medium',
    enabled BOOLEAN NOT NULL DEFAULT true,
    poll_interval_minutes INT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Raw documents (from SEC, emails, news, etc)
CREATE TABLE raw_documents (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id),
    source_id BIGINT REFERENCES sources(id),
    source_type TEXT NOT NULL,
    external_id TEXT,
    source_url TEXT,
    title TEXT,
    author TEXT,
    published_at TIMESTAMP,
    retrieved_at TIMESTAMP NOT NULL DEFAULT now(),
    content_hash TEXT NOT NULL UNIQUE,
    raw_text TEXT NOT NULL,
    metadata_json JSONB,
    processing_status TEXT NOT NULL DEFAULT 'new',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_raw_documents_company_id ON raw_documents(company_id);
CREATE INDEX idx_raw_documents_processing_status ON raw_documents(processing_status);
CREATE INDEX idx_raw_documents_content_hash ON raw_documents(content_hash);

-- Document chunks (for embedding)
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES raw_documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    token_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(document_id, chunk_index)
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);

-- Document embeddings (pgvector)
CREATE TABLE document_embeddings (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL REFERENCES document_chunks(id) ON DELETE CASCADE,
    embedding vector(1536),
    model_name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_embeddings_chunk_id ON document_embeddings(chunk_id);

-- Extracted investment events
CREATE TABLE extracted_events (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES raw_documents(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id),
    event_type TEXT NOT NULL,
    event_date DATE,
    summary TEXT NOT NULL,
    what_changed TEXT,
    bull_case JSONB,
    bear_case JSONB,
    risks JSONB,
    watch_items JSONB,
    bullish_score INT CHECK (bullish_score BETWEEN 0 AND 100),
    bearish_score INT CHECK (bearish_score BETWEEN 0 AND 100),
    source_quality_score INT CHECK (source_quality_score BETWEEN 0 AND 100),
    confidence_score INT CHECK (confidence_score BETWEEN 0 AND 100),
    requires_manual_review BOOLEAN DEFAULT false,
    manual_review_reason TEXT,
    extraction_model TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_extracted_events_company_id ON extracted_events(company_id);
CREATE INDEX idx_extracted_events_event_type ON extracted_events(event_type);
CREATE INDEX idx_extracted_events_created_at ON extracted_events(created_at DESC);

-- Event evidence snippets
CREATE TABLE event_evidence (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES extracted_events(id) ON DELETE CASCADE,
    evidence_text TEXT NOT NULL,
    evidence_type TEXT,
    source_location TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Portfolio accounts (TFSA, RRSP, non-registered)
CREATE TABLE portfolio_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_name TEXT NOT NULL,
    account_type TEXT NOT NULL,
    base_currency TEXT NOT NULL DEFAULT 'CAD',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Portfolio holdings
CREATE TABLE portfolio_holdings (
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

-- Investment theses
CREATE TABLE investment_theses (
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

-- Stock scores
CREATE TABLE stock_scores (
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

CREATE INDEX idx_stock_scores_company_id ON stock_scores(company_id);
CREATE INDEX idx_stock_scores_calculated_at ON stock_scores(calculated_at DESC);

-- Recommendations
CREATE TABLE recommendations (
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

CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);
CREATE INDEX idx_recommendations_company_id ON recommendations(company_id);

-- Reports (daily, weekly, action)
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_type TEXT NOT NULL,
    title TEXT NOT NULL,
    report_markdown TEXT NOT NULL,
    report_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_reports_user_id ON reports(user_id);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);

-- Job runs (for monitoring)
CREATE TABLE job_runs (
    id BIGSERIAL PRIMARY KEY,
    job_name TEXT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT now(),
    finished_at TIMESTAMP,
    status TEXT NOT NULL DEFAULT 'running',
    error_message TEXT,
    documents_found INT DEFAULT 0,
    documents_processed INT DEFAULT 0,
    ai_calls INT DEFAULT 0,
    estimated_cost_usd NUMERIC(12, 4),
    metadata_json JSONB
);

CREATE INDEX idx_job_runs_job_name ON job_runs(job_name);
CREATE INDEX idx_job_runs_started_at ON job_runs(started_at DESC);

-- App settings
CREATE TABLE app_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    setting_key TEXT NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, setting_key)
);

-- Insert sample themes
INSERT INTO themes (name, description) VALUES
('AI/ML', 'Artificial Intelligence and Machine Learning trends'),
('Semiconductors', 'Semiconductor manufacturing and design'),
('Cybersecurity', 'Cybersecurity and data protection'),
('Cloud Computing', 'Cloud infrastructure and services'),
('Biotech', 'Biotechnology and life sciences'),
('Energy', 'Energy sector including renewables'),
('Finance Tech', 'Financial technology and fintech')
ON CONFLICT DO NOTHING;

-- Insert sample sources
INSERT INTO sources (source_type, name, trust_level, enabled) VALUES
('SEC_FILING', 'SEC EDGAR', 'high', true),
('EARNINGS_CALL', 'Company Earnings Calls', 'high', true),
('NEWS_WIRE', 'Reuters/Bloomberg', 'high', true),
('RSS_FEED', 'Company IR RSS', 'medium', true),
('MANUAL_PASTE', 'Manual Document Paste', 'medium', true),
('EMAIL', 'Email Newsletters', 'medium', true),
('SOCIAL_MEDIA', 'Twitter/X', 'low', false)
ON CONFLICT DO NOTHING;
