package br.com.vituux.renegociacaopagamentos.controller;

import br.com.vituux.renegociacaopagamentos.domain.SagaLog;
import br.com.vituux.renegociacaopagamentos.service.BoletoPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final BoletoPaymentService boletoPaymentService;

    public MetricsController(BoletoPaymentService boletoPaymentService) {
        this.boletoPaymentService = boletoPaymentService;
    }

    @GetMapping("/sagas/statistics")
    public ResponseEntity<Map<String, Object>> getSagaStatistics() {
        return ResponseEntity.ok(boletoPaymentService.getSagaStatistics());
    }
}