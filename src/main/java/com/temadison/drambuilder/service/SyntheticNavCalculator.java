package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.HoldingInput;
import java.math.BigDecimal;
import java.util.List;

public interface SyntheticNavCalculator {

    SyntheticNavResult calculate(BigDecimal marketPrice, List<HoldingInput> holdings);
}
