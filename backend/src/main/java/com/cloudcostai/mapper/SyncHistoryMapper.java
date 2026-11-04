package com.cloudcostai.mapper;

import com.cloudcostai.dto.SyncHistoryResponse;
import com.cloudcostai.entity.SyncHistory;
import org.springframework.stereotype.Component;

@Component
public class SyncHistoryMapper {

    public SyncHistoryResponse toResponse(SyncHistory entity) {
        if (entity == null) return null;
        return SyncHistoryResponse.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .cloudAccountId(entity.getCloudAccount() != null ? entity.getCloudAccount().getId() : null)
                .accountName(entity.getCloudAccount() != null ? entity.getCloudAccount().getAccountName() : null)
                .provider(entity.getProvider())
                .syncStartTime(entity.getSyncStartTime())
                .syncEndTime(entity.getSyncEndTime())
                .syncStatus(entity.getSyncStatus())
                .recordsFetched(entity.getRecordsFetched())
                .recordsInserted(entity.getRecordsInserted())
                .recordsSkipped(entity.getRecordsSkipped())
                .errorSummary(entity.getErrorSummary())
                .dateRangeStart(entity.getDateRangeStart())
                .dateRangeEnd(entity.getDateRangeEnd())
                .build();
    }
}
