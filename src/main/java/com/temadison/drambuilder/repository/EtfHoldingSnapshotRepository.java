package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.EtfHoldingSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfHoldingSnapshotRepository extends JpaRepository<EtfHoldingSnapshot, Long> {

    @EntityGraph(attributePaths = {"etf", "holdings", "holdings.security"})
    Optional<EtfHoldingSnapshot> findFirstByEtfTickerOrderByCreatedAtDesc(String ticker);
}
