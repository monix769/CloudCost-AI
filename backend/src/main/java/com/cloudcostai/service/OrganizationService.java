package com.cloudcostai.service;

import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public static final Long DEFAULT_ORG_ID = 1L;

    @Transactional
    public Organization getDefaultOrganization() {
        return organizationRepository.findById(DEFAULT_ORG_ID)
                .orElseGet(() -> organizationRepository.findBySlug("acme-corp")
                        .orElseGet(() -> {
                            log.info("Default organization not found. Initializing 'Acme Cloud Corp'");
                            Organization org = Organization.builder()
                                    .name("Acme Cloud Corp")
                                    .slug("acme-corp")
                                    .tier("ENTERPRISE")
                                    .isActive(true)
                                    .build();
                            return organizationRepository.save(org);
                        }));
    }
}
