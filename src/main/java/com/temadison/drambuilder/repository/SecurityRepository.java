package com.temadison.drambuilder.repository;

import com.temadison.drambuilder.domain.Security;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityRepository extends JpaRepository<Security, Long> {

    Optional<Security> findByTickerAndExchange(String ticker, String exchange);
}
