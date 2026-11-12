package me.sonam.role.repo;

import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ClientOrganizationUserRoleRepository extends ReactiveCrudRepository<ClientOrganizationUserRole, UUID> {
    Mono<Long> deleteByUserId(UUID userId);

    Flux<ClientOrganizationUserRole> findByOrganizationId(UUID organizationId);

    Flux<ClientOrganizationUserRole> findByClientId(UUID clientId);

    Flux<ClientOrganizationUserRole> findByRoleId(UUID roleId);

    Flux<ClientOrganizationUserRole> findByUserId(UUID userId);

    Flux<ClientOrganizationUserRole> findByClientIdAndOrganizationIdAndUserIdIn(UUID clientId, UUID organizationId, List<UUID> userId);

    Mono<ClientOrganizationUserRole> findByClientIdAndOrganizationIdAndUserId(UUID clientId, UUID organizationId, UUID userId);

    Mono<Long> deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Mono<Long> deleteByClientIdAndOrganizationIdAndUserId(UUID clientId, UUID organizationId, UUID userId);

    Mono<Long> countByOrganizationId(UUID organizationId);

    Mono<Long> countByOrganizationIdAndUserIdNot(UUID organizationId, UUID userId);
}
