package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

public class AuthzManagerRoleOrganization implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID authzManagerRoleId;
    private UUID userId;
    private UUID organizationId;
    private UUID authzManagerRoleUserId;

    @Transient
    private boolean isNew;

    public AuthzManagerRoleOrganization(UUID id, UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId) {
        if (id != null) {
            this.id = id;
            this.isNew = false;
        }
        else {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        this.authzManagerRoleId = authzManagerRoleId;
        this.organizationId = organizationId;
        this.userId = userId;
        this.authzManagerRoleUserId = authzManagerRoleUserId;
    }

    public AuthzManagerRoleOrganization() {

    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getAuthzManagerRoleId() {
        return authzManagerRoleId;
    }

    public UUID getAuthzManagerRoleUserId() {
        return authzManagerRoleUserId;
    }

    @Override
    public String toString() {
        return "AuthzManagerRoleOrganization{" +
                "id=" + id +
                ", authzManagerRoleId=" + authzManagerRoleId +
                ", userId=" + userId +
                ", organizationId=" + organizationId +
                ", authzManagerRoleUserId=" + authzManagerRoleUserId +
                ", isNew=" + isNew +
                '}';
    }
}

