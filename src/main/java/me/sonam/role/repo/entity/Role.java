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
    private String name;

    @Transient
    private boolean isNew;

    public Role(UUID id,  String name) {
        if (id != null) {
            this.id = id;
            this.isNew = false;
        }
        else {
            this.id = UUID.randomUUID();
            this.isNew = true;
        }
        this.name = name;
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

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isNew=" + isNew +
                '}';
    }
}
