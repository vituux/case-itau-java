package br.com.vituux.renegociacaopagamentos.service;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private final AmazonSNS amazonSNS;
    private final ObjectMapper objectMapper;

    public NotificationService(AmazonSNS amazonSNS, ObjectMapper objectMapper) {
        this.amazonSNS = amazonSNS;
        this.objectMapper = objectMapper;
    }
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${aws.sns.topic-arn}")
    private String topicArn;

    public void notifyPayment(Boleto boleto) {
        try {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("boletoId", boleto.getId());
            messageData.put("barcode", boleto.getBarcode());
            messageData.put("value", boleto.getValue());
            messageData.put("paymentDate", boleto.getPaymentDate());
            messageData.put("paymentMethod", boleto.getPaymentMethod());
            messageData.put("status", boleto.getStatus().name());

            String message = objectMapper.writeValueAsString(messageData);

            PublishRequest publishRequest = new PublishRequest()
                    .withTopicArn(topicArn)
                    .withMessage(message)
                    .withSubject("Pagamento de Boleto Processado");

            PublishResult result = amazonSNS.publish(publishRequest);

            log.info("Notificação de pagamento enviada. MessageId: {}", result.getMessageId());
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar notificação: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao enviar notificação: {}", e.getMessage());
        }
    }
}
