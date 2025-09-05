package me.sonam.role.repo;

import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface AuthzManagerRoleOrganizationRepository extends ReactiveCrudRepository<AuthzManagerRoleOrganization, UUID> {
    Mono<Boolean> existsByAuthzManagerRoleIdAndOrganizationIdAndUserId(UUID authzManagerRoleId, UUID organizationId, UUID userId);
    Flux<AuthzManagerRoleOrganization> findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
            UUID authzManagerRoleId, UUID organizationId, UUID userId);
    Flux<AuthzManagerRoleOrganization> findByAuthzManagerRoleIdAndOrganizationId(UUID authzManagerRoleId, UUID organizationId, Pageable pageable);
    Mono<Long> countByAuthzManagerRoleIdAndOrganizationId(UUID authzManagerRoleId, UUID organizationId);
    Flux<AuthzManagerRoleOrganization> findByUserIdInAndAuthzManagerRoleIdAndOrganizationId(List<UUID> userIdList, UUID authzManagerRoleId, UUID organizationId);
    Mono<Boolean> existsByUserIdAndAuthzManagerRoleIdAndOrganizationId(UUID userId, UUID authzManagerRoleId, UUID organizationID);
    Flux<AuthzManagerRoleOrganization> findByUserIdAndAuthzManagerRoleId(UUID userId, UUID authzManagerRoleId, Pageable pageable);
    Mono<Integer> countByUserIdAndAuthzManagerRoleId(UUID userId, UUID authzManagerRoleId);
}
