package t_12.backend.api.transaction;

import java.math.BigDecimal;

/**
 * Request body for POST /api/transactions/transfer.
 * Contains the fields needed to submit a blockchain
 * transfer between two wallet addresses.
 */
public class TransferRequest {

    private String senderAddress;
    private String receiverAddress;
    private BigDecimal amount;
    private BigDecimal fee;
    private Integer nonce;

    public TransferRequest() {}

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public Integer getNonce() {
        return nonce;
    }

    public void setNonce(Integer nonce) {
        this.nonce = nonce;
    }
}