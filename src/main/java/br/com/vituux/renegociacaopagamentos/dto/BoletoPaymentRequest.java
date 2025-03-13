package br.com.vituux.renegociacaopagamentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BoletoPaymentRequest {

    @NotBlank(message = "Método de pagamento é obrigatório")
    private String paymentMethod;

    @NotNull(message = "Valor do pagamento é obrigatório")
    @Positive(message = "Valor do pagamento deve ser positivo")
    private BigDecimal paymentValue;

    // Construtor padrão
    public BoletoPaymentRequest() {
    }

    // Construtor com todos os campos
    public BoletoPaymentRequest(String paymentMethod, BigDecimal paymentValue) {
        this.paymentMethod = paymentMethod;
        this.paymentValue = paymentValue;
    }

    // Getters e Setters
    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getPaymentValue() {
        return paymentValue;
    }

    public void setPaymentValue(BigDecimal paymentValue) {
        this.paymentValue = paymentValue;
    }

    @Override
    public String toString() {
        return "BoletoPaymentRequest{" +
                "paymentMethod='" + paymentMethod + '\'' +
                ", paymentValue=" + paymentValue +
                '}';
    }
}
