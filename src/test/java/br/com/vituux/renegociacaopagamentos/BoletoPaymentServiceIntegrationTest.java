package br.com.vituux.renegociacaopagamentos;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import br.com.vituux.renegociacaopagamentos.domain.PaymentStatus;
import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentRequest;
import br.com.vituux.renegociacaopagamentos.repository.BoletoRepository;
import br.com.vituux.renegociacaopagamentos.service.BoletoPaymentService;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
public class BoletoPaymentServiceIntegrationTest {

    @Autowired
    private BoletoPaymentService boletoPaymentService;

    @Autowired
    private BoletoRepository boletoRepository;

    @Autowired
    private AmazonSNS amazonSNS;

    @Autowired
    private AmazonS3 amazonS3;

    private Boleto testBoleto;

    @BeforeEach
    public void setup() {
        boletoRepository.deleteAll();

        testBoleto = new Boleto();
        // Cria um boleto para teste
        testBoleto = Boleto.builder()
                .barcode("34191790010104351004791020150008291070026999")
                .beneficiary("Empresa Teste")
                .payer("Cliente Teste")
                .value(new BigDecimal("100.00"))
                .dueDate(LocalDate.now().plusDays(5))
                .status(PaymentStatus.PENDING)
                .build();

        testBoleto = boletoRepository.save(testBoleto);
    }

    @Test
    public void testProcessPayment() {
        // Cria uma requisição de pagamento
        BoletoPaymentRequest request = new BoletoPaymentRequest();
        request.setPaymentMethod("BANK_TRANSFER");
        request.setPaymentValue(new BigDecimal("100.00"));

        // Processa o pagamento
        var response = boletoPaymentService.processPayment(testBoleto.getId(), request);

        // Verifica o resultado
        assertNotNull(response);
        assertEquals(testBoleto.getId(), response.getId());
        assertEquals("PAID", response.getStatus());
        assertEquals("BANK_TRANSFER", response.getPaymentMethod());
        assertNotNull(response.getPaymentDate());
        assertNotNull(response.getTransactionId());
        assertNotNull(response.getReceiptUrl());

        // Verifica se o boleto foi atualizado no banco de dados
        Boleto updatedBoleto = boletoRepository.findById(testBoleto.getId()).orElse(null);
        assertNotNull(updatedBoleto);
        assertEquals(PaymentStatus.PAID, updatedBoleto.getStatus());
    }

    @Test
    public void testPaymentWithInvalidValue() {
        // Cria uma requisição de pagamento com valor menor
        BoletoPaymentRequest request = new BoletoPaymentRequest();
        request.setPaymentMethod("BANK_TRANSFER");
        request.setPaymentValue(new BigDecimal("50.00"));  // Metade do valor do boleto

        // Verifica se lança exceção
        assertThrows(IllegalArgumentException.class, () -> boletoPaymentService.processPayment(testBoleto.getId(), request));

        // Verifica se o boleto continua pendente
        Boleto boleto = boletoRepository.findById(testBoleto.getId()).orElse(null);
        assertNotNull(boleto);
        assertEquals(PaymentStatus.PENDING, boleto.getStatus());
    }
}