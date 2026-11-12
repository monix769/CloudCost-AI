package com.cloudcostai.repository.projection;

import com.cloudcostai.entity.CloudProvider;
import java.math.BigDecimal;

public interface AccountCostSummary {
    Long getAccountId();
    String getAccountName();
    CloudProvider getProvider();
    BigDecimal getTotalCost();
    Long getRecordCount();
}

