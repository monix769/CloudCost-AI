package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public String getFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? email : combined;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            this.firstName = "";
            this.lastName = "";
            return;
        }
        String trimmed = fullName.trim();
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx > 0) {
            this.firstName = trimmed.substring(0, spaceIdx).trim();
            this.lastName = trimmed.substring(spaceIdx + 1).trim();
        } else {
            this.firstName = trimmed;
            this.lastName = "";
        }
    }

    public void setRole(UserRole userRole) {
        this.role = userRole != null ? userRole.name() : "ROLE_USER";
    }

    public void setRole(String role) {
        this.role = role != null ? role : "ROLE_USER";
    }

    public UserRole getUserRole() {
        return UserRole.fromString(this.role);
    }
}
