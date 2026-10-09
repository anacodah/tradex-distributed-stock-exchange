package com.tradex.common.repository;

import com.tradex.common.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    List<MarketPrice> findTop50BySymbolOrderByTimestampDesc(String symbol);

    @Query("SELECT mp FROM MarketPrice mp WHERE mp.symbol = :symbol ORDER BY mp.timestamp DESC")
    List<MarketPrice> findLatestBySymbol(@Param("symbol") String symbol);

    @Query(value = "SELECT * FROM market_prices WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1",
           nativeQuery = true)
    Optional<MarketPrice> findMostRecentBySymbol(@Param("symbol") String symbol);
}
