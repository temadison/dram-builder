package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.OfficialNavSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialNavSnapshotRepository extends JpaRepository<OfficialNavSnapshot, Long> {

    Optional<OfficialNavSnapshot> findFirstByEtfTickerOrderByObservedAtDesc(String ticker);

    List<OfficialNavSnapshot> findTop20ByOrderByObservedAtDesc();
}
