package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * Roles are associated to a organizationId
 */
public class Role implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID organizationId;
    private String name;
    @Transient
    private boolean isNew;

    public Role(UUID id, UUID organizationId, String name) {
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
    public UUID getOrganizationId() { return this.organizationId; }
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public String toString() {
        return "ApplicationRole{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", organizationId='"+ organizationId +'\''+
                ", isNew=" + isNew +
                '}';
    }
}
