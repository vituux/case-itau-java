package br.com.vituux.renegociacaopagamentos.repository;

import br.com.vituux.renegociacaopagamentos.domain.Boleto;
import br.com.vituux.renegociacaopagamentos.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long> {

    List<Boleto> findByStatus(PaymentStatus status);

}
