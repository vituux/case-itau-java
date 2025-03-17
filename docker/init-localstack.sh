#!/bin/bash

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Define a função de verificação de disponibilidade
check_localstack_ready() {
    # Tentativa mais robusta usando o endpoint de health check
    curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1
    return $?
}

# Aguarda o LocalStack estar pronto com timeout
echo "Verificando se o LocalStack está disponível..."
MAX_TRIES=30
TRIES=0

while ! check_localstack_ready; do
    TRIES=$((TRIES+1))
    if [ $TRIES -ge $MAX_TRIES ]; then
        echo "Timeout aguardando o LocalStack iniciar. Verifique se o contêiner está rodando."
        exit 1
    fi
    echo "Aguardando o LocalStack iniciar... ($TRIES/$MAX_TRIES)"
    sleep 2
done

echo "LocalStack está pronto! Criando recursos..."

# Cria o bucket S3 para armazenar comprovantes
aws --endpoint-url=http://localhost:4566 s3 mb s3://boleto-receipts || true
echo "Bucket S3 para comprovantes criado/verificado"

# Cria tópico SNS para notificações de pagamento
TOPIC_ARN=$(aws --endpoint-url=http://localhost:4566 sns create-topic --name boleto-payment-notifications --output text --query 'TopicArn' || aws --endpoint-url=http://localhost:4566 sns list-topics --output text --query 'Topics[0].TopicArn')
echo "Tópico SNS criado/verificado: $TOPIC_ARN"

# Cria fila SQS para processar notificações
QUEUE_URL=$(aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name boleto-payment-queue --output text --query 'QueueUrl' || aws --endpoint-url=http://localhost:4566 sqs get-queue-url --queue-name boleto-payment-queue --query 'QueueUrl' --output text)
echo "Fila SQS criada/verificada: $QUEUE_URL"

# Obtém ARN da fila SQS
QUEUE_ARN=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)
echo "ARN da fila SQS: $QUEUE_ARN"

# Inscreve a fila SQS no tópico SNS
aws --endpoint-url=http://localhost:4566 sns subscribe \
  --topic-arn $TOPIC_ARN \
  --protocol sqs \
  --notification-endpoint $QUEUE_ARN \
  --output text \
  --query 'SubscriptionArn' || true
echo "Fila SQS inscrita no tópico SNS"

# Cria namespace no CloudWatch
aws --endpoint-url=http://localhost:4566 cloudwatch put-metric-data \
  --namespace BoletoPayment/Notifications \
  --metric-name Initialization \
  --value 1 \
  --unit Count || true
echo "Namespace CloudWatch configurado"

echo "Configuração do LocalStack concluída!"