package me.sonam.role.repo;

import me.sonam.role.repo.entity.RoleUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleUserRepository extends ReactiveCrudRepository<RoleUser, UUID> {
    Mono<Integer> deleteByRoleId(UUID roleId);
    Mono<Boolean> existsByRoleIdAndUserId(UUID roleId, UUID userId);
    Mono<Integer> deleteByRoleIdAndUserId(UUID roleId, UUID userId);
    Mono<RoleUser> findByUserId(UUID userId);
    Flux<RoleUser> findByClientId(UUID clientId, Pageable pageable);
    Mono<Long> countByClientId(UUID organizationId);
    Mono<RoleUser> findByClientIdAndUserId(UUID clientId, UUID userId);
}
