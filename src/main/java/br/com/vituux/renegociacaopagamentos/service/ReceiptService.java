package br.com.vituux.renegociacaopagamentos.service;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public ReceiptService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    @CircuitBreaker(name = "s3Service", fallbackMethod = "fallbackGenerateReceipt")
    @Retry(name = "s3Service")
    public String generateReceipt(Boleto boleto) {
        log.info("Gerando comprovante para boleto id={}", boleto.getId());

        try {
            String receiptId = UUID.randomUUID().toString();
            String fileName = String.format("receipts/%d/%s.txt", boleto.getId(), receiptId);

            String receiptContent = generateReceiptContent(boleto);

            // Upload para o S3
            PutObjectRequest request = uploadS3(receiptContent, fileName);
            amazonS3.putObject(request);

            // Gerar URL do comprovante
            String receiptUrl = amazonS3.getUrl(bucketName, fileName).toString();
            log.info("Comprovante gerado com sucesso: {}", receiptUrl);

            return receiptUrl;
        } catch (Exception e) {
            log.error("Erro ao gerar comprovante: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao gerar comprovante de pagamento", e);
        }
    }

    private PutObjectRequest uploadS3(String receiptContent, String fileName) {
        byte[] contentBytes = receiptContent.getBytes(StandardCharsets.UTF_8);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentBytes.length);
        metadata.setContentType("text/plain");

        PutObjectRequest request = new PutObjectRequest(
                bucketName,
                fileName,
                new ByteArrayInputStream(contentBytes),
                metadata
        );
        return request;
    }

    public String fallbackGenerateReceipt(Boleto boleto, Exception e) {
        log.warn("Circuito aberto ou erro de retry para S3. Utilizando fallback para boleto id={}", boleto.getId());
        return "/receipts/temp/" + boleto.getId() + "-" + System.currentTimeMillis();
    }

    private String generateReceiptContent(Boleto boleto) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        sb.append("=============== COMPROVANTE DE PAGAMENTO ===============\n\n");
        sb.append("Código de Barras: ").append(boleto.getBarcode()).append("\n");
        sb.append("Beneficiário: ").append(boleto.getBeneficiary()).append("\n");
        sb.append("Pagador: ").append(boleto.getPayer()).append("\n");
        sb.append("Valor: R$ ").append(boleto.getValue()).append("\n");
        sb.append("Data de Vencimento: ").append(boleto.getDueDate().format(dateFormatter)).append("\n");
        sb.append("Data do Pagamento: ").append(boleto.getPaymentDate().format(dateTimeFormatter)).append("\n");
        sb.append("Forma de Pagamento: ").append(boleto.getPaymentMethod()).append("\n");
        sb.append("ID da Transação: ").append(boleto.getTransactionId()).append("\n\n");
        sb.append("Status: ").append(boleto.getStatus()).append("\n\n");
        sb.append("Este documento é um comprovante de pagamento.\n");
        sb.append("=====================================================\n");

        return sb.toString();
    }
}