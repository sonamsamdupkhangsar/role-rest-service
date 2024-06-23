package me.sonam.role.repo;

import me.sonam.role.repo.entity.ClientUserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClientUserRoleRepository extends ReactiveCrudRepository<ClientUserRole, UUID> {
    Mono<Integer> deleteByRoleId(UUID roleId);
    Mono<Boolean> existsByClientIdAndRoleIdAndUserId(String clientId, UUID roleId, UUID userId);
    Mono<Integer> deleteByRoleIdAndUserId(UUID roleId, UUID userId);
    Mono<ClientUserRole> findByUserId(UUID userId);
    Flux<ClientUserRole> findByClientId(String clientId, Pageable pageable);
    Mono<Long> countByClientId(String clientId);
    Flux<ClientUserRole> findByClientIdAndUserId(String clientId, UUID userId);
    Mono<Boolean> existsByClientIdAndUserId(String clientId, UUID userId);
}
