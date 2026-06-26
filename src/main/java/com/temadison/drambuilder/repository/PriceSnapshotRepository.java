package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.PriceSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {

    Optional<PriceSnapshot> findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(String ticker, String exchange);

    List<PriceSnapshot> findTop2BySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(String ticker, String exchange);

    List<PriceSnapshot> findTop20ByOrderByObservedAtDesc();
}
