package com.cloudcostai.service;

import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

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

    /**
     * Resolves the current user's organization based on SecurityContext.
     * Falls back to default organization if unauthenticated (e.g. during test executions).
     */
    @Transactional(readOnly = true)
    public Organization getCurrentUserOrganization() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            Long orgId = principal.getOrganizationId();
            if (orgId != null) {
                return organizationRepository.findById(orgId)
                        .orElseGet(this::getDefaultOrganization);
            }
        }
        return getDefaultOrganization();
    }

    /**
     * Resolves an existing organization by name or creates a new one without creating duplicates.
     */
    @Transactional
    public Organization getOrCreateOrganization(String organizationName) {
        if (!StringUtils.hasText(organizationName)) {
            return getDefaultOrganization();
        }

        String trimmedName = organizationName.trim();
        return organizationRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    String baseSlug = trimmedName.toLowerCase(Locale.ROOT)
                            .replaceAll("[^a-z0-9]+", "-")
                            .replaceAll("^-|-$", "");
                    if (baseSlug.isEmpty()) {
                        baseSlug = "org-" + System.currentTimeMillis();
                    }

                    String slug = baseSlug;
                    int suffix = 1;
                    while (organizationRepository.findBySlug(slug).isPresent()) {
                        slug = baseSlug + "-" + suffix++;
                    }

                    log.info("Registering new tenant organization: [{}] with slug: [{}]", trimmedName, slug);
                    Organization newOrg = Organization.builder()
                            .name(trimmedName)
                            .slug(slug)
                            .tier("STANDARD")
                            .isActive(true)
                            .build();
                    return organizationRepository.save(newOrg);
                });
    }
}
