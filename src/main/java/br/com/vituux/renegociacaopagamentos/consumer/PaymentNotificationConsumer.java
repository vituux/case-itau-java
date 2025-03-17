package br.com.vituux.renegociacaopagamentos.consumer;


import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class PaymentNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationConsumer.class);

    private final AmazonSQS amazonSQS;
    private final AmazonCloudWatch cloudWatch;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.payment-queue}")
    private String queueUrl;

    public PaymentNotificationConsumer(AmazonSQS amazonSQS,
                                       AmazonCloudWatch cloudWatch,
                                       ObjectMapper objectMapper) {
        this.amazonSQS = amazonSQS;
        this.cloudWatch = cloudWatch;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.polling-interval:5000}")
    public void consumeMessages() {
        log.info("Iniciando consumo de mensagens da fila SQS: {}", queueUrl);

        try {
            // Configuração para receber mensagens da fila
            ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMaxNumberOfMessages(10)
                    .withWaitTimeSeconds(5);

            // Recebe as mensagens
            List<Message> messages = amazonSQS.receiveMessage(receiveRequest).getMessages();
            log.info("Recebidas {} mensagens da fila", messages.size());

            // Processa cada mensagem
            for (Message message : messages) {
                try {
                    processMessage(message);

                    // Remove a mensagem da fila após processamento bem-sucedido
                    amazonSQS.deleteMessage(new DeleteMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withReceiptHandle(message.getReceiptHandle()));

                    // Envia métrica de sucesso para CloudWatch
                    publishSuccessMetric();

                } catch (Exception e) {
                    log.error("Erro ao processar mensagem: {}", e.getMessage(), e);
                    publishFailureMetric(e.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            log.error("Erro ao consumir mensagens da fila: {}", e.getMessage(), e);
        }
    }

    private void processMessage(Message message) throws Exception {
        log.info("Processando mensagem: {}", message.getMessageId());

        // Extrai o conteúdo da mensagem
        Map messageContent = objectMapper.readValue(message.getBody(), Map.class);

        // No caso do SNS, o payload real está dentro de um campo específico
        if (messageContent.containsKey("Message")) {
            String messageBody = (String) messageContent.get("Message");
            Map paymentData = objectMapper.readValue(messageBody, Map.class);

            // Lógica de processamento da notificação de pagamento
            Long boletoId = ((Number) paymentData.get("boletoId")).longValue();
            String status = (String) paymentData.get("status");

            log.info("Notificação de pagamento processada: boletoId={}, status={}", boletoId, status);

        }
    }

    private void publishSuccessMetric() {
        MetricDatum datum = new MetricDatum()
                .withMetricName("ProcessedMessages")
                .withUnit(StandardUnit.Count)
                .withValue(1.0)
                .withTimestamp(new Date())
                .withDimensions(new Dimension()
                        .withName("Status")
                        .withValue("Success"));

        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace("BoletoPayment/Notifications")
                .withMetricData(datum);

        cloudWatch.putMetricData(request);
    }

    private void publishFailureMetric(String errorType) {
        MetricDatum datum = new MetricDatum()
                .withMetricName("FailedMessages")
                .withUnit(StandardUnit.Count)
                .withValue(1.0)
                .withTimestamp(new Date())
                .withDimensions(
                        new Dimension().withName("Status").withValue("Failure"),
                        new Dimension().withName("ErrorType").withValue(errorType)
                );

        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace("BoletoPayment/Notifications")
                .withMetricData(datum);

        cloudWatch.putMetricData(request);
    }
}