package com.cloudcostai.repository;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CloudAccountRepository extends JpaRepository<CloudAccount, Long> {

    List<CloudAccount> findByOrganizationId(Long organizationId);

    List<CloudAccount> findByOrganizationIdAndProvider(Long organizationId, CloudProvider provider);

    Optional<CloudAccount> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<CloudAccount> findByOrganizationIdAndProviderAndAccountId(Long organizationId, CloudProvider provider, String accountId);

    boolean existsByOrganizationIdAndProviderAndAccountId(Long organizationId, CloudProvider provider, String accountId);

    boolean existsByOrganizationIdAndProviderAndAccountIdAndIdNot(Long organizationId, CloudProvider provider, String accountId, Long id);
}
