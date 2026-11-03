package com.cloudcostai.repository.projection;

import com.cloudcostai.entity.CloudProvider;
import java.math.BigDecimal;

public interface ServiceCostSummary {
    String getServiceName();
    CloudProvider getProvider();
    BigDecimal getTotalCost();
    Long getRecordCount();
}
