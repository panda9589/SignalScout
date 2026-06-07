package com.scout.application.service;

import com.scout.application.dto.*;
import com.scout.domain.entity.Company;
import com.scout.domain.entity.ExtractedEvent;
import com.scout.domain.repository.CompanyRepository;
import com.scout.domain.repository.ExtractedEventRepository;
import com.scout.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final String DEFAULT_USER_EMAIL = "local@signalscout.dev";

    private final JdbcTemplate jdbcTemplate;
    private final CompanyRepository companyRepository;
    private final ExtractedEventRepository extractedEventRepository;
    private final ScoringService scoringService;

    @Transactional
    public List<PortfolioAccountDto> getAccounts() {
        Long userId = ensureDefaultUser();
        return jdbcTemplate.query("""
                select id, account_name, account_type, base_currency
                from portfolio_accounts
                where user_id = ?
                order by id
                """, (rs, rowNum) -> PortfolioAccountDto.builder()
                .id(rs.getLong("id"))
                .accountName(rs.getString("account_name"))
                .accountType(rs.getString("account_type"))
                .baseCurrency(rs.getString("base_currency"))
                .build(), userId);
    }

    @Transactional
    public PortfolioAccountDto createAccount(CreatePortfolioAccountRequest request) {
        Long userId = ensureDefaultUser();
        Long id = jdbcTemplate.queryForObject("""
                insert into portfolio_accounts (user_id, account_name, account_type, base_currency)
                values (?, ?, ?, ?)
                returning id
                """, Long.class, userId, request.getAccountName(), request.getAccountType(),
                blankToDefault(request.getBaseCurrency(), "CAD"));

        return getAccounts().stream()
                .filter(account -> account.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public PortfolioAccountDto updateAccount(Long accountId, CreatePortfolioAccountRequest request) {
        Long userId = ensureDefaultUser();
        int updated = jdbcTemplate.update("""
                update portfolio_accounts
                set account_name = ?, account_type = ?, base_currency = ?
                where id = ? and user_id = ?
                """,
                request.getAccountName(),
                request.getAccountType(),
                blankToDefault(request.getBaseCurrency(), "CAD"),
                accountId,
                userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Portfolio account not found: " + accountId);
        }
        return getAccounts().stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        Long userId = ensureDefaultUser();
        int deleted = jdbcTemplate.update(
                "delete from portfolio_accounts where id = ? and user_id = ?",
                accountId,
                userId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Portfolio account not found: " + accountId);
        }
        recalculateWeights(userId);
    }

    @Transactional
    public List<PortfolioHoldingDto> getHoldings() {
        Long userId = ensureDefaultUser();
        List<PortfolioHoldingDto> holdings = jdbcTemplate.query("""
                select h.id, h.account_id, a.account_name, h.company_id, c.ticker, c.name as company_name,
                       h.symbol, h.quantity, h.avg_cost, h.market_value_cad, h.portfolio_weight
                from portfolio_holdings h
                join portfolio_accounts a on a.id = h.account_id
                left join companies c on c.id = h.company_id
                where a.user_id = ?
                order by a.id, h.symbol
                """, this::mapHolding, userId);

        BigDecimal total = totalMarketValue(holdings);
        return holdings.stream()
                .map(holding -> withComputedWeight(holding, total))
                .toList();
    }

    @Transactional
    public PortfolioHoldingDto createHolding(UpsertPortfolioHoldingRequest request) {
        Long userId = ensureDefaultUser();
        assertAccountBelongsToUser(request.getAccountId(), userId);

        Long companyId = request.getCompanyId();
        if (companyId == null) {
            companyId = companyRepository.findByTicker(request.getSymbol().trim().toUpperCase())
                    .map(Company::getId)
                    .orElse(null);
        }

        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found: " + companyId);
        }

        Long id = jdbcTemplate.queryForObject("""
                insert into portfolio_holdings
                    (account_id, company_id, symbol, quantity, avg_cost, market_value_cad)
                values (?, ?, ?, ?, ?, ?)
                returning id
                """, Long.class,
                request.getAccountId(),
                companyId,
                request.getSymbol().trim().toUpperCase(),
                request.getQuantity(),
                request.getAvgCost(),
                request.getMarketValueCad());

        recalculateWeights();
        return getHoldings().stream()
                .filter(holding -> holding.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public PortfolioHoldingDto updateHolding(Long holdingId, UpsertPortfolioHoldingRequest request) {
        Long userId = ensureDefaultUser();
        assertAccountBelongsToUser(request.getAccountId(), userId);
        assertHoldingBelongsToUser(holdingId, userId);

        Long companyId = resolveCompanyId(request.getCompanyId(), request.getSymbol());
        int updated = jdbcTemplate.update("""
                update portfolio_holdings
                set account_id = ?, company_id = ?, symbol = ?, quantity = ?, avg_cost = ?,
                    market_value_cad = ?, updated_at = now()
                where id = ?
                """,
                request.getAccountId(),
                companyId,
                request.getSymbol().trim().toUpperCase(),
                request.getQuantity(),
                request.getAvgCost(),
                request.getMarketValueCad(),
                holdingId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Portfolio holding not found: " + holdingId);
        }

        recalculateWeights(userId);
        return getHoldings().stream()
                .filter(holding -> holding.getId().equals(holdingId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void deleteHolding(Long holdingId) {
        Long userId = ensureDefaultUser();
        assertHoldingBelongsToUser(holdingId, userId);
        jdbcTemplate.update("delete from portfolio_holdings where id = ?", holdingId);
        recalculateWeights(userId);
    }

    @Transactional
    public List<InvestmentThesisDto> getTheses() {
        Long userId = ensureDefaultUser();
        return jdbcTemplate.query("""
                select t.id, t.company_id, c.ticker, c.name as company_name, t.thesis_text,
                       t.buy_reason, t.expected_time_horizon_months, t.status
                from investment_theses t
                join companies c on c.id = t.company_id
                where t.user_id = ?
                order by c.ticker
                """, this::mapThesis, userId);
    }

    @Transactional
    public InvestmentThesisDto createThesis(UpsertInvestmentThesisRequest request) {
        Long userId = ensureDefaultUser();
        if (!companyRepository.existsById(request.getCompanyId())) {
            throw new ResourceNotFoundException("Company not found: " + request.getCompanyId());
        }

        Long id = jdbcTemplate.queryForObject("""
                insert into investment_theses
                    (user_id, company_id, thesis_text, buy_reason, expected_time_horizon_months, status)
                values (?, ?, ?, ?, ?, ?)
                returning id
                """, Long.class,
                userId,
                request.getCompanyId(),
                request.getThesisText(),
                request.getBuyReason(),
                request.getExpectedTimeHorizonMonths(),
                normalizeStatus(request.getStatus()));

        return getTheses().stream()
                .filter(thesis -> thesis.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public InvestmentThesisDto updateThesis(Long thesisId, UpsertInvestmentThesisRequest request) {
        Long userId = ensureDefaultUser();
        if (!companyRepository.existsById(request.getCompanyId())) {
            throw new ResourceNotFoundException("Company not found: " + request.getCompanyId());
        }

        int updated = jdbcTemplate.update("""
                update investment_theses
                set company_id = ?, thesis_text = ?, buy_reason = ?,
                    expected_time_horizon_months = ?, status = ?, updated_at = now()
                where id = ? and user_id = ?
                """,
                request.getCompanyId(),
                request.getThesisText(),
                request.getBuyReason(),
                request.getExpectedTimeHorizonMonths(),
                normalizeStatus(request.getStatus()),
                thesisId,
                userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Investment thesis not found: " + thesisId);
        }

        return getTheses().stream()
                .filter(thesis -> thesis.getId().equals(thesisId))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void deleteThesis(Long thesisId) {
        Long userId = ensureDefaultUser();
        int deleted = jdbcTemplate.update(
                "delete from investment_theses where id = ? and user_id = ?",
                thesisId,
                userId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Investment thesis not found: " + thesisId);
        }
    }

    @Transactional
    public RiskSettingsDto getRiskSettings() {
        Long userId = ensureDefaultUser();
        return RiskSettingsDto.builder()
                .maxSingleStockPositionPct(settingDecimal(userId, "max_single_stock_position_pct", "10"))
                .maxSectorExposurePct(settingDecimal(userId, "max_sector_exposure_pct", "35"))
                .minScoreForNewBuy(settingInt(userId, "min_score_for_new_buy", "80"))
                .minSourceQualityForNewBuy(settingInt(userId, "min_source_quality_for_new_buy", "70"))
                .build();
    }

    @Transactional
    public RiskSettingsDto updateRiskSettings(RiskSettingsDto request) {
        Long userId = ensureDefaultUser();
        upsertSetting(userId, "max_single_stock_position_pct", request.getMaxSingleStockPositionPct());
        upsertSetting(userId, "max_sector_exposure_pct", request.getMaxSectorExposurePct());
        upsertSetting(userId, "min_score_for_new_buy", request.getMinScoreForNewBuy());
        upsertSetting(userId, "min_source_quality_for_new_buy", request.getMinSourceQualityForNewBuy());
        return getRiskSettings();
    }

    @Transactional
    public PortfolioActionReportResponse generateActionReport() {
        Long userId = ensureDefaultUser();
        RiskSettingsDto settings = getRiskSettings();
        List<PortfolioHoldingDto> holdings = getHoldings();
        BigDecimal total = totalMarketValue(holdings);
        Map<Long, PortfolioHoldingDto> holdingByCompany = holdings.stream()
                .filter(holding -> holding.getCompanyId() != null)
                .collect(Collectors.toMap(PortfolioHoldingDto::getCompanyId, holding -> holding, (a, b) -> a));
        Map<String, BigDecimal> sectorExposure = sectorExposure(holdings);
        Set<Long> activeThesisCompanyIds = activeThesisCompanyIds(userId);

        List<PortfolioActionDto> actions = companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getTicker))
                .map(company -> buildAction(company, holdingByCompany.get(company.getId()),
                        sectorExposure.getOrDefault(company.getSector(), BigDecimal.ZERO),
                        activeThesisCompanyIds.contains(company.getId()),
                        settings))
                .toList();

        persistRecommendations(userId, actions);

        return PortfolioActionReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalMarketValueCad(total)
                .riskSettings(settings)
                .actions(actions)
                .build();
    }

    private PortfolioActionDto buildAction(
            Company company,
            PortfolioHoldingDto holding,
            BigDecimal sectorExposure,
            boolean hasActiveThesis,
            RiskSettingsDto settings) {

        ExtractedEvent latestEvent = latestEvent(company.getId()).orElse(null);
        Integer score = latestEvent != null ? scoringService.computeStockScore(latestEvent) : null;
        String baseRecommendation = scoringService.generateRecommendation(score);
        Integer sourceQuality = latestEvent != null ? latestEvent.getSourceQualityScore() : null;
        BigDecimal currentWeight = holding != null ? nullToZero(holding.getPortfolioWeight()) : BigDecimal.ZERO;
        boolean hasHolding = holding != null;
        boolean sourceQualityClears = sourceQuality != null
                && sourceQuality >= settings.getMinSourceQualityForNewBuy();

        String action;
        String reason;

        if (score == null) {
            action = hasHolding ? "HOLD" : "WATCH";
            reason = "No extracted investment event yet; collect more evidence before acting.";
        } else if (!hasHolding && !sourceQualityClears && score >= settings.getMinScoreForNewBuy()) {
            action = "WATCH";
            reason = "Score is constructive, but source quality is below the configured new-buy threshold.";
        } else if (hasHolding && currentWeight.compareTo(settings.getMaxSingleStockPositionPct()) > 0) {
            action = "TRIM";
            reason = "Position is above the configured max single-stock limit.";
        } else if (hasHolding && score < 40) {
            action = "SELL";
            reason = "Latest score is weak and the position is already owned.";
        } else if (!hasHolding && score >= settings.getMinScoreForNewBuy()
                && sourceQualityClears
                && sectorExposure.compareTo(settings.getMaxSectorExposurePct()) < 0
                && hasActiveThesis) {
            action = "BUY";
            reason = "Score clears the new-buy threshold, sector exposure allows it, and an active thesis exists.";
        } else if (hasHolding && score >= 75
                && sectorExposure.compareTo(settings.getMaxSectorExposurePct()) < 0) {
            action = "ADD";
            reason = "Owned position has a strong score and sector exposure is below limit.";
        } else if (hasHolding) {
            action = "HOLD";
            reason = "Keep monitoring; current evidence does not force an add, trim, or sell.";
        } else if (score >= 60) {
            action = "WATCH";
            reason = hasActiveThesis
                    ? "Evidence is constructive but does not clear new-buy rules."
                    : "Evidence is constructive; add a thesis before a new buy.";
        } else {
            action = "AVOID";
            reason = "Latest score is below the watch threshold.";
        }

        return PortfolioActionDto.builder()
                .companyId(company.getId())
                .ticker(company.getTicker())
                .companyName(company.getName())
                .sector(company.getSector())
                .action(action)
                .stockScore(score)
                .sourceQualityScore(sourceQuality)
                .latestRecommendation(baseRecommendation)
                .currentWeightPct(currentWeight)
                .sectorExposurePct(sectorExposure)
                .hasHolding(hasHolding)
                .hasActiveThesis(hasActiveThesis)
                .reason(reason)
                .buyTrigger("Score clears threshold, source quality is high, and position/sector limits allow it.")
                .sellTrigger("Score falls below 40, thesis is invalidated, or source quality deteriorates.")
                .riskNotes("Check position size, sector concentration, source quality, and thesis status before trading.")
                .build();
    }

    private void persistRecommendations(Long userId, List<PortfolioActionDto> actions) {
        for (PortfolioActionDto action : actions) {
            jdbcTemplate.update("""
                    insert into recommendations
                        (user_id, company_id, action, confidence_score, reason, buy_trigger,
                         sell_trigger, suggested_position_size_pct, risk_notes)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    userId,
                    action.getCompanyId(),
                    action.getAction(),
                    action.getStockScore(),
                    action.getReason(),
                    action.getBuyTrigger(),
                    action.getSellTrigger(),
                    suggestedPositionSize(action),
                    action.getRiskNotes());
        }
    }

    private BigDecimal suggestedPositionSize(PortfolioActionDto action) {
        if ("BUY".equals(action.getAction())) {
            return BigDecimal.valueOf(3);
        }
        if ("ADD".equals(action.getAction())) {
            return BigDecimal.valueOf(1);
        }
        if ("TRIM".equals(action.getAction())) {
            return action.getCurrentWeightPct();
        }
        return BigDecimal.ZERO;
    }

    private Optional<ExtractedEvent> latestEvent(Long companyId) {
        List<ExtractedEvent> events = extractedEventRepository.findByCompanyId(
                companyId,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    private Long ensureDefaultUser() {
        List<Long> ids = jdbcTemplate.query(
                "select id from users where email = ? limit 1",
                (rs, rowNum) -> rs.getLong("id"),
                DEFAULT_USER_EMAIL);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        return jdbcTemplate.queryForObject("""
                insert into users (email, display_name, password_hash, base_currency)
                values (?, 'Local User', 'local-dev-no-login', 'CAD')
                returning id
                """, Long.class, DEFAULT_USER_EMAIL);
    }

    private void assertAccountBelongsToUser(Long accountId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from portfolio_accounts where id = ? and user_id = ?",
                Integer.class,
                accountId,
                userId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Portfolio account not found: " + accountId);
        }
    }

    private void assertHoldingBelongsToUser(Long holdingId, Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from portfolio_holdings h
                join portfolio_accounts a on a.id = h.account_id
                where h.id = ? and a.user_id = ?
                """, Integer.class, holdingId, userId);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("Portfolio holding not found: " + holdingId);
        }
    }

    private void recalculateWeights() {
        recalculateWeights(null);
    }

    private void recalculateWeights(Long userId) {
        String userFilter = userId == null ? "" : " where a.user_id = ?";
        BigDecimal total = jdbcTemplate.queryForObject(
                "select coalesce(sum(h.market_value_cad), 0) from portfolio_holdings h join portfolio_accounts a on a.id = h.account_id" + userFilter,
                BigDecimal.class,
                userId == null ? new Object[]{} : new Object[]{userId});
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            if (userId == null) {
                jdbcTemplate.update("update portfolio_holdings set portfolio_weight = 0");
            } else {
                jdbcTemplate.update("""
                        update portfolio_holdings h
                        set portfolio_weight = 0
                        from portfolio_accounts a
                        where a.id = h.account_id and a.user_id = ?
                        """, userId);
            }
            return;
        }

        if (userId == null) {
            jdbcTemplate.update("""
                    update portfolio_holdings
                    set portfolio_weight = round(((market_value_cad / ?) * 100)::numeric, 4)
                    where market_value_cad is not null
                    """, total);
        } else {
            jdbcTemplate.update("""
                    update portfolio_holdings h
                    set portfolio_weight = round(((h.market_value_cad / ?) * 100)::numeric, 4)
                    from portfolio_accounts a
                    where a.id = h.account_id and a.user_id = ? and h.market_value_cad is not null
                    """, total, userId);
        }
    }

    private PortfolioHoldingDto mapHolding(ResultSet rs, int rowNum) throws SQLException {
        return PortfolioHoldingDto.builder()
                .id(rs.getLong("id"))
                .accountId(rs.getLong("account_id"))
                .accountName(rs.getString("account_name"))
                .companyId(nullableLong(rs, "company_id"))
                .ticker(rs.getString("ticker"))
                .companyName(rs.getString("company_name"))
                .symbol(rs.getString("symbol"))
                .quantity(rs.getBigDecimal("quantity"))
                .avgCost(rs.getBigDecimal("avg_cost"))
                .marketValueCad(rs.getBigDecimal("market_value_cad"))
                .portfolioWeight(rs.getBigDecimal("portfolio_weight"))
                .build();
    }

    private InvestmentThesisDto mapThesis(ResultSet rs, int rowNum) throws SQLException {
        return InvestmentThesisDto.builder()
                .id(rs.getLong("id"))
                .companyId(rs.getLong("company_id"))
                .ticker(rs.getString("ticker"))
                .companyName(rs.getString("company_name"))
                .thesisText(rs.getString("thesis_text"))
                .buyReason(rs.getString("buy_reason"))
                .expectedTimeHorizonMonths((Integer) rs.getObject("expected_time_horizon_months"))
                .status(rs.getString("status"))
                .build();
    }

    private Set<Long> activeThesisCompanyIds(Long userId) {
        return new HashSet<>(jdbcTemplate.query("""
                select company_id
                from investment_theses
                where user_id = ? and status = 'active'
                """, (rs, rowNum) -> rs.getLong("company_id"), userId));
    }

    private Map<String, BigDecimal> sectorExposure(List<PortfolioHoldingDto> holdings) {
        Map<Long, String> companySector = companyRepository.findAll().stream()
                .collect(Collectors.toMap(Company::getId, Company::getSector));
        Map<String, BigDecimal> exposure = new HashMap<>();
        for (PortfolioHoldingDto holding : holdings) {
            if (holding.getCompanyId() == null) {
                continue;
            }
            String sector = companySector.getOrDefault(holding.getCompanyId(), "Unknown");
            exposure.merge(sector, nullToZero(holding.getPortfolioWeight()), BigDecimal::add);
        }
        return exposure;
    }

    private BigDecimal totalMarketValue(List<PortfolioHoldingDto> holdings) {
        return holdings.stream()
                .map(PortfolioHoldingDto::getMarketValueCad)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PortfolioHoldingDto withComputedWeight(PortfolioHoldingDto holding, BigDecimal total) {
        if (holding.getMarketValueCad() == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            holding.setPortfolioWeight(BigDecimal.ZERO);
            return holding;
        }
        holding.setPortfolioWeight(holding.getMarketValueCad()
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 4, RoundingMode.HALF_UP));
        return holding;
    }

    private BigDecimal settingDecimal(Long userId, String key, String defaultValue) {
        return new BigDecimal(settingValue(userId, key, defaultValue));
    }

    private Integer settingInt(Long userId, String key, String defaultValue) {
        return Integer.valueOf(settingValue(userId, key, defaultValue));
    }

    private String settingValue(Long userId, String key, String defaultValue) {
        List<String> values = jdbcTemplate.query(
                "select setting_value from app_settings where user_id = ? and setting_key = ?",
                (rs, rowNum) -> rs.getString("setting_value"),
                userId,
                key);
        return values.isEmpty() || values.get(0) == null ? defaultValue : values.get(0);
    }

    private void upsertSetting(Long userId, String key, Object value) {
        if (value == null) {
            return;
        }
        jdbcTemplate.update("""
                insert into app_settings (user_id, setting_key, setting_value)
                values (?, ?, ?)
                on conflict (user_id, setting_key)
                do update set setting_value = excluded.setting_value, updated_at = now()
                """, userId, key, value.toString());
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Long resolveCompanyId(Long requestedCompanyId, String symbol) {
        Long companyId = requestedCompanyId;
        if (companyId == null) {
            companyId = companyRepository.findByTicker(symbol.trim().toUpperCase())
                    .map(Company::getId)
                    .orElse(null);
        }

        if (companyId != null && !companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company not found: " + companyId);
        }
        return companyId;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalizeStatus(String status) {
        return blankToDefault(status, "active").trim().toLowerCase();
    }
}
