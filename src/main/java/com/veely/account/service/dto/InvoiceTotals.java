package com.veely.account.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InvoiceTotals {

    @Builder.Default
    private final BigDecimal netTotal = BigDecimal.ZERO;

    @Builder.Default
    private final BigDecimal vatTotal = BigDecimal.ZERO;

    @Builder.Default
    private final BigDecimal grossTotal = BigDecimal.ZERO;

    @Builder.Default
    private final BigDecimal withholdingTotal = BigDecimal.ZERO;

    @Builder.Default
    private final BigDecimal payableTotal = BigDecimal.ZERO;
}
