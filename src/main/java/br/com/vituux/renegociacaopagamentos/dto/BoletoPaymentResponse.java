package br.com.vituux.renegociacaopagamentos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public class BoletoPaymentResponse {
    private Long id;
    private String barcode;
    private String beneficiary;
    private String payer;
    private BigDecimal value;
    private LocalDate dueDate;
    private String status;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String receiptUrl;
    private String transactionId;

    // Construtor padrão
    public BoletoPaymentResponse() {
    }

    // Construtor com todos os campos
    public BoletoPaymentResponse(Long id, String barcode, String beneficiary, String payer,
                                 BigDecimal value, LocalDate dueDate, String status,
                                 LocalDateTime paymentDate, String paymentMethod,
                                 String receiptUrl, String transactionId) {
        this.id = id;
        this.barcode = barcode;
        this.beneficiary = beneficiary;
        this.payer = payer;
        this.value = value;
        this.dueDate = dueDate;
        this.status = status;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.receiptUrl = receiptUrl;
        this.transactionId = transactionId;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(String beneficiary) {
        this.beneficiary = beneficiary;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "BoletoPaymentResponse{" +
                "id=" + id +
                ", barcode='" + barcode + '\'' +
                ", beneficiary='" + beneficiary + '\'' +
                ", payer='" + payer + '\'' +
                ", value=" + value +
                ", dueDate=" + dueDate +
                ", status='" + status + '\'' +
                ", paymentDate=" + paymentDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", receiptUrl='" + receiptUrl + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }

    // Classe Builder para manter a funcionalidade de builder
    public static BoletoPaymentResponseBuilder builder() {
        return new BoletoPaymentResponseBuilder();
    }

    public static class BoletoPaymentResponseBuilder {
        private Long id;
        private String barcode;
        private String beneficiary;
        private String payer;
        private BigDecimal value;
        private LocalDate dueDate;
        private String status;
        private LocalDateTime paymentDate;
        private String paymentMethod;
        private String receiptUrl;
        private String transactionId;

        BoletoPaymentResponseBuilder() {
        }

        public BoletoPaymentResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BoletoPaymentResponseBuilder barcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        public BoletoPaymentResponseBuilder beneficiary(String beneficiary) {
            this.beneficiary = beneficiary;
            return this;
        }

        public BoletoPaymentResponseBuilder payer(String payer) {
            this.payer = payer;
            return this;
        }

        public BoletoPaymentResponseBuilder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public BoletoPaymentResponseBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public BoletoPaymentResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public BoletoPaymentResponseBuilder paymentDate(LocalDateTime paymentDate) {
            this.paymentDate = paymentDate;
            return this;
        }

        public BoletoPaymentResponseBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public BoletoPaymentResponseBuilder receiptUrl(String receiptUrl) {
            this.receiptUrl = receiptUrl;
            return this;
        }

        public BoletoPaymentResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public BoletoPaymentResponse build() {
            return new BoletoPaymentResponse(id, barcode, beneficiary, payer, value, dueDate,
                    status, paymentDate, paymentMethod, receiptUrl, transactionId);
        }
    }
}