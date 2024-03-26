package me.sonam.role.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
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

    /**
     * This is for returning roles for a organizationId
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getOrganizationRoles(ServerRequest serverRequest) {
        LOG.info("get organization roles");
        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getOrganizationRoles(
                        UUID.fromString(serverRequest.pathVariable("organizationId")), pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get roles call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * This is for returning roles owned by a userId
     * @param serverRequest
     * @return
     */

    public Mono<ServerResponse> getUserRoles(ServerRequest serverRequest) {
        LOG.info("get organization roles");
        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getUserRoles(
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

        return organizationRole.createRole(serverRequest.bodyToMono(Map.class))
                .flatMap(s -> {
                            Map<String, String> map = new HashMap<>();
                            map.put("id", s);
                            LOG.info("returning created");
                            return ServerResponse.created(URI.create("/roles/" + s))
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("id", s));
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

        return organizationRole.updateRole(serverRequest.bodyToMono(Map.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", s)))
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

    public Mono<ServerResponse> addRoleUser(ServerRequest serverRequest) {
        LOG.info("add role client user");

        return organizationRole.addRoleClientUser(serverRequest.bodyToMono(Map.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(
                        Map.of("message", "created new role client user row",
                                "object", s)))
                .onErrorResume(throwable -> {
                    LOG.error("add role user failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }


    public Mono<ServerResponse> updateRoleUser(ServerRequest serverRequest) {
        LOG.info("update role client user");

        return organizationRole.updateRoleClientUser(serverRequest.bodyToMono(Map.class))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", s)))
                .onErrorResume(throwable -> {
                    LOG.error("update role user failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteRoleUser(ServerRequest serverRequest) {
        LOG.info("delete role");

        return organizationRole.deleteRoleClientUser(UUID.fromString(serverRequest.pathVariable("roleId")),
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
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getRoleClientUsersByClientId(ServerRequest serverRequest) {
        LOG.info("get role users");

        Pageable pageable = Util.getPageable(serverRequest);

        return organizationRole.getRoleClientUsersByClientId(serverRequest.pathVariable("clientId"), pageable)
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get role user call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * get RoleClientUsers by clientId and userId
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getRoleForUsersByClientAndUserId(ServerRequest serverRequest) {
        LOG.info("get role for User");

        return organizationRole.getRoleClientUsersByClientAndUserId(serverRequest.pathVariable("clientId"),
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

}