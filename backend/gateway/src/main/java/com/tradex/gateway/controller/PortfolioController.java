package com.tradex.gateway.controller;

import com.tradex.gateway.dto.HoldingDto;
import com.tradex.gateway.dto.PortfolioSummaryDto;
import com.tradex.gateway.dto.ReconciliationReportDto;
import com.tradex.gateway.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Portfolio and Performance Analytics API.
 *
 * Endpoints:
 * - GET  /api/portfolio            Full portfolio overview & analytics
 * - GET  /api/portfolio/holdings   Holdings breakdown with sellable & reserved quantities
 * - POST /api/portfolio/reconcile  Authoritative position reconciliation from trades
 */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<PortfolioSummaryDto> getPortfolio(Authentication auth) {
        return ResponseEntity.ok(portfolioService.getPortfolioSummary(auth.getName()));
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingDto>> getHoldings(Authentication auth) {
        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary(auth.getName());
        return ResponseEntity.ok(summary.getHoldings());
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ReconciliationReportDto> reconcilePositions(Authentication auth) {
        return ResponseEntity.ok(portfolioService.reconcilePositions(auth.getName()));
    }
}
