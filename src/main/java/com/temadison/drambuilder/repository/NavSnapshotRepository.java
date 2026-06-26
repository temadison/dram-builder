package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.NavSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavSnapshotRepository extends JpaRepository<NavSnapshot, Long> {

    Optional<NavSnapshot> findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(String ticker);

    List<NavSnapshot> findTop2ByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(String ticker);
}
