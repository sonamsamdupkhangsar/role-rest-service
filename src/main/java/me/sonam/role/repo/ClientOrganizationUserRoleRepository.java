package me.sonam.role.repo;

import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ClientOrganizationUserRoleRepository extends ReactiveCrudRepository<ClientOrganizationUserRole, UUID> {

    Flux<ClientOrganizationUserRole> findByClientIdAndOrganizationIdAndUserIdIn(UUID clientId, UUID organizationId, List<UUID> userId);
    Mono<ClientOrganizationUserRole> findByClientIdAndOrganizationIdAndUserId(UUID clientId, UUID organizationId, UUID userId);
    Mono<Integer> deleteByRoleId(UUID roleId);
    Mono<Long> deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    Mono<Long> countByOrganizationId(UUID organizationId);
    Mono<Long> countByOrganizationIdAndUserIdNot(UUID organizationId, UUID userId);
}
