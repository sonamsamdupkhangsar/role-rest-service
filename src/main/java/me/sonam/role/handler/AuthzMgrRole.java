package me.sonam.role.handler;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import me.sonam.role.repo.entity.AuthzManagerRoleUser;
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
    Mono<AuthzManagerRole> createAuthzManagerRole(String name);
    Mono<AuthzManagerRoleUser> assignUsertToAuthzManagerRole(UUID authzManagerRoleId, UUID userId);
    Mono<AuthzManagerRoleOrganization> assignOrganizationToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId);
    Mono<String> deleteUserFromAuthzManagerRoleOrganization(UUID authzManagerRoleOrganizationId);
    Mono<Page<UUID>> getUserIdByAuthzManagerRoleIdAndOrgId(UUID authzManagerRoleId, UUID organizationId, Pageable pageable);
    Mono<Map<UUID, Boolean>>  areUsersSuperAdminByOrgId(List<UUID> userIdsList, UUID organizationId);
}
