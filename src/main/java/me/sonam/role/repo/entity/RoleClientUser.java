package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * This is for associating Role with a clientId, userid using roleId
 */
public class RoleClientUser implements Persistable<UUID> {
    @Id
    private UUID id;
    private String clientId;
    private UUID userId;
    private UUID roleId;
    @Transient
    private String roleName;

    @Override
    public String toString() {
        return "RoleClientUser{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", userId=" + userId +
                ", roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", isNew=" + isNew +
                '}';
    }

    public RoleClientUser(UUID id, String clientId, UUID userId, UUID roleId) {
        if (id == null) {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        else {
            this.id = id;
            this.isNew = false;
        }
        this.clientId = clientId;
        this.userId = userId;
        this.roleId = roleId;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getClientId() {
        return clientId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }
    public String getRoleName() {
        return  this.roleName;
    }

    @Transient
    private boolean isNew;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
