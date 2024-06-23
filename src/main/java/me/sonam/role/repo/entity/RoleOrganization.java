package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

/**
 * RoleOrganization maintains the Role owner by the organizationId if not owned by class {@link RoleUser}
 */
public class RoleOrganization implements Persistable<UUID> {
    @Id
    private UUID id;

    private UUID roleId;
    private UUID organizationId;
    @Transient
    private boolean isNew;
    public RoleOrganization() {
        this.isNew = true;
    }
    public RoleOrganization(UUID id, UUID roleId, UUID organizationId) {
        if (id != null) {
            this.id = id;
            this.isNew = false;
        }
        else {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        this.roleId = roleId;
        this.organizationId = organizationId;
    }
    @Override
    public UUID getId() {
        return id;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleOrganization that = (RoleOrganization) o;
        return isNew == that.isNew && Objects.equals(id, that.id) && Objects.equals(roleId, that.roleId) && Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, organizationId, isNew);
    }

    @Override
    public String toString() {
        return "RoleOrganization{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", organizationId=" + organizationId +
                ", isNew=" + isNew +
                '}';
    }
}
