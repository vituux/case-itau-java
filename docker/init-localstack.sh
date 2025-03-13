# Aguarda o LocalStack estar pronto
echo "Esperando o LocalStack iniciar..."
until aws --endpoint-url=http://localhost:4566 s3 ls 2>/dev/null; do
  sleep 1
done

echo "LocalStack está pronto! Criando recursos..."

# Configurar credenciais
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

# Cria o bucket S3 para armazenar comprovantes
aws --endpoint-url=http://localhost:4566 s3 mb s3://boleto-receipts
echo "Bucket S3 para comprovantes criado"

# Cria tópico SNS para notificações de pagamento
aws --endpoint-url=http://localhost:4566 sns create-topic --name boleto-payment-notifications
echo "Tópico SNS criado"

# Cria fila SQS para processar notificações
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name boleto-payment-queue
echo "Fila SQS criada"

# Obtém ARN do tópico SNS
TOPIC_ARN=$(aws --endpoint-url=http://localhost:4566 sns list-topics --query 'Topics[0].TopicArn' --output text)

# Obtém URL da fila SQS
QUEUE_URL=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-url --queue-name boleto-payment-queue --query 'QueueUrl' --output text)

# Obtém ARN da fila SQS
QUEUE_ARN=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

# Inscreve a fila SQS no tópico SNS
aws --endpoint-url=http://localhost:4566 sns subscribe \
  --topic-arn $TOPIC_ARN \
  --protocol sqs \
  --notification-endpoint $QUEUE_ARN
echo "Fila SQS inscrita no tópico SNS"

echo "Configuração do LocalStack concluída!"