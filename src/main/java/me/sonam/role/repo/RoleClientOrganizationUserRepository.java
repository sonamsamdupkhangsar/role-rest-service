package me.sonam.role.repo;

import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface RoleClientOrganizationUserRepository extends ReactiveCrudRepository<ClientOrganizationUserRole, UUID> {

    Flux<ClientOrganizationUserRole> findByClientIdAndOrganizationIdAndUserIdIn(UUID clientId, UUID organizationId, List<UUID> userId);
}
