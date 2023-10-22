package me.sonam.role.repo;

import me.sonam.role.repo.entity.RoleOrganization;
import me.sonam.role.repo.entity.RoleUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleUserRepository extends ReactiveCrudRepository<RoleUser, UUID> {
    Mono<Void> deleteByRoleId(UUID roleId);
    Mono<Long> countByUserId(UUID userId);
    Mono<Boolean> existsByRoleId(UUID roleId);
    Mono<Boolean> existsByUserId(UUID userId);
    Mono<Boolean> existsByRoleIdAndUserId(UUID roleId, UUID userId);
}
