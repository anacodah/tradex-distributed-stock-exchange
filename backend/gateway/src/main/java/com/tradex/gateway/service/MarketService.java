package com.tradex.gateway.service;

import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.repository.StockRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MarketService {

    private final StockRepository stockRepository;

    public MarketService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Stock getStockBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Stock not found: " + symbol));
    }

    public List<Stock> searchStocks(String query) {
        return stockRepository.findBySymbolContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(query, query);
    }
}
