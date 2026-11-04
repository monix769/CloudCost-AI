package com.cloudcostai.service;

import com.cloudcostai.dto.CloudAccountRequest;
import com.cloudcostai.dto.CloudAccountResponse;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.DuplicateResourceException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.mapper.CloudAccountMapper;
import com.cloudcostai.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudAccountService {

    private final CloudAccountRepository cloudAccountRepository;
    private final CloudAccountMapper cloudAccountMapper;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<CloudAccountResponse> getAllAccounts(CloudProvider provider) {
        Organization org = organizationService.getCurrentUserOrganization();
        List<CloudAccount> accounts;
        if (provider != null) {
            accounts = cloudAccountRepository.findByOrganizationIdAndProvider(org.getId(), provider);
        } else {
            accounts = cloudAccountRepository.findByOrganizationId(org.getId());
        }
        return accounts.stream()
                .map(cloudAccountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CloudAccountResponse getAccountById(Long id) {
        CloudAccount account = findAccountEntity(id);
        return cloudAccountMapper.toResponse(account);
    }

    @Transactional
    public CloudAccountResponse createAccount(CloudAccountRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();

        if (cloudAccountRepository.existsByOrganizationIdAndProviderAndAccountId(
                org.getId(), request.getProvider(), request.getAccountId().trim())) {
            throw new DuplicateResourceException("CloudAccount", "accountId", request.getAccountId());
        }

        CloudAccount account = cloudAccountMapper.toEntity(request, org);
        account.setLastSyncedAt(Instant.now());
        CloudAccount saved = cloudAccountRepository.save(account);
        log.info("Registered new cloud account [ID: {}, Provider: {}, Account: {}]",
                saved.getId(), saved.getProvider(), saved.getAccountId());
        return cloudAccountMapper.toResponse(saved);
    }

    @Transactional
    public CloudAccountResponse updateAccount(Long id, CloudAccountRequest request) {
        CloudAccount account = findAccountEntity(id);
        Organization org = account.getOrganization();

        if (cloudAccountRepository.existsByOrganizationIdAndProviderAndAccountIdAndIdNot(
                org.getId(), request.getProvider(), request.getAccountId().trim(), id)) {
            throw new DuplicateResourceException("CloudAccount", "accountId", request.getAccountId());
        }

        cloudAccountMapper.updateEntity(account, request);
        CloudAccount updated = cloudAccountRepository.save(account);
        log.info("Updated cloud account [ID: {}, Name: {}]", updated.getId(), updated.getAccountName());
        return cloudAccountMapper.toResponse(updated);
    }

    @Transactional
    public void deleteAccount(Long id) {
        CloudAccount account = findAccountEntity(id);
        cloudAccountRepository.delete(account);
        log.info("Deleted cloud account [ID: {}]", id);
    }

    @Transactional(readOnly = true)
    public CloudAccount findAccountEntity(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        return cloudAccountRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CloudAccount", "id", id));
    }
}
