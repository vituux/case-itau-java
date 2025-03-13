package br.com.vituux.renegociacaopagamentos.controller;


import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentRequest;
import br.com.vituux.renegociacaopagamentos.dto.BoletoPaymentResponse;
import br.com.vituux.renegociacaopagamentos.service.BoletoPaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@RestController
@RequestMapping("/boletos")
public class BoletoPaymentController {

    private final BoletoPaymentService boletoPaymentService;

    public BoletoPaymentController(BoletoPaymentService boletoPaymentService) {
        this.boletoPaymentService = boletoPaymentService;
    }
    private static final Logger log = LoggerFactory.getLogger(BoletoPaymentController.class);

    @GetMapping
    public ResponseEntity<List<BoletoPaymentResponse>> getAllBoletos() {
        return ResponseEntity.ok(boletoPaymentService.getAllBoletos());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<BoletoPaymentResponse>> getPendingBoletos() {
        return ResponseEntity.ok(boletoPaymentService.getPendingBoletos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoletoPaymentResponse> getBoletoById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(boletoPaymentService.getBoletoById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> payBoleto(
            @PathVariable Long id,
            @Valid @RequestBody BoletoPaymentRequest request) {
        try {
            BoletoPaymentResponse response = boletoPaymentService.processPayment(id, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao processar pagamento: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar o pagamento: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> getReceipt(@PathVariable Long id) {
        try {
            BoletoPaymentResponse boleto = boletoPaymentService.getBoletoById(id);
            if (Objects.isNull(boleto.getReceiptUrl())) {
                return ResponseEntity.badRequest().body("Comprovante não disponível para este boleto");
            }
            return ResponseEntity.ok().body(Map.of("receiptUrl", boleto.getReceiptUrl()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}