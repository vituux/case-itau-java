package br.com.vituux.renegociacaopagamentos.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "boletos")
public class Boleto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String barcode;

    @Column(nullable = false)
    private String beneficiary;

    @Column(nullable = false)
    private String payer;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private LocalDateTime paymentDate;

    private String paymentMethod;

    private String receiptUrl;

    private String transactionId;

    // Construtor padrão
    public Boleto() {
    }

    // Construtor com todos os campos
    public Boleto(Long id, String barcode, String beneficiary, String payer, BigDecimal value,
                  LocalDate dueDate, PaymentStatus status, LocalDateTime paymentDate,
                  String paymentMethod, String receiptUrl, String transactionId) {
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
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

    // Static Builder class para manter a funcionalidade builder sem usar Lombok
    public static BoletoBuilder builder() {
        return new BoletoBuilder();
    }

    public static class BoletoBuilder {
        private Long id;
        private String barcode;
        private String beneficiary;
        private String payer;
        private BigDecimal value;
        private LocalDate dueDate;
        private PaymentStatus status;
        private LocalDateTime paymentDate;
        private String paymentMethod;
        private String receiptUrl;
        private String transactionId;

        BoletoBuilder() {
        }

        public BoletoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BoletoBuilder barcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        public BoletoBuilder beneficiary(String beneficiary) {
            this.beneficiary = beneficiary;
            return this;
        }

        public BoletoBuilder payer(String payer) {
            this.payer = payer;
            return this;
        }

        public BoletoBuilder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public BoletoBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public BoletoBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public BoletoBuilder paymentDate(LocalDateTime paymentDate) {
            this.paymentDate = paymentDate;
            return this;
        }

        public BoletoBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public BoletoBuilder receiptUrl(String receiptUrl) {
            this.receiptUrl = receiptUrl;
            return this;
        }

        public BoletoBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Boleto build() {
            return new Boleto(id, barcode, beneficiary, payer, value, dueDate, status,
                    paymentDate, paymentMethod, receiptUrl, transactionId);
        }
    }
}
