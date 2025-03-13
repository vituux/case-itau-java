package br.com.vituux.renegociacaopagamentos.service;


import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
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

    private final AmazonS3 amazonS3;

    public ReceiptService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }
    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String generateReceipt(Boleto boleto) {
        try {
            String receiptId = UUID.randomUUID().toString();
            String fileName = String.format("receipts/%d/%s.txt", boleto.getId(), receiptId);

            // Criar conteúdo do comprovante
            String receiptContent = generateReceiptContent(boleto);

            // Upload para o S3
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

            amazonS3.putObject(request);

            // Gerar URL do comprovante
            String receiptUrl = amazonS3.getUrl(bucketName, fileName).toString();
            log.info("Comprovante gerado com sucesso: {}", receiptUrl);

            return receiptUrl;
        } catch (Exception e) {
            log.error("Erro ao gerar comprovante: {}", e.getMessage());
            throw new RuntimeException("Falha ao gerar comprovante de pagamento", e);
        }
    }

    private String generateReceiptContent(Boleto boleto) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        StringBuilder string = new StringBuilder();
        string.append("=============== COMPROVANTE DE PAGAMENTO ===============\n\n");
        string.append("Código de Barras: ").append(boleto.getBarcode()).append("\n");
        string.append("Beneficiário: ").append(boleto.getBeneficiary()).append("\n");
        string.append("Pagador: ").append(boleto.getPayer()).append("\n");
        string.append("Valor: R$ ").append(boleto.getValue()).append("\n");
        string.append("Data de Vencimento: ").append(boleto.getDueDate().format(dateFormatter)).append("\n");
        string.append("Data do Pagamento: ").append(boleto.getPaymentDate().format(dateTimeFormatter)).append("\n");
        string.append("Forma de Pagamento: ").append(boleto.getPaymentMethod()).append("\n");
        string.append("ID da Transação: ").append(boleto.getTransactionId()).append("\n\n");
        string.append("Status: ").append(boleto.getStatus()).append("\n\n");
        string.append("Este documento é um comprovante de pagamento.\n");
        string.append("=====================================================\n");

        return string.toString();
    }
}
