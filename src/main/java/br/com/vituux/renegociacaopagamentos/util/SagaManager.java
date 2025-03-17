package br.com.vituux.renegociacaopagamentos.util;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import br.com.vituux.renegociacaopagamentos.domain.PaymentStatus;
import br.com.vituux.renegociacaopagamentos.domain.SagaLog;
import br.com.vituux.renegociacaopagamentos.repository.BoletoRepository;
import br.com.vituux.renegociacaopagamentos.repository.SagaLogRepository;
import com.amazonaws.services.s3.AmazonS3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Classe utilitária para gerenciamento de Sagas.
 * Encapsula todas as operações relacionadas ao padrão de design Saga.
 */
@Component
public class SagaManager {
    private static final Logger log = LoggerFactory.getLogger(SagaManager.class);

    private final SagaLogRepository sagaLogRepository;
    private final BoletoRepository boletoRepository;
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public SagaManager(SagaLogRepository sagaLogRepository,
                       BoletoRepository boletoRepository,
                       AmazonS3 amazonS3) {
        this.sagaLogRepository = sagaLogRepository;
        this.boletoRepository = boletoRepository;
        this.amazonS3 = amazonS3;
    }

    public static String createSagaId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Cria um log para um passo da saga.
     *
     * @param sagaId       ID da saga
     * @param entityId     ID da entidade relacionada
     * @param entityType   Tipo da entidade
     * @param step         Passo da saga
     * @param status       Status do passo (SUCCESS, FAILED, ERROR)
     * @param errorMessage Mensagem de erro (se houver)
     */
    public void createSagaLog(String sagaId, String entityId, String entityType,
                              String step, String status, String errorMessage) {
        SagaLog log = new SagaLog();
        log.setSagaId(sagaId);
        log.setEntityId(entityId);
        log.setEntityType(entityType);
        log.setStep(step);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());

        sagaLogRepository.save(log);
    }

    /**
     * Executa compensações para um pagamento com falha.
     * @param sagaId ID da saga
     * @param boleto Boleto relacionado
     */
    public void compensateFailedPayment(String sagaId, Boleto boleto) {
        try {
            // Obtém o último passo da saga para saber o que precisa ser compensado
            SagaLog lastStep = sagaLogRepository.findTopBySagaIdOrderByCreatedAtDesc(sagaId)
                    .orElseThrow(() -> new IllegalStateException("Nenhum log de saga encontrado para: " + sagaId));

            log.info("Iniciando compensação para saga {} no passo {}", sagaId, lastStep.getStep());
            createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "COMPENSATION_STARTED", "SUCCESS", null);

            // Compensa conforme o último passo concluído com sucesso
            switch (lastStep.getStep()) {
                case "PAYMENT_PROCESSED", "STATUS_UPDATED_TO_PROCESSING" -> {
                    // Reverte o status do boleto para PENDING
                    boleto.setStatus(PaymentStatus.PENDING);
                    boleto.setPaymentDate(null);
                    boleto.setPaymentMethod(null);
                    boleto.setTransactionId(null);
                    boleto.setReceiptUrl(null);
                    boletoRepository.save(boleto);
                    createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "STATUS_REVERTED", "SUCCESS", null);
                }
                case "RECEIPT_GENERATED" -> {
                    // Remove o comprovante do S3 se necessário
                    if (boleto.getReceiptUrl() != null) {
                        try {
                            // Extrai o caminho do arquivo no S3 a partir da URL
                            String key = extractS3KeyFromUrl(boleto.getReceiptUrl());
                            amazonS3.deleteObject(bucketName, key);
                            createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "RECEIPT_REMOVED", "SUCCESS", null);
                        } catch (Exception e) {
                            log.error("Erro ao remover comprovante: {}", e.getMessage());
                            createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "RECEIPT_REMOVAL", "FAILED", e.getMessage());
                        }
                    }
                }
            }

            createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "COMPENSATION_COMPLETED", "SUCCESS", null);
            log.info("Compensação concluída para saga {}", sagaId);

        } catch (Exception e) {
            log.error("Erro durante compensação da saga {}: {}", sagaId, e.getMessage());
            createSagaLog(sagaId, boleto.getId().toString(), "Boleto", "COMPENSATION_FAILED", "ERROR", e.getMessage());
            // Em ambiente de produção, poderia notificar equipe de operações ou inserir em uma fila de correção manual
        }
    }

    /**
     * Extrai a chave S3 a partir da URL de um objeto.
     * @param url URL do objeto S3
     * @return Chave do objeto no S3
     */
    public static String extractS3KeyFromUrl(String url) {

        if (url.contains("localhost:4566")) {
            // LocalStack URL
            String path = url.substring(url.indexOf("/", 8));
            path = path.substring(path.indexOf("/", 1));
            return path.startsWith("/") ? path.substring(1) : path;
        } else {
            return url.substring(url.indexOf(".com/") + 5);
        }
    }

    /**
     * Obtém o histórico completo de uma saga.
     * @param sagaId ID da saga
     * @return Lista de logs da saga em ordem cronológica
     */
    public List<SagaLog> getSagaHistory(String sagaId) {
        return sagaLogRepository.findBySagaIdOrderByCreatedAtAsc(sagaId);
    }

    /**
     * Obtém estatísticas sobre sagas no sistema.
     * @return Mapa com estatísticas de sagas
     */
    public Map<String, Object> getSagaStatistics() {
        long completedSagas = sagaLogRepository.countByStepAndStatus("SAGA_COMPLETED", "SUCCESS");
        long failedSagas = sagaLogRepository.countByStepAndStatus("SAGA_FAILED", "ERROR");
        long compensatedSagas = sagaLogRepository.countByStepAndStatus("COMPENSATION_COMPLETED", "SUCCESS");

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("completedSagas", completedSagas);
        statistics.put("failedSagas", failedSagas);
        statistics.put("compensatedSagas", compensatedSagas);
        statistics.put("successRate", calculateSuccessRate(completedSagas, failedSagas));

        return statistics;
    }

    /**
     * Calcula a taxa de sucesso das sagas.
     * @param completed Número de sagas completadas com sucesso
     * @param failed Número de sagas que falharam
     * @return Taxa de sucesso em porcentagem
     */
    private static double calculateSuccessRate(long completed, long failed) {
        long total = completed + failed;
        return total > 0 ? (completed * 100.0 / total) : 0.0;
    }

    /**
     * Verifica se uma saga existe.
     * @param sagaId ID da saga
     * @return true se a saga existe, false caso contrário
     */
    public boolean sagaExists(String sagaId) {
        return sagaLogRepository.findTopBySagaIdOrderByCreatedAtDesc(sagaId).isPresent();
    }

    /**
     * Verifica se uma saga foi concluída com sucesso.
     * @param sagaId ID da saga
     * @return true se a saga foi concluída com sucesso, false caso contrário
     */
    public boolean isSagaCompleted(String sagaId) {
        return sagaLogRepository.findTopBySagaIdOrderByCreatedAtDesc(sagaId)
                .map(log -> "SAGA_COMPLETED".equals(log.getStep()) && "SUCCESS".equals(log.getStatus()))
                .orElse(false);
    }
}