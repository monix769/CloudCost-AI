package com.cloudcostai.repository.projection;

import com.cloudcostai.entity.CloudProvider;
import java.math.BigDecimal;

public interface RegionCostSummary {
    String getRegion();
    CloudProvider getProvider();
    BigDecimal getTotalCost();
    Long getRecordCount();
}

