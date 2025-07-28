package me.sonam.role.repo;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthzManagerRoleUserRepository extends ReactiveCrudRepository<AuthzManagerRoleUser, UUID> {
    Mono<Boolean> existsByUserId(UUID userId);
    Mono<AuthzManagerRoleUser> findByUserId(UUID userId);
    Mono<Integer> deleteByAuthzManagerRoleIdAndUserId(UUID authzManagerRoleId, UUID userId);
}
