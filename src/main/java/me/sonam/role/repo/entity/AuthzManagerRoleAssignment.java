package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

public class AuthzManagerRoleAssignment implements Persistable<UUID> {
    public static final String ORGANIZATION = "ORGANIZATION";
    public static final String SUBDOMAIN = "SUBDOMAIN";

    @Id
    private UUID id;
    private UUID authzManagerRoleId;
    private UUID userId;
    private String scopeType;
    private UUID scopeId;

    @Transient
    private boolean isNew;

    public AuthzManagerRoleAssignment() {
    }

    public AuthzManagerRoleAssignment(UUID id, UUID authzManagerRoleId, UUID userId, String scopeType, UUID scopeId) {
        if (id != null) {
            this.id = id;
            this.isNew = false;
        }
        else {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        this.authzManagerRoleId = authzManagerRoleId;
        this.userId = userId;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getAuthzManagerRoleId() {
        return authzManagerRoleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public UUID getOrganizationId() {
        return ORGANIZATION.equals(scopeType) ? scopeId : null;
    }

    public UUID getSubdomainId() {
        return SUBDOMAIN.equals(scopeType) ? scopeId : null;
    }

    @Override
    public String toString() {
        return "AuthzManagerRoleAssignment{" +
                "id=" + id +
                ", authzManagerRoleId=" + authzManagerRoleId +
                ", userId=" + userId +
                ", scopeType='" + scopeType + '\'' +
                ", scopeId=" + scopeId +
                ", isNew=" + isNew +
                '}';
    }
}
