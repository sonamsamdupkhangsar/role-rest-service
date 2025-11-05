package me.sonam.role.handler;

import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import me.sonam.role.repo.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface RoleManager {
    Mono<Role> getRoleById(UUID id);
    //return all rolws owned by a organizationId
    Mono<Page<Role>> getRolesByOrganizationId(UUID organizationId, Pageable pageable);
    Mono<Role> createRole(Role role);
    Mono<Role> updateRole(Role role);//Mono<Map<String, Object>> mapMono);
    Mono<String> deleteRole(UUID id);
    Mono<List<ClientOrganizationUserWithRole>> getClientOrganizationUserWithRoles(UUID clientId, UUID orgId, List<UUID> userUuids);
    Mono<UUID> getRoleIdForClientOrganizationUser(UUID clientId, UUID orgId, UUID userId);
    Mono<ClientOrganizationUserRole> addClientOrganizationUserRole(UUID clientId, UUID orgId, UUID roleId, UUID userId);
    Mono<String> deleteClientOrganizationUserRoleById(UUID id);
    Mono<String> deleteMyRole(UUID orgId);
    Mono<Long> getCountOfUsersWithUserClientOrganizationRoleByOrgId(UUID orgId);
}
