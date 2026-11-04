package com.cloudcostai.cloud.provider;

import com.cloudcostai.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCapabilities {
    private CloudProvider provider;
    private String providerName;
    private String supportedApi;
    private String reportingLag;
    private List<String> requiredPermissions;
    private List<String> supportedDimensions;
    private boolean liveSupported;
    private String documentationUrl;
    private String authType;
}
