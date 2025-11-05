package me.sonam.role.repo;

import me.sonam.role.repo.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
@Repository
public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {
    Flux<Role> findByOrganizationId(UUID organizationId);
    Flux<Role> findByOrganizationId(UUID organizationId, Pageable pageable);
    Mono<Long> countByOrganizationId(UUID organizationId);
    Mono<Long> deleteByOrganizationId(UUID organizationId);
}
