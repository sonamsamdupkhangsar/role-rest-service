package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

/**
 * Roles are associated to a organizationId
 */
public class Role implements Persistable<UUID> {

    @Id
    private UUID id;
    private String name;

    private UUID organizationId;

    @Transient
    private boolean isNew;

    public Role() {}
    public Role(UUID id,  String name, UUID organizationId) {
        if (id != null) {
            this.id = id;
            this.isNew = false;
        }
        else {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        this.name = name;
        this.organizationId = organizationId;
    }
    public String getName() {
        return this.name;
    }
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean value) {
        this.isNew = value;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", organizationId=" + organizationId +
                ", isNew=" + isNew +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) && Objects.equals(name, role.name) && Objects.equals(organizationId, role.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, organizationId);
    }
}
