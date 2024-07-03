package me.sonam.role.repo;

import me.sonam.role.repo.entity.ClientUserRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ClientUserRoleRepository extends ReactiveCrudRepository<ClientUserRole, UUID> {
    Mono<Integer> deleteByRoleId(UUID roleId);
    Mono<Boolean> existsByClientIdAndRoleIdAndUserId(UUID clientId, UUID roleId, UUID userId);
    Mono<Integer> deleteByRoleIdAndUserId(UUID roleId, UUID userId);
    Mono<ClientUserRole> findByUserId(UUID userId);
    Flux<ClientUserRole> findByClientId(UUID clientId, Pageable pageable);
    Mono<Long> countByClientId(UUID clientId);
    Flux<ClientUserRole> findByClientIdAndUserId(UUID clientId, UUID userId);
    Mono<Boolean> existsByClientIdAndUserId(UUID clientId, UUID userId);
}
