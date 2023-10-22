package me.sonam.role.repo;

import me.sonam.role.repo.entity.RoleClientUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleClientUserRepository extends ReactiveCrudRepository<RoleClientUser, UUID> {
    Mono<Integer> deleteByRoleId(UUID roleId);
    Mono<Boolean> existsByClientIdAndRoleIdAndUserId(String clientId, UUID roleId, UUID userId);
    Mono<Integer> deleteByRoleIdAndUserId(UUID roleId, UUID userId);
    Mono<RoleClientUser> findByUserId(UUID userId);
    Flux<RoleClientUser> findByClientId(String clientId, Pageable pageable);
    Mono<Long> countByClientId(String clientId);
    Flux<RoleClientUser> findByClientIdAndUserId(String clientId, UUID userId);
    Mono<Boolean> existsByClientIdAndUserId(String clientId, UUID userId);
}
