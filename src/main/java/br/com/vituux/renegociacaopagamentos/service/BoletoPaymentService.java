package br.com.vituux.renegociacaopagamentos.service;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import br.com.vituux.renegociacaopagamentos.domain.PaymentStatus;
import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentRequest;
import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentResponse;
import br.com.vituux.renegociacaopagamentos.repository.BoletoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoletoPaymentService {

    private final BoletoRepository boletoRepository;
    private final NotificationService notificationService;
    private final ReceiptService receiptService;

    public BoletoPaymentService(BoletoRepository boletoRepository, NotificationService notificationService, ReceiptService receiptService) {
        this.boletoRepository = boletoRepository;
        this.notificationService = notificationService;
        this.receiptService = receiptService;
    }

    private static final Logger log = LoggerFactory.getLogger(BoletoPaymentService.class);

    public List<BoletoPaymentResponse> getAllBoletos() {
        return boletoRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BoletoPaymentResponse> getPendingBoletos() {
        return boletoRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BoletoPaymentResponse getBoletoById(Long id) {
        Boleto boleto = findBoletoById(id);
        return mapToResponse(boleto);
    }

    @Transactional
    public BoletoPaymentResponse processPayment(Long boletoId, BoletoPaymentRequest request) {
        log.info("Processando pagamento do boleto id={}", boletoId);

        Boleto boleto = findBoletoById(boletoId);

        // Verifica se o boleto já foi pago
        if (PaymentStatus.PAID.equals(boleto.getStatus())) {
            throw new IllegalStateException("Boleto já foi pago");
        }

        // Verifica se o boleto está vencido
        if (boleto.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Boleto vencido");
        }

        // Verifica se o valor do pagamento é válido
        validatePaymentValue(boleto.getValue(), request.getPaymentValue());

        // Processa o pagamento
        boleto.setStatus(PaymentStatus.PAID);
        boleto.setPaymentDate(LocalDateTime.now());
        boleto.setPaymentMethod(request.getPaymentMethod());
        boleto.setTransactionId(generateTransactionId());

        // Gera comprovante
        String receiptUrl = receiptService.generateReceipt(boleto);
        boleto.setReceiptUrl(receiptUrl);

        // Salva o boleto atualizado
        boleto = boletoRepository.save(boleto);
        log.info("Boleto id={} pago com sucesso", boletoId);

        // Envia notificação de pagamento
        notificationService.notifyPayment(boleto);

        return mapToResponse(boleto);
    }

    private String generateTransactionId() {
        return "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validatePaymentValue(BigDecimal boletoValue, BigDecimal paymentValue) {

        if (paymentValue.compareTo(boletoValue) < 0) {
            throw new IllegalArgumentException("Valor do pagamento menor que o valor do boleto");
        }
    }

    private Boleto findBoletoById(Long id) {
        return boletoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Boleto não encontrado: " + id));
    }

    private BoletoPaymentResponse mapToResponse(Boleto boleto) {
        return BoletoPaymentResponse.builder()
                .id(boleto.getId())
                .barcode(boleto.getBarcode())
                .beneficiary(boleto.getBeneficiary())
                .payer(boleto.getPayer())
                .value(boleto.getValue())
                .dueDate(boleto.getDueDate())
                .status(boleto.getStatus().name())
                .paymentDate(boleto.getPaymentDate())
                .paymentMethod(boleto.getPaymentMethod())
                .receiptUrl(boleto.getReceiptUrl())
                .transactionId(boleto.getTransactionId())
                .build();
    }
}