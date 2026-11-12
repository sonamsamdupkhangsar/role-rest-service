package me.sonam.role.handler;

import java.util.UUID;
import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Removes client-role assignments only; clients, role definitions and admin assignments remain. */
@Component
public class OrganizationMemberRoleCleanupHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationMemberRoleCleanupHandler.class);

    private final AuthzManagerRoleRepository authzManagerRoleRepository;
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository;
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository;

    public OrganizationMemberRoleCleanupHandler(AuthzManagerRoleRepository authzManagerRoleRepository,
            AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository,
            ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository) {
        this.authzManagerRoleRepository = authzManagerRoleRepository;
        this.authzManagerRoleAssignmentRepository = authzManagerRoleAssignmentRepository;
        this.clientOrganizationUserRoleRepository = clientOrganizationUserRoleRepository;
    }

    public Mono<ServerResponse> removeMemberClientRoles(ServerRequest request) {
        UUID organizationId = UUID.fromString(request.pathVariable("organizationId"));
        UUID userId = UUID.fromString(request.pathVariable("userId"));

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {
                    if (context.getAuthentication() == null
                            || !(context.getAuthentication().getPrincipal() instanceof Jwt jwt)
                            || jwt.getClaimAsString("userId") == null) {
                        return Mono.just(false);
                    }

                    UUID callerUserId = UUID.fromString(jwt.getClaimAsString("userId"));
                    return authzManagerRoleRepository.findByName("OrgAdmin")
                            .flatMap(role -> authzManagerRoleAssignmentRepository
                                    .existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(
                                            role.getId(), callerUserId,
                                            AuthzManagerRoleAssignment.ORGANIZATION, organizationId));
                })
                .defaultIfEmpty(false)
                .flatMap(allowed -> {
                    if (!allowed) {
                        LOG.warn("Member client-role cleanup denied for organization {}: OrgAdmin access required", organizationId);
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "OrgAdmin access is required to remove member client roles"));
                    }

                    return clientOrganizationUserRoleRepository.deleteByOrganizationIdAndUserId(organizationId, userId)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "Client-role cleanup did not return a result")))
                            .doOnNext(count -> LOG.info("Removed {} client-role assignments for user {} in organization {}",
                                    count, userId, organizationId))
                            .then(ServerResponse.noContent().build());
                });
    }
}
