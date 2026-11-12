package com.cloudcostai.repository;

import com.cloudcostai.entity.InvitationStatus;
import com.cloudcostai.entity.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, Long> {

    Optional<OrganizationInvitation> findByInvitationToken(String token);

    Optional<OrganizationInvitation> findByIdAndOrganizationId(Long id, Long organizationId);

    List<OrganizationInvitation> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<OrganizationInvitation> findByOrganizationIdAndStatus(Long organizationId, InvitationStatus status);

    boolean existsByOrganizationIdAndEmailAndStatus(Long organizationId, String email, InvitationStatus status);
}

