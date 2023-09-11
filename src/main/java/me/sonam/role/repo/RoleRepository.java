package me.sonam.role.repo;

import me.sonam.role.repo.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {
    Flux<Role> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    Mono<Long> countByOrganizationId(UUID organizationId);
    Mono<Boolean> existsByOrganizationIdAndName(UUID organizationId, String name);
}
