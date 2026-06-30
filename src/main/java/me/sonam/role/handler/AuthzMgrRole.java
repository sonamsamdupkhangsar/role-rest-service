package me.sonam.role.handler;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
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
 *                 .andRoute(DELETE("/authzmanagerroles/users/assignments"), handler::deleteUserFromAuthzManagerRoleAssignment)
 */
public interface AuthzMgrRole {
    Mono<UUID> getAuthzManagerRoleId(String name);
    Mono<AuthzManagerRole> createAuthzManagerRole(String name);
    Mono<AuthzManagerRoleAssignment> assignOrganizationToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID organizationId, UUID userId);
    Mono<AuthzManagerRoleAssignment> assignSubdomainToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID subdomainId, UUID userId);
    Mono<String> deleteUserFromAuthzManagerRoleAssignment(UUID authzManagerRoleAssignmentId);
    Mono<Page<UUID>> getUserIdByAuthzManagerRoleIdAndOrgId(UUID authzManagerRoleId, UUID organizationId, Pageable pageable);
    Mono<Map<UUID, UUID>> areUsersOrgAdminByOrgId(List<UUID> userIdsList, UUID organizationId);
    Mono<Page<UUID>> getOrgAdminOrganizations(Pageable pageable);
    Mono<Integer> getOrgAdminOrganizationsCount();
    Mono<Page<UUID>> getSubdomainAdminSubdomains(Pageable pageable);
    Mono<Integer> getSubdomainAdminSubdomainsCount();
    Mono<Boolean> isUserOrgAdminByOrgId(UUID userId, UUID organizationId);
    Mono<Boolean> isUserSubdomainAdminBySubdomainId(UUID userId, UUID subdomainId);
    Mono<AuthzManagerRoleAssignment> setUserAsAuthzManagerRoleNameForOrganization(String authzManagerRoleName, UUID organizationId, UUID userId);
    Mono<AuthzManagerRoleAssignment> setUserAsAuthzManagerRoleNameForSubdomain(String authzManagerRoleName, UUID subdomainId, UUID userId);
}
