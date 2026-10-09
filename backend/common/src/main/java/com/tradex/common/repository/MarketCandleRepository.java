package com.tradex.common.repository;

import com.tradex.common.entity.MarketCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketCandleRepository extends JpaRepository<MarketCandle, Long> {

    List<MarketCandle> findBySymbolAndTimeframeOrderByBucketStartDesc(String symbol, String timeframe);

    List<MarketCandle> findBySymbolAndTimeframeOrderByBucketStartAsc(String symbol, String timeframe);

    @Query("SELECT mc FROM MarketCandle mc WHERE mc.symbol = :symbol AND mc.timeframe = :timeframe " +
           "AND mc.bucketStart >= :from ORDER BY mc.bucketStart ASC")
    List<MarketCandle> findBySymbolTimeframeFrom(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("from") ZonedDateTime from);

    @Query("SELECT mc FROM MarketCandle mc WHERE mc.symbol = :symbol AND mc.timeframe = :timeframe " +
           "AND mc.bucketStart >= :from AND mc.bucketStart <= :to ORDER BY mc.bucketStart ASC")
    List<MarketCandle> findBySymbolTimeframeRange(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("from") ZonedDateTime from,
            @Param("to") ZonedDateTime to);

    Optional<MarketCandle> findByStockIdAndTimeframeAndBucketStart(Long stockId, String timeframe, ZonedDateTime bucketStart);

    @Query("SELECT mc FROM MarketCandle mc WHERE mc.symbol = :symbol AND mc.timeframe = :timeframe " +
           "ORDER BY mc.bucketStart DESC")
    List<MarketCandle> findLatestBySymbolAndTimeframe(
            @Param("symbol") String symbol, @Param("timeframe") String timeframe);
}

