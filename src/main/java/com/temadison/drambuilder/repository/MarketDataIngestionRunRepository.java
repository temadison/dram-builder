package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.MarketDataIngestionRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketDataIngestionRunRepository extends JpaRepository<MarketDataIngestionRun, Long> {

    List<MarketDataIngestionRun> findTop10ByOrderByStartedAtDesc();
}
