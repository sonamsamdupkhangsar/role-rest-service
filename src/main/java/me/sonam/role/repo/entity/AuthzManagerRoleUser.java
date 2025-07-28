package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

public class AuthzManagerRoleUser implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID authzManagerRoleId;
    private UUID userId;
    @Transient
    private boolean isNew;

    public AuthzManagerRoleUser(UUID id, UUID authzManagerRoleId, UUID userId) {
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
    }

    public AuthzManagerRoleUser() {

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

    public UUID getAuthzManagerRoleId() {
        return authzManagerRoleId;
    }

    @Override
    public String toString() {
        return "AuthzManagerRoleUser{" +
                "id=" + id +
                ", authzManagerRoleId=" + authzManagerRoleId +
                ", userId=" + userId +
                ", isNew=" + isNew +
                '}';
    }
}
