# Sistema de Pagamento de Boletos

Um microserviço resiliente para processamento de pagamentos de boletos bancários, desenvolvido em Spring Boot e Java, utilizando LocalStack para simular serviços AWS.

## 🚀 Recursos

- Processamento de pagamentos de boletos
- Geração automática de comprovantes
- Integração com sistema de notificação
- Suporte a diferentes métodos de pagamento
- Controle de concorrência
- Mecanismos de resiliência com Resilience4j
- Integração com AWS (via LocalStack) para:
  - Armazenamento de comprovantes (S3)
  - Envio de notificações (SNS)
  - Processamento assíncrono (SQS)

## 📋 Pré-requisitos

- Java 17+
- Docker e Docker Compose
- Maven

## 🔧 Configuração e Execução

### Clone o repositório

```bash
git clone https://github.com/vituux/case-itau-java.git
cd case-itau-java
```

### Inicie o ambiente local com Docker Compose

```bash
docker-compose up -d
```

### Execute o script de inicialização do LocalStack

```bash
# Dentro do container
docker exec -it boleto-localstack bash -c "cd /docker-entrypoint-initaws.d && sh init-localstack.sh"
```

### Compile e execute a aplicação

```bash
./mvnw spring-boot:run -Dspring.profiles.active=local
```

A aplicação estará disponível em: http://localhost:8080/api/boletos

## 📌 API Endpoints

### Listar todos os boletos
```
GET /api/boletos
```

### Listar boletos pendentes
```
GET /api/boletos/pending
```

### Obter boleto pelo ID
```
GET /api/boletos/{id}
```

### Efetuar pagamento
```
POST /api/boletos/{id}/pay
Content-Type: application/json

{
  "paymentMethod": "BANK_TRANSFER",
  "paymentValue": 150.75
}
```

### Obter comprovante
```
GET /api/boletos/{id}/receipt
```

## 🧪 Testando com LocalStack

### Verificando notificações no SNS/SQS
```bash
# Listar tópicos SNS
aws --endpoint-url=http://localhost:4566 sns list-topics

# Receber mensagens da fila SQS
aws --endpoint-url=http://localhost:4566 sqs receive-message --queue-url http://localhost:4566/000000000000/boleto-payment-queue
```

### Verificando comprovantes no S3
```bash
# Listar comprovantes no bucket
aws --endpoint-url=http://localhost:4566 s3 ls s3://boleto-receipts/receipts/

# Baixar um comprovante específico
aws --endpoint-url=http://localhost:4566 s3 cp s3://boleto-receipts/receipts/{id}/{receipt-id}.txt comprovante.txt
```

## 🛠️ Arquitetura

### Componentes Principais

- **BoletoPaymentController**: API REST para operações com boletos
- **BoletoPaymentService**: Lógica de negócio para processamento de pagamentos
- **NotificationService**: Envio de notificações via SNS
- **ReceiptService**: Geração e armazenamento de comprovantes no S3
- **AwsConfig**: Configuração dos clientes AWS/LocalStack

### Mecanismos de Resiliência

- **Controle de Concorrência**: Implementação de versionamento otimista (via @Version)
- **Circuit Breaker**: Proteção contra falhas em serviços externos
- **Retry com Backoff Exponencial**: Tentativas automáticas em caso de falhas temporárias
- **Transações Distribuídas**: Implementação do padrão Saga
- **Compensações**: Mecanismos de rollback em caso de falha parcial

## 📈 Design Técnico

### Banco de Dados
- PostgreSQL para armazenamento de dados transacionais
- Modelo de entidades com controle de versão otimista

### AWS (via LocalStack)
- S3 para armazenamento de comprovantes
- SNS para publicação de eventos
- SQS para processamento assíncrono

### Segurança
- Validação de requisições
- Tratamento adequado de exceções
- Logs de auditoria de operações críticas

## 🔍 Monitoramento
- Health indicators personalizados
- Métricas de Circuit Breaker
- Logs estruturados

## 📝 Licença

Este projeto está sob a licença MIT - veja o arquivo [LICENSE.md](LICENSE.md) para detalhes.
