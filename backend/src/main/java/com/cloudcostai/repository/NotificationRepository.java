package com.cloudcostai.repository;

import com.cloudcostai.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

    List<Notification> findByOrganizationIdAndIsReadFalseOrderByCreatedAtDesc(Long organizationId);

    long countByOrganizationIdAndIsReadFalse(Long organizationId);

    boolean existsByOrganizationIdAndReferenceTypeAndReferenceId(Long organizationId, String referenceType, Long referenceId);

    Optional<Notification> findByIdAndOrganizationId(Long id, Long organizationId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.organization.id = :organizationId AND n.isRead = false")
    int markAllAsReadForOrganization(@Param("organizationId") Long organizationId, @Param("readAt") Instant readAt);
}

