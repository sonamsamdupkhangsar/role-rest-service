package me.sonam.role.handler;

import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleOrganization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class Handler {
    private static final Logger LOG = LoggerFactory.getLogger(Handler.class);

    @Autowired
    private OrganizationRole organizationRole;

    public Mono<ServerResponse> getRole(ServerRequest serverRequest) {
        LOG.info("get role");

        return organizationRole.getRoleById(UUID.fromString(serverRequest.pathVariable("id")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get role by id failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getRolesByUserId(ServerRequest serverRequest) {
        LOG.info("get roles by userId");
        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getRolesByUserId(UUID.fromString(serverRequest.pathVariable("userId")), pageable)
                .flatMap(page -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(page))
                .onErrorResume(throwable -> {
                    LOG.error("get roles by owner-id failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }


    /**
     * This is for returning roles for a organizationId
     *
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getOrganizationRoles(ServerRequest serverRequest) {
        LOG.info("get organization roles");
        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getOrganizationRoles(
                        UUID.fromString(serverRequest.pathVariable("organizationId")), pageable)
                .flatMap(s -> {
                    LOG.info("get organizationRoles response  {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("get roles call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * this is for handling request to add role to organization
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> addRoleToOrganization(ServerRequest serverRequest) {
        LOG.info("add role to organization");

        return serverRequest.bodyToMono(RoleOrganization.class).
                flatMap(roleOrganization -> organizationRole.addRoleToOrganization(roleOrganization))
                .flatMap(s -> {
                    LOG.info("add roleToOrganization response  {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("add role to organization failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteRoleOrganization(ServerRequest serverRequest) {
        LOG.info("delete role from organization");

        return organizationRole.deleteRoleOrganization(UUID.fromString(serverRequest.pathVariable("roleId")),
                        UUID.fromString(serverRequest.pathVariable("organizationId")))
                .flatMap(s -> {
                    LOG.info("delete roleOrganization response  {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("message", s));
                })
                .onErrorResume(throwable -> {
                    LOG.error("delete roleOrganization failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * This is for returning roles owned by a userId
     *
     * @param serverRequest
     * @return
     */

    public Mono<ServerResponse> getRolesForUser(ServerRequest serverRequest) {
        LOG.info("get organization roles");
        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getUserAssociatedRoles(
                        UUID.fromString(serverRequest.pathVariable("userId")), pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get user roles call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> createRole(ServerRequest serverRequest) {
        LOG.info("create role");

        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .switchIfEmpty(Mono.error(new RoleException("map is empty")))
                .flatMap(map -> organizationRole.createRole(map))
                .flatMap(role -> {
                            LOG.info("returning created");
                            return ServerResponse.created(URI.create("/roles/" + role.getId()))
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(role);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("create role failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> updateRole(ServerRequest serverRequest) {
        LOG.info("update role");

        return organizationRole.updateRole(serverRequest.bodyToMono(Role.class))
                .flatMap(role -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(role))
                .onErrorResume(throwable -> {
                    LOG.error("update role failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteRole(ServerRequest serverRequest) {
        LOG.info("delete role");

        return organizationRole.deleteRole(UUID.fromString(serverRequest.pathVariable("id")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", s)))
                .onErrorResume(throwable -> {
                    LOG.error("delete role failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> addClientUserRole(ServerRequest serverRequest) {
        LOG.info("add role client user");

        return organizationRole.addClientUserRole(serverRequest.bodyToMono(Map.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        Map.of("message", "created new role client user row",
                                "object", s)))
                .onErrorResume(throwable -> {
                    LOG.error("add role user failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }


    public Mono<ServerResponse> updateClientUserRole(ServerRequest serverRequest) {
        LOG.info("update role client user");

        return organizationRole.updateClientUserRole(serverRequest.bodyToMono(Map.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", s)))
                .onErrorResume(throwable -> {
                    LOG.error("update role user failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteClientUserRole(ServerRequest serverRequest) {
        LOG.info("delete role");

        return organizationRole.deleteClientUserRole(UUID.fromString(serverRequest.pathVariable("roleId")),
                        UUID.fromString(serverRequest.pathVariable("userId")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Pair.of("message", s)))
                .onErrorResume(throwable -> {
                    LOG.error("delete role user failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * get all RoleClientUsers by clientId
     *
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getClientUserRolePage(ServerRequest serverRequest) {
        LOG.info("get role users");

        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getClientUserRolePage(serverRequest.pathVariable("clientId"), pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get role user call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * get RoleClientUsers by clientId and userId
     *
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getClientUserRoles(ServerRequest serverRequest) {
        LOG.info("get role for User");

        return organizationRole.getClientUserRoles(serverRequest.pathVariable("clientId"),
                        UUID.fromString(serverRequest.pathVariable("userId")))
                .collectList()
                .flatMap(s -> {
                    LOG.info("sending response back: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("get role for user call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getRoleClientUsersByClientAndUserId(ServerRequest serverRequest) {
        LOG.info("get organizationId for roleId");

        return organizationRole.getClientUserRoles(serverRequest.pathVariable("clientId"),
                        UUID.fromString(serverRequest.pathVariable("userId")))
                .collectList()
                .flatMap(s -> {
                    LOG.info("sending response back: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("get role for user call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getClientOrganziationUserWithRoles(ServerRequest serverRequest) {
        LOG.info("get user roles by clientId, organizationId and userIds");

        UUID clientId = UUID.fromString(serverRequest.pathVariable("clientId"));
        UUID organizationId = UUID.fromString(serverRequest.pathVariable("organizationId"));
        String userIds = serverRequest.pathVariable("userIds");
        String[] userIdArray = userIds.split(",");
        List<UUID> userIdList = new ArrayList<>();
        for(int i = 0; i < userIdArray.length; i++) {
            userIdList.add(UUID.fromString(userIdArray[i]));
        }

        return organizationRole.getClientOrganizationUserWithRoles(clientId, organizationId, userIdList)
                .flatMap(s -> {
                    LOG.info("sending response back: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("get role for user call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> addClientOrganizationUserRole(ServerRequest serverRequest) {
        LOG.info("add userRoles By ClientIdOrganizationIdUserId");

        return serverRequest.bodyToMono(ClientOrganizationUserWithRole.class)
                .flatMap(clientOrganizationUserWithRole -> organizationRole.addClientOrganizationUserRole(
                                clientOrganizationUserWithRole.getClientId(),
                                clientOrganizationUserWithRole.getOrganizationId(),
                                clientOrganizationUserWithRole.getUser().getRole().getId(),
                                clientOrganizationUserWithRole.getUser().getId()))
                .flatMap(s -> {
                    LOG.info("sending response back: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("add RoleClientOrganizationUser call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteClientOrganizationUserRoleById(ServerRequest serverRequest) {
        LOG.info("delete userRoles By ClientIdOrganizationIdUserId");

        UUID id = UUID.fromString(serverRequest.pathVariable("id"));

        return organizationRole.deleteClientOrganizationUserRoleById(id)
                .flatMap(s -> {
                    LOG.info("deleted RoleClientOrganizationUser, sending response: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("delete RoleClientOrganizationUser by id call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }
}