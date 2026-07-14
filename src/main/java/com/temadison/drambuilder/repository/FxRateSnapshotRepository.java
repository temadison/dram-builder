package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.FxRateSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateSnapshotRepository extends JpaRepository<FxRateSnapshot, Long> {

    Optional<FxRateSnapshot> findFirstByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(String baseCurrency, String quoteCurrency);

    Optional<FxRateSnapshot> findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
            String baseCurrency,
            String quoteCurrency,
            Instant observedAtStart,
            Instant observedAtEnd
    );

    List<FxRateSnapshot> findTop2ByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(String baseCurrency, String quoteCurrency);

    Optional<FxRateSnapshot> findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtBeforeOrderByObservedAtDesc(
            String baseCurrency,
            String quoteCurrency,
            Instant observedAt
    );

    List<FxRateSnapshot> findTop20ByOrderByObservedAtDesc();
}
