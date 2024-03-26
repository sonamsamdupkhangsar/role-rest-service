package me.sonam.role.handler;

import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleClientUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface OrganizationRole {
    Mono<Role> getRoleById(UUID applicationId);
    //return all rolws owned by a organizationId
    Mono<Page<Role>> getOrganizationRoles(UUID organizationId, Pageable pageable);
    //return all roles owned by a userId
    Mono<Page<Role>> getUserRoles(UUID userId, Pageable pageable);
    Mono<String> createRole(Mono<Map> mapMono);
    Mono<String> updateRole(Mono<Map> mapMono);
    Mono<String> deleteRole(UUID id);

    Mono<RoleClientUser> addRoleClientUser(Mono<Map> mapMono);
    Mono<String> updateRoleClientUser(Mono<Map> mapMono);
    Mono<String> deleteRoleClientUser(UUID roleId, UUID userId);

    Mono<Page<RoleClientUser>> getRoleClientUsersByClientId(String clientId, Pageable pageable);
    Flux<RoleClientUser> getRoleClientUsersByClientAndUserId(String clientId, UUID userId);

}
