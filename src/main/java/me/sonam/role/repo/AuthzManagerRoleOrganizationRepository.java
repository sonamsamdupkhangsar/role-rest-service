package me.sonam.role.repo;

import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface AuthzManagerRoleOrganizationRepository extends ReactiveCrudRepository<AuthzManagerRoleOrganization, UUID> {
    Mono<Boolean> existsByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
    UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId);
    Flux<AuthzManagerRoleOrganization> findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
            UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId);
    Flux<AuthzManagerRoleOrganization> findByAuthzManagerRoleIdAndOrganizationId(UUID authzManagerRoleId, UUID organizationId, Pageable pageable);
    Mono<Long> countByAuthzManagerRoleIdAndOrganizationId(UUID authzManagerRoleId, UUID organizationId);
    Flux<AuthzManagerRoleOrganization> findByUserIdInAndAuthzManagerRoleIdAndOrganizationId(List<UUID> userIdList, UUID authzManagerRoleId, UUID organizationId);
}
