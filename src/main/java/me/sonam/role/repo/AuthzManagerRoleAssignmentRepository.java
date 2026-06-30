package me.sonam.role.repo;

import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface AuthzManagerRoleAssignmentRepository extends ReactiveCrudRepository<AuthzManagerRoleAssignment, UUID> {
    Mono<Boolean> existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(
            UUID authzManagerRoleId, UUID userId, String scopeType, UUID scopeId);

    Flux<AuthzManagerRoleAssignment> findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(
            UUID authzManagerRoleId, UUID userId, String scopeType, UUID scopeId);

    Flux<AuthzManagerRoleAssignment> findByAuthzManagerRoleIdAndScopeTypeAndScopeId(
            UUID authzManagerRoleId, String scopeType, UUID scopeId, Pageable pageable);

    Mono<Long> countByAuthzManagerRoleIdAndScopeTypeAndScopeId(UUID authzManagerRoleId, String scopeType, UUID scopeId);

    Flux<AuthzManagerRoleAssignment> findByUserIdInAndAuthzManagerRoleIdAndScopeTypeAndScopeId(
            List<UUID> userIds, UUID authzManagerRoleId, String scopeType, UUID scopeId);

    Flux<AuthzManagerRoleAssignment> findByUserIdAndAuthzManagerRoleIdAndScopeType(
            UUID userId, UUID authzManagerRoleId, String scopeType, Pageable pageable);

    Mono<Integer> countByUserIdAndAuthzManagerRoleIdAndScopeType(
            UUID userId, UUID authzManagerRoleId, String scopeType);

    Mono<Long> deleteByScopeTypeAndScopeId(String scopeType, UUID scopeId);

    Mono<Long> deleteByScopeTypeAndScopeIdAndUserId(String scopeType, UUID scopeId, UUID userId);
}
