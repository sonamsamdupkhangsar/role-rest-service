package me.sonam.role.repo;

import me.sonam.role.repo.entity.AuthzManagerRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AuthzManagerRoleRepository extends ReactiveCrudRepository<AuthzManagerRole, UUID> {
    Mono<AuthzManagerRole> findByName(String name);
    Mono<Long> countByName(String name);
    Mono<Boolean> existsByName(String name);
}
