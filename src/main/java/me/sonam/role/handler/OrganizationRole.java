package me.sonam.role.handler;

import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import me.sonam.role.repo.entity.ClientUserRole;
import me.sonam.role.repo.entity.RoleOrganization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrganizationRole {
    Mono<Role> getRoleById(UUID applicationId);
    //return all rolws owned by a organizationId
    Mono<Page<Role>> getOrganizationRoles(UUID organizationId, Pageable pageable);
    Mono<RoleOrganization> addRoleToOrganization(RoleOrganization roleOrganization);
    Mono<String> deleteRoleOrganization(UUID roleId, UUID organizationId);
    //return all roles owned by a userId
    Mono<Page<Role>> getUserAssociatedRoles(UUID userId, Pageable pageable);
    Mono<Page<Role>> getRolesByUserId(UUID userId, Pageable pageable);
    Mono<Role> createRole(Map<String, Object> map);
    Mono<Role> updateRole(Mono<Role> roleMono);//Mono<Map<String, Object>> mapMono);
    Mono<String> deleteRole(UUID id);

    Mono<ClientUserRole> addClientUserRole(UUID clientId, UUID roleId, UUID userId);
    Mono<String> updateClientUserRole(UUID id, UUID clientId, UUID roleId, UUID userId);
    Mono<String> deleteClientUserRole(UUID roleId, UUID userId);

    Mono<Page<ClientUserRole>> getClientUserRolePage(UUID clientId, Pageable pageable);
    Flux<ClientUserRole> getClientUserRoles(UUID clientId, UUID userId);
    Mono<List<ClientOrganizationUserWithRole>> getClientOrganizationUserWithRoles(UUID clientId, UUID orgId, List<UUID> userUuids);
    Mono<ClientOrganizationUserRole> addClientOrganizationUserRole(UUID clientId, UUID orgId, UUID roleId, UUID userId);
    Mono<String> deleteClientOrganizationUserRoleById(UUID id);

}
