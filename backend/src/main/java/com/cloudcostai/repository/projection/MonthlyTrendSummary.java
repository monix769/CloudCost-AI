package com.cloudcostai.repository.projection;

import com.cloudcostai.entity.CloudProvider;
import java.math.BigDecimal;

public interface MonthlyTrendSummary {
    Integer getYear();
    Integer getMonth();
    CloudProvider getProvider();
    BigDecimal getTotalCost();
}
