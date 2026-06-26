package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.Etf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtfRepository extends JpaRepository<Etf, Long> {

    Optional<Etf> findByTicker(String ticker);
}
