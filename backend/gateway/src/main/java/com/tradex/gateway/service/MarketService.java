package com.tradex.gateway.service;

import com.tradex.common.entity.Company;
import com.tradex.common.entity.MarketCandle;
import com.tradex.common.repository.CompanyRepository;
import com.tradex.common.repository.MarketCandleRepository;
import com.tradex.common.repository.MarketPriceRepository;
import com.tradex.gateway.dto.CompanyDto;
import com.tradex.gateway.dto.MarketStatsDto;
import com.tradex.gateway.dto.OhlcvCandleDto;
import com.tradex.gateway.dto.PriceSnapshotDto;
import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Market data read service — all methods read from the database.
 * No prices are generated here. Price evolution is exclusively handled
 * by {@link com.tradex.gateway.marketdata.MarketDataEngine}.
 */
@Service
public class MarketService {

    private static final Logger log = LoggerFactory.getLogger(MarketService.class);

    private final StockRepository stockRepository;
    private final CompanyRepository companyRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final MarketPriceRepository marketPriceRepository;

    public MarketService(StockRepository stockRepository,
                         CompanyRepository companyRepository,
                         MarketCandleRepository marketCandleRepository,
                         MarketPriceRepository marketPriceRepository) {
        this.stockRepository = stockRepository;
        this.companyRepository = companyRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.marketPriceRepository = marketPriceRepository;
    }

