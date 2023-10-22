package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

/**
 * this is for Role ownership by userid if it is not owned by a organization using {@link RoleOrganization}
 */
public class RoleUser implements Persistable<UUID> {
    @Id
    private UUID id;

    private UUID roleId;
    private UUID userId;
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

    public RoleUser(UUID id, UUID roleId, UUID userId) {
        if (id == null) {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        else {
            this.id = id;
            this.isNew = false;
        }
        this.roleId = roleId;
        this.userId = userId;
    }

    public RoleUser() {}

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleUser roleUser = (RoleUser) o;
        return isNew == roleUser.isNew && Objects.equals(id, roleUser.id) && Objects.equals(roleId, roleUser.roleId) && Objects.equals(userId, roleUser.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, userId, isNew);
    }

    @Override
    public String toString() {
        return "RoleUser{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", userId=" + userId +
                ", isNew=" + isNew +
                '}';
    }
}
