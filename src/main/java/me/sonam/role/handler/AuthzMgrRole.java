package me.sonam.role.handler;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *   .andRoute(POST("/authzmanagerroles"), handler::createAuthzManagerRole)
 *                 .andRoute(POST("/authzmanagerroles/users"), handler::assignUserToAuthzManagerRole)
 *                 .andRoute(POST("/authzmanagerroles/users/organizations"), handler::assignOrganizationToAuthzManagerRoleWithUser)
 *                 .andRoute(DELETE("/authzmanagerroles/users/organizations"), handler::deleteUserFromAuthzManagerRoleOrganization)
 *                 .andRoute(DELETE("/authzmanagerroles/users/organizations"), handler::deleteOrganizationFromAuthzManagerRoleOrganization);
 */
public interface AuthzMgrRole {
    Mono<UUID> getAuthzManagerRoleId(String name);
    Mono<AuthzManagerRole> createAuthzManagerRole(String name);
    Mono<AuthzManagerRoleOrganization> assignOrganizationToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID organizationId, UUID userId);
    Mono<String> deleteUserFromAuthzManagerRoleOrganization(UUID authzManagerRoleOrganizationId);
    Mono<Page<UUID>> getUserIdByAuthzManagerRoleIdAndOrgId(UUID authzManagerRoleId, UUID organizationId, Pageable pageable);
    Mono<Map<UUID, UUID>>  areUsersSuperAdminByOrgId(List<UUID> userIdsList, UUID organizationId);
    Mono<Page<UUID>> getSuperAdminOrganizations(Pageable pageable);
    Mono<Integer> getSuperAdminOrganizationsCount();
    Mono<Boolean> isUserSuperAdminByOrgId(UUID userId, UUID organizationId);
    Mono<AuthzManagerRoleOrganization> setUserAsAuthzManagerRoleNameForOrganization(String authzManagerRoleName, UUID organizationId, UUID userId);
}
