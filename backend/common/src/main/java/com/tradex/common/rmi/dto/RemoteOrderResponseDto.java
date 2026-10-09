package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RemoteOrderResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String correlationId;
    private Long orderId;
    private String status;
    private BigDecimal filledQuantity;
    private BigDecimal remainingQuantity;
    private BigDecimal executionPrice;
    private String matchedByNode;
    private long executionTimestamp;
    private boolean successful;
    private String errorMessage;
    private List<RemoteTradeDto> trades = new ArrayList<>();

    public RemoteOrderResponseDto() {}

    public static class RemoteTradeDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String tradeId;
        private Long buyerUserId;
        private Long sellerUserId;
        private Long makerOrderId;
        private Long takerOrderId;
        private BigDecimal price;
        private BigDecimal quantity;
        private BigDecimal totalValue;

        public RemoteTradeDto() {}

        public String getTradeId() { return tradeId; }
        public void setTradeId(String tradeId) { this.tradeId = tradeId; }

        public Long getBuyerUserId() { return buyerUserId; }
        public void setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; }

        public Long getSellerUserId() { return sellerUserId; }
        public void setSellerUserId(Long sellerUserId) { this.sellerUserId = sellerUserId; }

        public Long getMakerOrderId() { return makerOrderId; }
        public void setMakerOrderId(Long makerOrderId) { this.makerOrderId = makerOrderId; }

        public Long getTakerOrderId() { return takerOrderId; }
        public void setTakerOrderId(Long takerOrderId) { this.takerOrderId = takerOrderId; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(BigDecimal filledQuantity) { this.filledQuantity = filledQuantity; }

    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal remainingQuantity) { this.remainingQuantity = remainingQuantity; }

    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }

    public String getMatchedByNode() { return matchedByNode; }
    public void setMatchedByNode(String matchedByNode) { this.matchedByNode = matchedByNode; }

    public long getExecutionTimestamp() { return executionTimestamp; }
    public void setExecutionTimestamp(long executionTimestamp) { this.executionTimestamp = executionTimestamp; }

    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<RemoteTradeDto> getTrades() { return trades; }
    public void setTrades(List<RemoteTradeDto> trades) { this.trades = trades; }
}
