package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.NavSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavSnapshotRepository extends JpaRepository<NavSnapshot, Long> {

    @EntityGraph(attributePaths = {"holdingSnapshot", "holdingSnapshot.etf", "holdingSnapshot.holdings", "holdingSnapshot.holdings.security"})
    Optional<NavSnapshot> findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(String ticker);
}
