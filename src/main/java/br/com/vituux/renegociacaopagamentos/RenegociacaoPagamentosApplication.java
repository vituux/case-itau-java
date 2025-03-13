package br.com.vituux.renegociacaopagamentos;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import br.com.vituux.renegociacaopagamentos.domain.PaymentStatus;
import br.com.vituux.renegociacaopagamentos.repository.BoletoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
@EnableDiscoveryClient
public class RenegociacaoPagamentosApplication {

    public static void main(String[] args) {
        SpringApplication.run(RenegociacaoPagamentosApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public CommandLineRunner demoData(BoletoRepository boletoRepository) {
        return args -> {
            // Verifica se já existem boletos no banco
            if (boletoRepository.count() == 0) {
                // Cria alguns boletos de demonstração
                Boleto boleto1 = Boleto.builder()
                        .barcode("34191790010104351004791020150008291070026000")
                        .beneficiary("Empresa ABC Ltda")
                        .payer("João da Silva")
                        .value(new BigDecimal("149.90"))
                        .dueDate(LocalDate.now().plusDays(5))
                        .status(PaymentStatus.PENDING)
                        .build();

                Boleto boleto2 = Boleto.builder()
                        .barcode("34191790010104351004791020150008291070026111")
                        .beneficiary("Distribuidora XYZ S.A.")
                        .payer("Maria Oliveira")
                        .value(new BigDecimal("89.75"))
                        .dueDate(LocalDate.now().plusDays(10))
                        .status(PaymentStatus.PENDING)
                        .build();

                Boleto boleto3 = Boleto.builder()
                        .barcode("34191790010104351004791020150008291070026222")
                        .beneficiary("Serviços Gerais ME")
                        .payer("Carlos Pereira")
                        .value(new BigDecimal("210.50"))
                        .dueDate(LocalDate.now().plusDays(1))
                        .status(PaymentStatus.PENDING)
                        .build();

                // Salva os boletos
                boletoRepository.save(boleto1);
                boletoRepository.save(boleto2);
                boletoRepository.save(boleto3);

                System.out.println("Dados de demonstração criados com sucesso!");
            }
        };
    }

}
