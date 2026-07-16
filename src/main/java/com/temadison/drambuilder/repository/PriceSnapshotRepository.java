package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.PriceSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(String ticker, String exchange);

    Optional<PriceSnapshot> findFirstBySecurityTickerAndSecurityExchangeAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
            String ticker,
            String exchange,
            Instant observedAtStart,
            Instant observedAtEnd
    );

    List<PriceSnapshot> findTop2BySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(String ticker, String exchange);

    List<PriceSnapshot> findTop120BySecurityTickerAndSecurityExchangeOrderByObservedAtAsc(String ticker, String exchange);

    Optional<PriceSnapshot> findFirstBySecurityTickerAndSecurityExchangeAndObservedAtBeforeOrderByObservedAtDesc(
            String ticker,
            String exchange,
            Instant observedAt
    );

    List<PriceSnapshot> findTop20ByOrderByObservedAtDesc();
}
