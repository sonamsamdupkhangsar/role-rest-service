package me.sonam.role.repo;

import me.sonam.role.repo.entity.RoleOrganization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleOrganizationRepository extends ReactiveCrudRepository<RoleOrganization, UUID> {

    Mono<Long> deleteByRoleId(UUID roleId);
    Mono<Long> countByOrganizationId(UUID organizationId);
    Flux<RoleOrganization> findByRoleId(UUID roleId);
    Mono<Boolean> existsByRoleId(UUID roleId);
    Mono<Boolean> existsByOrganizationId(UUID organizationId);
    Mono<Boolean> existsByRoleIdAndOrganizationId(UUID roleId, UUID organizationId);
    Mono<Long> deleteByRoleIdAndOrganizationId(UUID roleId, UUID organizationId);
    Flux<RoleOrganization> findByRoleIdAndOrganizationId(UUID roleId, UUID organizationId);
    Flux<RoleOrganization> findByOrganizationId(UUID organizationId, Pageable pageable);
    Flux<RoleOrganization> findByOrganizationId(UUID organizationId);

}
