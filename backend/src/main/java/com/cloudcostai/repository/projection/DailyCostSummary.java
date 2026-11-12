package com.cloudcostai.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyCostSummary {
    LocalDate getUsageDate();
    BigDecimal getTotalCost();
    Long getRecordCount();
}

