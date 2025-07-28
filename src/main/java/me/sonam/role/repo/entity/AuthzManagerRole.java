package me.sonam.role.repo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.util.Objects;
import java.util.UUID;

public class AuthzManagerRole implements Persistable<UUID> {
    @Id
    private UUID id;
    private String name;
    @Transient
    private boolean isNew;


    public AuthzManagerRole(UUID id, String name) {
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

    public AuthzManagerRole() {

    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        AuthzManagerRole that = (AuthzManagerRole) object;
        return isNew == that.isNew && Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, isNew);
    }

    @Override
    public String toString() {
        return "AuthzManagerRole{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isNew=" + isNew +
                '}';
    }
}
