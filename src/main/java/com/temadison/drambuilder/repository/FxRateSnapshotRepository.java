package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.FxRateSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateSnapshotRepository extends JpaRepository<FxRateSnapshot, Long> {

    Optional<FxRateSnapshot> findFirstByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(String baseCurrency, String quoteCurrency);

    List<FxRateSnapshot> findTop20ByOrderByObservedAtDesc();
}
