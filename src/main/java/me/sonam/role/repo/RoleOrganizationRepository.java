package me.sonam.role.repo;

import me.sonam.role.repo.entity.RoleOrganization;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleOrganizationRepository extends ReactiveCrudRepository<RoleOrganization, UUID> {

    Mono<Void> deleteByRoleId(UUID roleId);
    Mono<Long> countByOrganizationId(UUID organizationId);
    Mono<Boolean> existsByRoleId(UUID roleId);
    Mono<Boolean> existsByOrganizationId(UUID organizationId);
    Mono<Boolean> existsByRoleIdAndOrganizationId(UUID roleId, UUID organizationId);
}