    // -------------------------------------------------------------------------
    // Stock / instrument queries (raw entity — for order routing etc.)
    // -------------------------------------------------------------------------

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Stock getStockBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found: " + symbol));
    }

    public List<Stock> searchStocks(String query) {
        return stockRepository.findBySymbolContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(query, query);
    }

    // -------------------------------------------------------------------------
    // Price snapshots — read from DB, never generated on demand
    // -------------------------------------------------------------------------

    /**
     * Returns the latest price snapshot for all instruments.
     * Reads directly from the stocks table which is updated by the engine every 5 s.
     * Session OHLC and the latest intraday price are kept strictly separate.
     */
    public List<PriceSnapshotDto> getAllSnapshots() {
        return stockRepository.findAll().stream()
                .map(this::toSnapshot)
                .collect(Collectors.toList());
    }

    /**
     * Latest price snapshot for one instrument.
     */
    public PriceSnapshotDto getSnapshot(String symbol) {
        Stock stock = getStockBySymbol(symbol);
        return toSnapshot(stock);
    }

    private PriceSnapshotDto toSnapshot(Stock stock) {
        BigDecimal changeAmount = stock.getCurrentPrice()
                .subtract(stock.getPreviousClose())
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal changePercent = stock.getPreviousClose().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : changeAmount.divide(stock.getPreviousClose(), 6, RoundingMode.HALF_UP)
                      .multiply(BigDecimal.valueOf(100))
                      .setScale(4, RoundingMode.HALF_UP);

        return new PriceSnapshotDto(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getSector(),
                stock.getExchange(),
                stock.getCurrency(),
                stock.getDataSource() != null ? stock.getDataSource() : "SIMULATED",
                stock.getCurrentPrice(),
                stock.getPriceTimestamp(),
                stock.getOpenPrice(),
                stock.getHighPrice(),
                stock.getLowPrice(),
                stock.getPreviousClose(),
                changeAmount,
                changePercent,
                stock.getVolume(),
                stock.getMarketCap()
        );
    }

    // -------------------------------------------------------------------------
    // Company / instrument metadata — stored separately from price data
    // -------------------------------------------------------------------------

    /**
     * Search companies by symbol or name fragment.
     */
    public List<CompanyDto> searchCompanies(String query) {
        return companyRepository.searchBySymbolOrName(query).stream()
                .map(this::toCompanyDto)
                .collect(Collectors.toList());
    }

    /**
     * Company profile for a single symbol.
     */
    public CompanyDto getCompanyProfile(String symbol) {
        Company company = companyRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Company not found: " + symbol));
        return toCompanyDto(company);
    }

    /**
     * All companies in a given sector.
     */
    public List<CompanyDto> getCompaniesBySector(String sector) {
        return companyRepository.findBySectorOrderBySymbolAsc(sector).stream()
                .map(this::toCompanyDto)
                .collect(Collectors.toList());
    }

    /**
     * All distinct sectors present in the database.
     */
    public List<String> getAllSectors() {
        return companyRepository.findAllSectors();
    }

    private CompanyDto toCompanyDto(Company c) {
        return new CompanyDto(
                c.getSymbol(),
                c.getName(),
                c.getSector(),
                c.getIndustry(),
                c.getDescription(),
                c.getWebsite(),
                c.getCountry(),
                c.getExchange(),
                c.getCurrency(),
                c.getMarketCap(),
                c.getEmployees()
        );
    }

    // -------------------------------------------------------------------------
    // OHLCV candles — read from market_candles table
    // Timeframes: "1M", "1H", "1D"
    // -------------------------------------------------------------------------

    /**
     * Historical OHLCV candles for a symbol at the given timeframe.
     * Optional {@code from}/{@code to} window to narrow results.
     *
     * <p><strong>Important semantics:</strong>
     * <ul>
     *   <li>1D candles contain session OHLCV (one entry per trading day).</li>
     *   <li>1M / 1H candles contain intraday OHLCV — never mix with daily candles.</li>
     * </ul>
     */
    public List<OhlcvCandleDto> getCandles(String symbol, String timeframe,
                                            ZonedDateTime from, ZonedDateTime to) {
        String sym = symbol.toUpperCase();
        String tf  = timeframe.toUpperCase();

        List<MarketCandle> candles;
        if (from != null && to != null) {
            candles = marketCandleRepository.findBySymbolTimeframeRange(sym, tf, from, to);
        } else if (from != null) {
            candles = marketCandleRepository.findBySymbolTimeframeFrom(sym, tf, from);
        } else {
            candles = marketCandleRepository.findBySymbolAndTimeframeOrderByBucketStartAsc(sym, tf);
        }

        return candles.stream().map(this::toCandleDto).collect(Collectors.toList());
    }

    /**
     * Convenience: latest N intraday 1-hour candles for quick chart rendering.
     */
    public List<OhlcvCandleDto> getRecentCandles(String symbol, String timeframe, int limit) {
        String sym = symbol.toUpperCase();
        String tf  = timeframe.toUpperCase();
        ZonedDateTime cutoff = ZonedDateTime.now(ZoneId.of("America/New_York"))
                .minus(limit, ChronoUnit.HOURS);
        List<MarketCandle> candles = marketCandleRepository
                .findBySymbolTimeframeFrom(sym, tf, cutoff);
        return candles.stream().map(this::toCandleDto).collect(Collectors.toList());
    }

    private OhlcvCandleDto toCandleDto(MarketCandle c) {
        return new OhlcvCandleDto(
                c.getSymbol(),
                c.getTimeframe(),
                c.getBucketStart(),
                c.getOpen(),
                c.getHigh(),
                c.getLow(),
                c.getClose(),
                c.getVolume(),
                c.getDataSource() != null ? c.getDataSource() : "SIMULATED"
        );
    }

    // -------------------------------------------------------------------------
    // Market-wide statistics — derived from persisted data
    // -------------------------------------------------------------------------

    /**
     * Compute market-wide advancers/decliners/unchanged and per-sector roll-ups.
     * All numbers derived from the latest persisted stock state — no generation.
     */
    public MarketStatsDto getMarketStats() {
        List<Stock> stocks = stockRepository.findAll();

        long advancers  = stocks.stream().filter(s -> s.getCurrentPrice().compareTo(s.getPreviousClose()) > 0).count();
        long decliners  = stocks.stream().filter(s -> s.getCurrentPrice().compareTo(s.getPreviousClose()) < 0).count();
        long unchanged  = stocks.stream().filter(s -> s.getCurrentPrice().compareTo(s.getPreviousClose()) == 0).count();

        BigDecimal totalMarketCap = stocks.stream()
                .map(s -> s.getMarketCap() != null ? s.getMarketCap() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by sector for per-sector stats
        Map<String, List<Stock>> bySector = stocks.stream()
                .filter(s -> s.getSector() != null && !s.getSector().isBlank())
                .collect(Collectors.groupingBy(Stock::getSector));

        List<MarketStatsDto.SectorStat> sectorStats = bySector.entrySet().stream()
                .map(e -> {
                    List<Stock> ss = e.getValue();
                    BigDecimal avgChg = ss.stream()
                            .map(s -> {
                                if (s.getPreviousClose().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
                                return s.getCurrentPrice().subtract(s.getPreviousClose())
                                        .divide(s.getPreviousClose(), 6, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(2, RoundingMode.HALF_UP);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(ss.size()), 2, RoundingMode.HALF_UP);
                    return new MarketStatsDto.SectorStat(e.getKey(), ss.size(), avgChg);
                })
                .sorted(Comparator.comparing(MarketStatsDto.SectorStat::sector))
                .collect(Collectors.toList());

        String dataSource = stocks.stream()
                .map(Stock::getDataSource)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("SIMULATED");

        return new MarketStatsDto(
                stocks.size(),
                advancers,
                decliners,
                unchanged,
                totalMarketCap,
                dataSource,
                sectorStats
        );
    }
}
