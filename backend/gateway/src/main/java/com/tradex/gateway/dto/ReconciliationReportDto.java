package com.tradex.gateway.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class ReconciliationReportDto {

    private boolean reconciled;
    private int positionsChecked;
    private int positionsRepaired;
    private List<Map<String, Object>> adjustments;
    private ZonedDateTime timestamp = ZonedDateTime.now();

    public boolean isReconciled() { return reconciled; }
    public void setReconciled(boolean reconciled) { this.reconciled = reconciled; }

    public int getPositionsChecked() { return positionsChecked; }
    public void setPositionsChecked(int positionsChecked) { this.positionsChecked = positionsChecked; }

    public int getPositionsRepaired() { return positionsRepaired; }
    public void setPositionsRepaired(int positionsRepaired) { this.positionsRepaired = positionsRepaired; }

    public List<Map<String, Object>> getAdjustments() { return adjustments; }
    public void setAdjustments(List<Map<String, Object>> adjustments) { this.adjustments = adjustments; }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
}
