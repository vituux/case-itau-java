package br.com.vituux.renegociacaopagamentos.repository;

import br.com.vituux.renegociacaopagamentos.domain.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {
    List<SagaLog> findBySagaIdOrderByCreatedAtAsc(String sagaId);

    Optional<SagaLog> findTopBySagaIdOrderByCreatedAtDesc(String sagaId);

    Long countByStepAndStatus(String step, String status);
}