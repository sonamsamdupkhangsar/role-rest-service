package me.sonam.role.handler;

import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface OrganizationRole {
    Mono<Role> getRoleById(UUID applicationId);
    Mono<Page<Role>> getOrganizationRoles(UUID organizationId, Pageable pageable);
    Mono<String> createRole(Mono<Map> mapMono);
    Mono<String> updateRole(Mono<Map> mapMono);
    Mono<String> deleteRole(UUID id);

    Mono<String> addRoleUser(Mono<Map> mapMono);
    Mono<String> updateRoleUser(Mono<Map> mapMono);
    Mono<String> deleteRoleUser(UUID roleId, UUID userId);

    Mono<Page<RoleUser>> getRoleUsers(String clientId, Pageable pageable);
    Flux<RoleUser> getRoleForUser(String clientId, UUID userId);

}
