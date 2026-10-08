package com.tradex.gateway.controller;

import com.tradex.gateway.entity.Holding;
import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.entity.Wallet;
import com.tradex.gateway.service.TradingService;
import com.tradex.gateway.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final TradingService tradingService;
    private final WalletService walletService;

    public PortfolioController(TradingService tradingService, WalletService walletService) {
        this.tradingService = tradingService;
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPortfolio(Authentication auth) {
        List<Holding> holdings = tradingService.getHoldings(auth.getName());
        Wallet wallet = walletService.getOrCreateWallet(auth.getName());

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;

        List<Map<String, Object>> holdingDtos = new ArrayList<>();
        for (Holding h : holdings) {
            Stock s = h.getStock();
            BigDecimal marketValue = s.getCurrentPrice().multiply(h.getQuantity()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal invested = h.getAveragePrice().multiply(h.getQuantity()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnl = marketValue.subtract(invested).setScale(4, RoundingMode.HALF_UP);
            BigDecimal unrealizedPct = invested.compareTo(BigDecimal.ZERO) != 0
                    ? unrealizedPnl.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            totalMarketValue = totalMarketValue.add(marketValue);
            totalInvested = totalInvested.add(invested);
            totalRealizedPnl = totalRealizedPnl.add(h.getRealizedPnl());

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("symbol", s.getSymbol());
            dto.put("companyName", s.getCompanyName());
            dto.put("quantity", h.getQuantity());
            dto.put("averagePrice", h.getAveragePrice());
            dto.put("currentPrice", s.getCurrentPrice());
            dto.put("marketValue", marketValue);
            dto.put("unrealizedPnl", unrealizedPnl);
            dto.put("unrealizedPnlPct", unrealizedPct);
            dto.put("realizedPnl", h.getRealizedPnl());
            holdingDtos.add(dto);
        }

        BigDecimal totalPortfolioValue = totalMarketValue.add(wallet.getAvailableBalance());
        BigDecimal totalUnrealizedPnl = totalMarketValue.subtract(totalInvested).setScale(4, RoundingMode.HALF_UP);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("holdings", holdingDtos);
        response.put("totalMarketValue", totalMarketValue);
        response.put("totalInvested", totalInvested);
        response.put("availableCash", wallet.getAvailableBalance());
        response.put("totalPortfolioValue", totalPortfolioValue);
        response.put("unrealizedPnl", totalUnrealizedPnl);
        response.put("realizedPnl", totalRealizedPnl);

        return ResponseEntity.ok(response);
    }
}
