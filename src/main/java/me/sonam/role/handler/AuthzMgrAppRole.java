package me.sonam.role.handler;

import jakarta.annotation.PostConstruct;
import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import me.sonam.role.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthzMgrAppRole implements AuthzMgrRole {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzMgrAppRole.class);
    private static final String ORG_ADMIN = "OrgAdmin";
    private static final String SUBDOMAIN_ADMIN = "SubdomainAdmin";

    @Autowired
    private AuthzManagerRoleRepository authzManagerRoleRepository;

    @Autowired
    private AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository;

    @PostConstruct
    public void createAdminRoles() {
        createRoleIfMissing(ORG_ADMIN).subscribe();
        createRoleIfMissing(SUBDOMAIN_ADMIN).subscribe();
    }

    @Override
    public Mono<UUID> getAuthzManagerRoleId(String name) {
        LOG.info("get authzManagerRoleId for roleName {}", name);
        return authzManagerRoleRepository.findByName(name).map(AuthzManagerRole::getId);
    }

    @Override
    public Mono<AuthzManagerRole> createAuthzManagerRole(String name) {
        LOG.info("Create new AuthzManagerRole with name {}", name);
        return authzManagerRoleRepository.existsByName(name).filter(aBoolean -> {
                    if (aBoolean) {
                        LOG.error("authzManagerRole with name {} already exists", name);
                    }
                    return !aBoolean;
                })
                .flatMap(aBoolean -> authzManagerRoleRepository.save(new AuthzManagerRole(null, name)));
    }

    @Override
    public Mono<AuthzManagerRoleAssignment> assignOrganizationToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID organizationId, UUID userId) {
        return assignScope(authzManagerRoleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
    }

    @Override
    public Mono<AuthzManagerRoleAssignment> assignSubdomainToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID subdomainId, UUID userId) {
        return assignScope(authzManagerRoleId, userId, AuthzManagerRoleAssignment.SUBDOMAIN, subdomainId);
    }

    @Override
    public Mono<String> deleteUserFromAuthzManagerRoleAssignment(UUID authzManagerRoleAssignmentId) {
        LOG.info("delete authzManagerRoleAssignment by id {}", authzManagerRoleAssignmentId);
        return authzManagerRoleAssignmentRepository.deleteById(authzManagerRoleAssignmentId)
                .thenReturn("authzManagerRoleAssignmentId deleted");
    }

    @Override
    public Mono<Page<UUID>> getUserIdByAuthzManagerRoleIdAndOrgId(UUID authzManagerRoleId, UUID organizationId,
                                                                   Pageable pageable) {
        LOG.info("get all users that have authz manager role {} in organization {}", authzManagerRoleId, organizationId);
        return authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndScopeTypeAndScopeId(
                        authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId, pageable)
                .map(AuthzManagerRoleAssignment::getUserId)
                .collectList()
                .zipWith(authzManagerRoleAssignmentRepository.countByAuthzManagerRoleIdAndScopeTypeAndScopeId(
                        authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Map<UUID, UUID>> areUsersOrgAdminByOrgId(List<UUID> userIdsList, UUID organizationId) {
        LOG.info("return a map of userId and OrgAdmin assignment id for orgId {}", organizationId);
        return roleId(ORG_ADMIN)
                .flatMap(roleId -> authzManagerRoleAssignmentRepository
                        .findByUserIdInAndAuthzManagerRoleIdAndScopeTypeAndScopeId(userIdsList, roleId,
                                AuthzManagerRoleAssignment.ORGANIZATION, organizationId)
                        .collectList())
                .map(assignments -> assignmentMap(userIdsList, assignments));
    }

    @Override
    public Mono<Page<UUID>> getOrgAdminOrganizations(Pageable pageable) {
        LOG.info("get OrgAdmin organization ids for logged-in user");
        return currentUserId().flatMap(userId -> roleId(ORG_ADMIN)
                .flatMap(roleId -> scopeIdsForUser(userId, roleId, AuthzManagerRoleAssignment.ORGANIZATION, pageable)));
    }

    @Override
    public Mono<Integer> getOrgAdminOrganizationsCount() {
        LOG.info("get OrgAdmin organization ids count for logged-in user");
        return currentUserId().flatMap(userId -> roleId(ORG_ADMIN)
                .flatMap(roleId -> authzManagerRoleAssignmentRepository
                        .countByUserIdAndAuthzManagerRoleIdAndScopeType(
                                userId, roleId, AuthzManagerRoleAssignment.ORGANIZATION)));
    }

    @Override
    public Mono<Page<UUID>> getSubdomainAdminSubdomains(Pageable pageable) {
        LOG.info("get SubdomainAdmin subdomain ids for logged-in user");
        return currentUserId().flatMap(userId -> roleId(SUBDOMAIN_ADMIN)
                .flatMap(roleId -> scopeIdsForUser(userId, roleId, AuthzManagerRoleAssignment.SUBDOMAIN, pageable)));
    }

    @Override
    public Mono<Integer> getSubdomainAdminSubdomainsCount() {
        LOG.info("get SubdomainAdmin subdomain ids count for logged-in user");
        return currentUserId().flatMap(userId -> roleId(SUBDOMAIN_ADMIN)
                .flatMap(roleId -> authzManagerRoleAssignmentRepository
                        .countByUserIdAndAuthzManagerRoleIdAndScopeType(
                                userId, roleId, AuthzManagerRoleAssignment.SUBDOMAIN)));
    }

    @Override
    public Mono<Boolean> isUserOrgAdminByOrgId(UUID userId, UUID organizationId) {
        LOG.info("check if userId {} is OrgAdmin in organizationId {}", userId, organizationId);
        return roleId(ORG_ADMIN)
                .flatMap(roleId -> authzManagerRoleAssignmentRepository
                        .existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(roleId, userId,
                                AuthzManagerRoleAssignment.ORGANIZATION, organizationId));
    }

    @Override
    public Mono<Boolean> isUserSubdomainAdminBySubdomainId(UUID userId, UUID subdomainId) {
        LOG.info("check if userId {} is SubdomainAdmin in subdomainId {}", userId, subdomainId);
        return roleId(SUBDOMAIN_ADMIN)
                .flatMap(roleId -> authzManagerRoleAssignmentRepository
                        .existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(roleId, userId,
                                AuthzManagerRoleAssignment.SUBDOMAIN, subdomainId));
    }

    @Override
    public Mono<AuthzManagerRoleAssignment> setUserAsAuthzManagerRoleNameForOrganization(
            String authzManagerRoleName, UUID organizationId, UUID userId) {
        LOG.info("set user.id {} as authzManagerRoleName {} in organization {}", userId, authzManagerRoleName,
                organizationId);
        return roleId(authzManagerRoleName)
                .flatMap(roleId -> assignScope(roleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId));
    }

    @Override
    public Mono<AuthzManagerRoleAssignment> setUserAsAuthzManagerRoleNameForSubdomain(
            String authzManagerRoleName, UUID subdomainId, UUID userId) {
        LOG.info("set user.id {} as authzManagerRoleName {} in subdomain {}", userId, authzManagerRoleName, subdomainId);
        return roleId(authzManagerRoleName)
                .flatMap(roleId -> assignScope(roleId, userId, AuthzManagerRoleAssignment.SUBDOMAIN, subdomainId));
    }

    private Mono<AuthzManagerRole> createRoleIfMissing(String roleName) {
        return authzManagerRoleRepository.countByName(roleName).flatMap(count -> {
            if (count < 1) {
                LOG.info("creating {} role", roleName);
                return authzManagerRoleRepository.save(new AuthzManagerRole(null, roleName));
            }
            LOG.info("{} role already exists", roleName);
            return authzManagerRoleRepository.findByName(roleName);
        });
    }

    private Mono<UUID> roleId(String roleName) {
        return authzManagerRoleRepository.findByName(roleName)
                .switchIfEmpty(Mono.error(new RoleException("No authzManagerRole with name " + roleName)))
                .map(AuthzManagerRole::getId);
    }

    private Mono<AuthzManagerRoleAssignment> assignScope(UUID roleId, UUID userId, String scopeType, UUID scopeId) {
        return authzManagerRoleAssignmentRepository.existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(
                        roleId, userId, scopeType, scopeId)
                .flatMap(exists -> {
                    if (!exists) {
                        var assignment = new AuthzManagerRoleAssignment(null, roleId, userId, scopeType, scopeId);
                        return authzManagerRoleAssignmentRepository.save(assignment).thenReturn(assignment);
                    }
                    return authzManagerRoleAssignmentRepository
                            .findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(roleId, userId, scopeType, scopeId)
                            .single();
                });
    }

    private Mono<Page<UUID>> scopeIdsForUser(UUID userId, UUID roleId, String scopeType, Pageable pageable) {
        return authzManagerRoleAssignmentRepository.findByUserIdAndAuthzManagerRoleIdAndScopeType(
                        userId, roleId, scopeType, pageable)
                .collectList()
                .flatMap(assignments -> authzManagerRoleAssignmentRepository
                        .countByUserIdAndAuthzManagerRoleIdAndScopeType(userId, roleId, scopeType)
                        .map(count -> {
                            List<UUID> scopeIds = new ArrayList<>();
                            assignments.forEach(assignment -> scopeIds.add(assignment.getScopeId()));
                            return new RestPage<>(scopeIds, pageable.getPageNumber(), pageable.getPageSize(), count,
                                    assignments.size(), pageable.getPageNumber());
                        }));
    }

    private Map<UUID, UUID> assignmentMap(List<UUID> userIds, List<AuthzManagerRoleAssignment> assignments) {
        Map<UUID, UUID> map = new HashMap<>();
        userIds.forEach(userId -> map.put(userId, null));
        assignments.forEach(assignment -> map.put(assignment.getUserId(), assignment.getId()));
        return map;
    }

    private Mono<UUID> currentUserId() {
        return ReactiveSecurityContextHolder.getContext().map(securityContext -> {
            Authentication authentication = securityContext.getAuthentication();
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return UUID.fromString(jwt.getClaim("userId"));
        });
    }
}
