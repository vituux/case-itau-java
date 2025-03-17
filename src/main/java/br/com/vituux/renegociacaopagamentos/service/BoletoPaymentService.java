package br.com.vituux.renegociacaopagamentos.service;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import br.com.vituux.renegociacaopagamentos.domain.PaymentStatus;
import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentRequest;
import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentResponse;
import br.com.vituux.renegociacaopagamentos.repository.BoletoRepository;
import br.com.vituux.renegociacaopagamentos.util.SagaManager;
import br.com.vituux.renegociacaopagamentos.domain.SagaLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoletoPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BoletoPaymentService.class);

    private final BoletoRepository boletoRepository;
    private final ReceiptService receiptService;
    private final NotificationService notificationService;
    private final SagaManager sagaManager;

    public BoletoPaymentService(BoletoRepository boletoRepository,
                                ReceiptService receiptService,
                                NotificationService notificationService,
                                SagaManager sagaManager) {
        this.boletoRepository = boletoRepository;
        this.receiptService = receiptService;
        this.notificationService = notificationService;
        this.sagaManager = sagaManager;
    }

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
        try {
            // Executa a saga de pagamento
            return executePaymentSaga(boletoId, request);
        } catch (OptimisticLockException e) {
            log.warn("Detectada concorrência ao processar pagamento para boleto id={}", boletoId);
            return retryPayment(boletoId, request);
        }
    }

    @Transactional
    public BoletoPaymentResponse executePaymentSaga(Long boletoId, BoletoPaymentRequest request) {
        // Gera um ID único para a saga
        String sagaId = SagaManager.createSagaId();

        // Recupera o boleto
        Boleto boleto = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new EntityNotFoundException("Boleto não encontrado: " + boletoId));

        try {
            // Registra o início da saga
            sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "PAYMENT_STARTED", "SUCCESS", null);

            // Passo 1: Validação do boleto
            if (PaymentStatus.PAID.equals(boleto.getStatus())) {
                throw new IllegalStateException("Boleto já foi pago");
            }

            if (boleto.getDueDate().isBefore(LocalDate.now())) {
                throw new IllegalStateException("Boleto vencido");
            }

            // Valida o valor do pagamento
            validatePaymentValue(boleto.getValue(), request.getPaymentValue());
            sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "VALIDATION_COMPLETED", "SUCCESS", null);

            // Passo 2: Atualização do status para PROCESSING
            boleto.setStatus(PaymentStatus.PROCESSING);
            boleto = boletoRepository.save(boleto);
            sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "STATUS_UPDATED_TO_PROCESSING", "SUCCESS", null);

            // Passo 3: Processamento do pagamento
            boleto.setStatus(PaymentStatus.PAID);
            boleto.setPaymentDate(LocalDateTime.now());
            boleto.setPaymentMethod(request.getPaymentMethod());
            boleto.setTransactionId(generateTransactionId());
            boleto = boletoRepository.save(boleto);
            sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "PAYMENT_PROCESSED", "SUCCESS", null);

            // Passo 4: Geração do comprovante (não bloqueia a saga em caso de falha)
            try {
                String receiptUrl = receiptService.generateReceipt(boleto);
                boleto.setReceiptUrl(receiptUrl);
                boleto = boletoRepository.save(boleto);
                sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "RECEIPT_GENERATED", "SUCCESS", null);
            } catch (Exception e) {
                log.error("Erro ao gerar comprovante para boleto {}: {}", boleto.getId(), e.getMessage());
                sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "RECEIPT_GENERATION", "FAILED", e.getMessage());
                // Continua a saga mesmo com falha no comprovante
            }

            // Passo 5: Envio de notificação (não bloqueia a saga em caso de falha)
            try {
                notificationService.notifyPayment(boleto);
                sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "NOTIFICATION_SENT", "SUCCESS", null);
            } catch (Exception e) {
                log.error("Erro ao enviar notificação para boleto {}: {}", boleto.getId(), e.getMessage());
                sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "NOTIFICATION", "FAILED", e.getMessage());
                // Continua a saga mesmo com falha na notificação
            }

            // Finaliza a saga com sucesso
            sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "SAGA_COMPLETED", "SUCCESS", null);

            return mapToResponse(boleto);

        } catch (Exception e) {
            // Em caso de erro, registra a falha e executa compensações
            log.error("Erro na saga de pagamento para boleto {}: {}", boletoId, e.getMessage());
            sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "SAGA_FAILED", "ERROR", e.getMessage());

            // Executa compensações para garantir consistência
            sagaManager.compensateFailedPayment(sagaId, boleto);

            throw e;
        }
    }

    private BoletoPaymentResponse retryPayment(Long boletoId, BoletoPaymentRequest request) {
        // Lógica de retry com backoff exponencial
        int maxRetries = 3;
        int attempt = 0;
        long waitTimeMs = 100; // tempo inicial de espera

        while (attempt < maxRetries) {
            try {
                attempt++;
                // Espera antes de tentar novamente
                Thread.sleep(waitTimeMs);
                // Aumenta o tempo de espera exponencialmente
                waitTimeMs *= 2;

                log.info("Tentativa {} de pagamento para boleto id={}", attempt, boletoId);

                // Busca novamente o boleto com dados atualizados
                Boleto boleto = boletoRepository.findById(boletoId)
                        .orElseThrow(() -> new EntityNotFoundException("Boleto não encontrado: " + boletoId));

                // Verifica novamente o status
                if (PaymentStatus.PAID.equals(boleto.getStatus())) {
                    log.info("Boleto id={} já foi pago por outro processo", boletoId);
                    return mapToResponse(boleto);
                }

                // Tenta executar a saga novamente
                return executePaymentSaga(boletoId, request);

            } catch (OptimisticLockException e) {
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Falha após múltiplas tentativas de pagamento do boleto", e);
                }
                log.warn("Retry {} falhou com conflito de concorrência", attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processamento de pagamento interrompido", e);
            } catch (Exception e) {
                // Outras exceções não relacionadas a concorrência são propagadas imediatamente
                throw new RuntimeException("Erro ao processar pagamento: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("Não foi possível processar o pagamento após " + maxRetries + " tentativas");
    }

    @Transactional
    public BoletoPaymentResponse cancelPayment(Long boletoId) {
        Boleto boleto = findBoletoById(boletoId);

        if (PaymentStatus.COMPLETED.equals(boleto.getStatus())) {
            throw new IllegalStateException("Não é possível cancelar um boleto já pago");
        }

        if (PaymentStatus.CANCELLED.equals(boleto.getStatus())) {
            return mapToResponse(boleto); // Já está cancelado
        }

        // Inicia uma nova saga para cancelamento
        String sagaId = SagaManager.createSagaId();
        sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "CANCELLATION_STARTED", "SUCCESS", null);

        boleto.setStatus(PaymentStatus.CANCELLED);
        boleto = boletoRepository.save(boleto);

        sagaManager.createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "CANCELLATION_COMPLETED", "SUCCESS", null);

        return mapToResponse(boleto);
    }

    public Map<String, Object> getSagaStatistics() {
        return sagaManager.getSagaStatistics();
    }

    public List<SagaLog> getSagaHistory(String sagaId) {
        return sagaManager.getSagaHistory(sagaId);
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