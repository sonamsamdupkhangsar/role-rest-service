package me.sonam.role.handler;

import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import me.sonam.role.security.InternalServiceAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Internal role inventories and guarded cleanup; global administrative role definitions remain. */
@Service
public class AdminRoleDeletionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AdminRoleDeletionHandler.class);

    private final AuthzManagerRoleRepository authzManagerRoleRepository;
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository;
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository;
    private final RoleRepository roleRepository;
    private final InternalServiceAuthorization internalServiceAuthorization;
    private final boolean protectionPolicyVerified;
    private final Set<UUID> protectedOrganizationIds;

    public AdminRoleDeletionHandler(
            AuthzManagerRoleRepository authzManagerRoleRepository,
            AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository,
            ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository,
            RoleRepository roleRepository,
            InternalServiceAuthorization internalServiceAuthorization,
            @Value("${account-deletion.protection-policy-configured:false}") boolean configured,
            @Value("${account-deletion.protected-organization-ids:UNCONFIGURED}") String protectedIds) {
        this.authzManagerRoleRepository = authzManagerRoleRepository;
        this.authzManagerRoleAssignmentRepository = authzManagerRoleAssignmentRepository;
        this.clientOrganizationUserRoleRepository = clientOrganizationUserRoleRepository;
        this.roleRepository = roleRepository;
        this.internalServiceAuthorization = internalServiceAuthorization;
        this.protectionPolicyVerified = configured && !"UNCONFIGURED".equals(protectedIds);
        this.protectedOrganizationIds = "UNCONFIGURED".equals(protectedIds) ? Set.of() : Arrays.stream(protectedIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Mono<ServerResponse> accountRoles(ServerRequest request) {
        return internalServiceAuthorization.requireInternalServiceIdentity()
                .then(Mono.defer(() -> inspectUserAdminRoles(extractId(request, "userId"))))
                .flatMap(this::json);
    }

    /** Removes only the owner's assignments before OAuth client deletion; role definitions remain. */
    public Mono<ServerResponse> deleteClientAssignments(ServerRequest request) {
        return internalServiceAuthorization.requireInternalServiceIdentity()
                .then(Mono.defer(() -> {
                    UUID userId = extractId(request, "userId");
                    UUID organizationId = extractId(request, "organizationId");
                    UUID clientId = extractId(request, "clientId");

                    if (!protectionPolicyVerified || protectedOrganizationIds.contains(organizationId)) {
                        return Mono.error(conflict("Organization protection prevents client-role cleanup"));
                    }

                    return inspectUserAdminRoles(userId)
                            .flatMap(accountRoles -> {
                                if (accountRoles.hasSubdomainAdminResponsibilities()
                                        || accountRoles.hasUnresolvedAdminResponsibilities()
                                        || !accountRoles.organizationIds().contains(organizationId)) {
                                    return Mono.error(conflict("OrgAdmin access without other administrative responsibilities is required"));
                                }

                                return inspectOrganizationRoles(organizationId);
                            })
                            .flatMap(organizationRoles -> {
                                if (!organizationRoles.adminRoleAssignmentsValid()
                                        || !organizationRoles.orgAdminUserIds().equals(Set.of(userId))) {
                                    return Mono.error(conflict("Other or unresolved administrator assignments prevent cleanup"));
                                }

                                return clientOrganizationUserRoleRepository.findByClientId(clientId)
                                        .all(assignment -> userId.equals(assignment.getUserId())
                                                && organizationId.equals(assignment.getOrganizationId()));
                            })
                            .flatMap(exclusiveAssignments -> {
                                if (!exclusiveAssignments) {
                                    return Mono.error(conflict("Client role assignments belong to another user or organization"));
                                }

                                return clientOrganizationUserRoleRepository
                                        .deleteByClientIdAndOrganizationIdAndUserId(clientId, organizationId, userId)
                                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                                HttpStatus.SERVICE_UNAVAILABLE, "Client-role cleanup returned no result")))
                                        .doOnNext(count -> LOG.info("Account cleanup removed {} role assignments for client {} in organization {}",
                                                count, clientId, organizationId))
                                        .then();
                            });
                }))
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> organizationRoles(ServerRequest request) {
        return internalServiceAuthorization.requireInternalServiceIdentity()
                .then(Mono.defer(() -> inspectOrganizationRoles(extractId(request, "organizationId"))))
                .flatMap(this::json);
    }

    public Mono<ServerResponse> clientRelationships(ServerRequest request) {
        return internalServiceAuthorization.requireInternalServiceIdentity()
                .then(Mono.defer(() -> {
                    UUID clientId = extractId(request, "clientId");

                    return clientOrganizationUserRoleRepository.findByClientId(clientId)
                            .collectList()
                            .map(assignments -> new ClientRoles(
                                    clientId,
                                    true,
                                    assignments.stream()
                                            .map(ClientOrganizationUserRole::getOrganizationId)
                                            .collect(Collectors.toSet()),
                                    assignments.stream()
                                            .map(ClientOrganizationUserRole::getUserId)
                                            .collect(Collectors.toSet())));
                }))
                .flatMap(this::json);
    }

    @Transactional
    public Mono<ServerResponse> deleteOrganizationRoles(ServerRequest request) {
        return internalServiceAuthorization.requireInternalServiceIdentity()
                .then(Mono.defer(() -> {
                    UUID userId = extractId(request, "userId");
                    UUID organizationId = extractId(request, "organizationId");

                    if (!protectionPolicyVerified || protectedOrganizationIds.contains(organizationId)) {
                        return Mono.error(conflict("Organization role cleanup is protected or unverified"));
                    }

                    return inspectUserAdminRoles(userId)
                            .flatMap(accountRoles -> {
                                if (accountRoles.hasSubdomainAdminResponsibilities() || accountRoles.hasUnresolvedAdminResponsibilities()
                                        || !accountRoles.organizationIds().contains(organizationId)) {
                                    return Mono.error(conflict("OrgAdmin access without other administrative responsibilities is required"));
                                }

                                return inspectOrganizationRoles(organizationId);
                            })
                            .flatMap(organizationRoles -> {
                                if (!organizationRoles.adminRoleAssignmentsValid() || !organizationRoles.orgAdminUserIds().equals(Set.of(userId))) {
                                    return Mono.error(conflict("Other or unresolved administrator assignments prevent deletion"));
                                }

                                return clientOrganizationUserRoleRepository.findByOrganizationId(organizationId)
                                        .all(assignment -> userId.equals(assignment.getUserId()));
                            })
                            .flatMap(noOtherUsers -> {
                                if (!noOtherUsers) {
                                    return Mono.error(conflict("Other users still have organization role assignments"));
                                }

                                return roleRepository.findByOrganizationId(organizationId)
                                        .concatMap(role -> clientOrganizationUserRoleRepository.findByRoleId(role.getId())
                                                .all(assignment -> organizationId.equals(assignment.getOrganizationId())
                                                        && userId.equals(assignment.getUserId())))
                                        .all(Boolean.TRUE::equals);
                            })
                            .flatMap(noSharedRoles -> {
                                if (!noSharedRoles) {
                                    return Mono.error(conflict("An organization role is shared with other users or organizations"));
                                }

                                return clientOrganizationUserRoleRepository.deleteByOrganizationIdAndUserId(organizationId, userId)
                                        .then(Mono.defer(() -> roleRepository.deleteByOrganizationId(organizationId)))
                                        .then(Mono.defer(() -> authzManagerRoleAssignmentRepository.deleteByScopeTypeAndScopeIdAndUserId(
                                                AuthzManagerRoleAssignment.ORGANIZATION,
                                                organizationId,
                                                userId)))
                                        .then();
                            });
                }))
                .then(ServerResponse.noContent()
                .build());
    }

    /**
     * Inspects one user's administrative responsibilities across all scopes, identifying OrgAdmin,
     * SubdomainAdmin, and unknown or inconsistent assignments. Also collects organization IDs from
     * the user's client-role assignments so account deletion checks cover those relationships.
     * This is read-only and does not by itself authorize deletion.
     */
    private Mono<AccountRoles> inspectUserAdminRoles(UUID userId) {
        return getAdminRoleNamesById()
                .flatMap(adminRoleNamesById -> authzManagerRoleAssignmentRepository.findByUserId(userId)
                        .collectList()
                        .map(assignments -> {
                            Set<UUID> organizationIds = new HashSet<>();
                            boolean subdomainAdmin = false;
                            boolean unresolved = false;

                            for (AuthzManagerRoleAssignment assignment : assignments) {
                                String name = adminRoleNamesById.get(assignment.getAuthzManagerRoleId());
                                boolean organizationAdmin = "OrgAdmin".equals(name)
                                        && AuthzManagerRoleAssignment.ORGANIZATION.equals(assignment.getScopeType())
                                        && assignment.getScopeId() != null;
                                boolean subdomainAdministrator = "SubdomainAdmin".equals(name)
                                        && AuthzManagerRoleAssignment.SUBDOMAIN.equals(assignment.getScopeType())
                                        && assignment.getScopeId() != null;

                                if (organizationAdmin) {
                                    organizationIds.add(assignment.getScopeId());
                                }

                                subdomainAdmin |= subdomainAdministrator;
                                unresolved |= !organizationAdmin && !subdomainAdministrator;
                            }

                            return new AccountRoles(userId, true, protectionPolicyVerified, subdomainAdmin, unresolved, organizationIds, Set.of());
                        }))
                .flatMap(accountRoles -> clientOrganizationUserRoleRepository.findByUserId(userId)
                        .map(ClientOrganizationUserRole::getOrganizationId)
                        .collect(Collectors.toSet())
                        .map(relatedOrganizationIds -> new AccountRoles(
                                userId, true, protectionPolicyVerified,
                                accountRoles.hasSubdomainAdminResponsibilities(),
                                accountRoles.hasUnresolvedAdminResponsibilities(),
                                accountRoles.organizationIds(), relatedOrganizationIds)));
    }

    /**
     * Inspects all users' administrative assignments within one organization, checking that each
     * references OrgAdmin. Collects associated client IDs and checks that organization-owned roles
     * are not referenced by assignments in other organizations. Unlike the user-wide inspection,
     * this validates relationships within a single organization. Does not modify or delete data.
     */
    private Mono<OrganizationRoles> inspectOrganizationRoles(UUID organizationId) {
        return getAdminRoleNamesById()
                .flatMap(adminRoleNamesById -> authzManagerRoleAssignmentRepository
                        .findByScopeTypeAndScopeId(AuthzManagerRoleAssignment.ORGANIZATION, organizationId)
                        .collectList()
                        .map(assignments -> {
                            // The query filters by organization scope, not by role type.
                            // These assignments should reference OrgAdmin; defensively reject a different
                            // or missing role definition so inconsistent data cannot allow deletion.
                            boolean adminRoleAssignmentsValid = assignments.stream()
                                    .allMatch(assignment -> {
                                        UUID adminRoleId = assignment.getAuthzManagerRoleId();
                                        String adminRoleName = adminRoleNamesById.get(adminRoleId);

                                        return "OrgAdmin".equals(adminRoleName);
                                    });

                            // Keep every assigned user ID; invalid role types block deletion separately.
                            Set<UUID> orgAdminUserIds = assignments.stream()
                                        .map(AuthzManagerRoleAssignment::getUserId)
                                        .collect(Collectors.toSet());

                            // Client IDs are populated by the next repository lookup.
                            Set<UUID> clientIds = Set.of();
                            boolean protectedResource = protectedOrganizationIds.contains(organizationId);

                            return new OrganizationRoles(
                                    organizationId,
                                    adminRoleAssignmentsValid,
                                    orgAdminUserIds,
                                    clientIds,
                                    protectedResource);
                        }))
                // Find unique client IDs associated with this organization through client-user-role assignments.
                .flatMap(organizationRoles -> clientOrganizationUserRoleRepository.findByOrganizationId(organizationId)
                        .map(ClientOrganizationUserRole::getClientId)
                        .collect(Collectors.toSet())
                        .map(clientIds -> new OrganizationRoles(
                                organizationId, organizationRoles.adminRoleAssignmentsValid(), organizationRoles.orgAdminUserIds(), clientIds,
                                organizationRoles.protectedResource())))
                .flatMap(organizationRoles -> roleRepository.findByOrganizationId(organizationId)
                        .concatMap(role -> clientOrganizationUserRoleRepository.findByRoleId(role.getId())
                                .all(assignment -> organizationId.equals(assignment.getOrganizationId())))
                        .all(Boolean.TRUE::equals)
                        // Preserve the collected details, but mark role assignments invalid if either
                        // the admin-role check failed or an owned role is referenced by another organization.
                        .map(exclusiveRoles -> new OrganizationRoles(
                                organizationId, organizationRoles.adminRoleAssignmentsValid() && exclusiveRoles,
                                organizationRoles.orgAdminUserIds(), organizationRoles.clientIds(),
                                organizationRoles.protectedResource())));
    }

    private Mono<Map<UUID, String>> getAdminRoleNamesById() {
        return authzManagerRoleRepository.findAll()
                .collectMap(AuthzManagerRole::getId, AuthzManagerRole::getName);
    }

    private UUID extractId(ServerRequest request, String name) {
        try {
            return UUID.fromString(request.pathVariable(name));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid identifier", exception);
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private Mono<ServerResponse> json(Object body) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    public record AccountRoles(
            UUID userId,
            boolean roleInventoryLoaded,
            boolean protectionPolicyVerified,
            boolean hasSubdomainAdminResponsibilities,
            boolean hasUnresolvedAdminResponsibilities,
            Set<UUID> organizationIds,
            Set<UUID> relatedOrganizationIds) {
    }

    /**
     * adminRoleAssignmentsValid requires recognized OrgAdmin assignments and no cross-organization
     * use of organization-owned roles. It does not establish that an administrator exists.
     */
    public record OrganizationRoles(UUID organizationId, boolean adminRoleAssignmentsValid, Set<UUID> orgAdminUserIds,
                                    Set<UUID> clientIds, boolean protectedResource) {
    }

    public record ClientRoles(UUID clientId, boolean clientRoleAssignmentsLoaded, Set<UUID> organizationIds, Set<UUID> userIds) {
    }
}
