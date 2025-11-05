package me.sonam.role.handler;

import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.repo.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
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
    private RoleManager roleManager;

    @Autowired
    private AuthzMgrRole authzMgrRole;

    public Mono<ServerResponse> getRole(ServerRequest serverRequest) {
        LOG.info("get role");

        return roleManager.getRoleById(UUID.fromString(serverRequest.pathVariable("id")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s))
                .onErrorResume(throwable -> {
                    LOG.error("get role by id failed, error: {}", throwable.getMessage());
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
    public Mono<ServerResponse> getRolesByOrganizationId(ServerRequest serverRequest) {
        LOG.info("get organization roles");
        Pageable pageable = Util.getPageable(serverRequest);

        return roleManager.getRolesByOrganizationId(
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


    public Mono<ServerResponse> createRole(ServerRequest serverRequest) {
        LOG.info("create role");

        return serverRequest.bodyToMono(Role.class)
                .switchIfEmpty(Mono.error(new RoleException("map is empty")))
                .flatMap(role -> roleManager.createRole(role))
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

        return serverRequest.bodyToMono(Role.class)
                .flatMap(role -> roleManager.updateRole(role))
                .flatMap(role -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(role))
                .onErrorResume(throwable -> {
                    LOG.error("role update failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteRole(ServerRequest serverRequest) {
        LOG.info("delete role");

        return roleManager.deleteRole(UUID.fromString(serverRequest.pathVariable("id")))
                .flatMap(s -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", s)))
                .onErrorResume(throwable -> {
                    LOG.error("delete role failed: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getClientOrganziationUserWithRoles(ServerRequest serverRequest) {
        LOG.info("get user roles by clientId, organizationId and userIds");

        UUID clientId = UUID.fromString(serverRequest.pathVariable("clientId"));
        UUID organizationId = UUID.fromString(serverRequest.pathVariable("organizationId"));

        return serverRequest.bodyToMono(String.class)
                .switchIfEmpty(Mono.error(new RoleException("no userId csv found")))
                .flatMap(userIdCsv -> {
                    String[] userIdArray = userIdCsv.split(",");
                    List<UUID> userIdList = new ArrayList<>();
                    for(int i = 0; i < userIdArray.length; i++) {
                        userIdList.add(UUID.fromString(userIdArray[i]));
                    }
                    LOG.info("userIdList {}", userIdList);
                    return Mono.just(userIdList);
                })
                .flatMap(userIdList -> roleManager.getClientOrganizationUserWithRoles(clientId, organizationId, userIdList))
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

    /**
     * This is similar to {@link #getClientOrganziationUserWithRoles(ServerRequest)} method except just return a roleId.
     * This will be called during authentication to verify user has a role organizationId, clientId and for userId tuple.
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getRoleIdForClientOrganziationUser(ServerRequest serverRequest) {
        LOG.info("get user roles by clientId, organizationId and userIds");

        UUID clientId = UUID.fromString(serverRequest.pathVariable("clientId"));
        UUID organizationId = UUID.fromString(serverRequest.pathVariable("organizationId"));
        UUID userId = UUID.fromString(serverRequest.pathVariable("userId"));

        return roleManager.getRoleIdForClientOrganizationUser(clientId, organizationId, userId)
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
                .switchIfEmpty(Mono.error(new RoleException("no clientOrganizationUserWithRole payload found")))
                .flatMap(clientOrganizationUserWithRole -> roleManager.addClientOrganizationUserRole(
                                clientOrganizationUserWithRole.getClientId(),
                                clientOrganizationUserWithRole.getOrganizationId(),
                                clientOrganizationUserWithRole.getRole().getId(),
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

        return roleManager.deleteClientOrganizationUserRoleById(id)
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

    /**
     * this is for deleting all roles related to a user, part of delete my info request
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> deleteMyRole(ServerRequest serverRequest) {
        LOG.info("delete my roles for organizationId: {}", serverRequest.pathVariable("organizationId"));
        UUID orgId = UUID.fromString(serverRequest.pathVariable("organizationId"));

        return roleManager.deleteMyRole(orgId)
                .flatMap(s -> {
                    LOG.info("delete my role success, sending response: {}", s);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(s);
                })
                .onErrorResume(throwable -> {
                    LOG.error("delete my role call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }


    public Mono<ServerResponse> getAuthzManagerRoleIdForName(ServerRequest serverRequest) {
        LOG.info("get AuthzManagerRole id for name");

        return serverRequest.bodyToMono(String.class)
                .switchIfEmpty(Mono.error(new RoleException("authzManagerRole name is empty")))
                .flatMap(authzManagerRoleName -> authzMgrRole.getAuthzManagerRoleId(authzManagerRoleName))
                .flatMap(id -> {
                            LOG.info("returning authzManagerRoleId {}", id);
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of("message", id));
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("failed to get id for authzManagerRole name, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> createAuthzManagerRole(ServerRequest serverRequest) {
        LOG.info("create AuthzManagerRole");

        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .switchIfEmpty(Mono.error(new RoleException("map is empty")))
                .flatMap(map -> authzMgrRole.createAuthzManagerRole(map.get("name")))
                .flatMap(role -> {
                            LOG.info("returning created");
                            return ServerResponse.created(URI.create("/authzmanagerroles/" + role.getId()))
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(role);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("create authzManagerRole failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * This creates a user as with AuthzManagerRole user, For example it can create a user with SuperAdmin role for a organization
     * @param serverRequest
     * @return
     */

    public Mono<ServerResponse> assignOrganizationToAuthzManagerRoleWithUser(ServerRequest serverRequest) {
        LOG.info("create AuthorizationRoleOrganization");

        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .switchIfEmpty(Mono.error(new RoleException("map is empty")))
                .flatMap(map -> {
                    LOG.info("converting input to UUIDs {}", map);
                    UUID authzManagerRoleId = UUID.fromString(map.get("authzManagerRoleId"));
                    UUID userId = UUID.fromString(map.get("userId"));
                    UUID organizationId = UUID.fromString(map.get("organizationId"));

                    return authzMgrRole.assignOrganizationToAuthzManagerRoleWithUser(authzManagerRoleId, organizationId, userId);
                })
                .flatMap(authzManagerRoleOrganization -> {
                            LOG.info("returning assignOrganizationToAuthzManagerRoleWithUser");
                            return ServerResponse.created(URI.create("/authzmanagerroles/users/organizations" + authzManagerRoleOrganization.getId()))
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(authzManagerRoleOrganization);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("create assignOrganizationToAuthzManagerRoleWithUser failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * This method is similar to above method {{@link #assignOrganizationToAuthzManagerRoleWithUser(ServerRequest)}} except
     * this can take the authzManagerRoleName as String and assign user with that role to a orgId
     * @param serverRequest get user payload
     * @return authzManagerRoleOrganization object
     */
    public Mono<ServerResponse> setUserAsAuthzManagerRoleForOrganization(ServerRequest serverRequest) {
        LOG.info("handler for setting user as authzManagerRole for org");

        return serverRequest.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
                })
                .switchIfEmpty(Mono.error(new RoleException("map is empty")))
                .flatMap(map -> {
                    LOG.info("get payload from map {}", map);
                    String authzManagerRoleName = map.get("authzManagerRoleName");
                    UUID userId = UUID.fromString(map.get("userId"));
                    UUID organizationId = UUID.fromString(map.get("organizationId"));

                    return authzMgrRole.setUserAsAuthzManagerRoleNameForOrganization(authzManagerRoleName, organizationId, userId);
                })
                .flatMap(authzManagerRoleOrganization -> {
                            LOG.info("user has been set as authzManagerRoleName for organization");
                            return ServerResponse.created(URI.create("/authzmanagerroles/users/organizations" + authzManagerRoleOrganization.getId()))
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(authzManagerRoleOrganization);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("failed to set user as authzManagerRoleName for orgId, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> deleteUserFromAuthzManagerRoleOrganization(ServerRequest serverRequest) {
        LOG.info("delete user from AuthzManagerRoleOrganization using pathVariable id {}", serverRequest.pathVariable("id"));
        UUID authzManagerRoleOrganizationId = UUID.fromString(serverRequest.pathVariable("id"));

        return authzMgrRole.deleteUserFromAuthzManagerRoleOrganization(authzManagerRoleOrganizationId)
                .flatMap(role -> {
                            LOG.info("authzManagerRoleOrganization deleted");
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of("message", "User removed from AuthzManagerRoleOrganization"));
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("deleteUserFromAuthzManagerRoleOrganization failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getAuthzManagerRoleByOrgId(ServerRequest serverRequest) {
        LOG.info("get AuthzManagerRoleUser by orgId pathVariable {}", serverRequest.pathVariable("organizationId"));
        UUID organizationId = UUID.fromString(serverRequest.pathVariable("organizationId"));
        UUID authzManagerRoleId = UUID.fromString(serverRequest.pathVariable("authzManagerRoleId"));

        Pageable pageable = Util.getPageable(serverRequest);


        return authzMgrRole.getUserIdByAuthzManagerRoleIdAndOrgId(authzManagerRoleId, organizationId, pageable)
                .flatMap(uuidPage -> {
                            LOG.info("authzManagerRoleOrganization deleted");
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(uuidPage);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("get AuthzManagerRole by orrgId failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * This is to check if the list of userIds in payload are superAdmin role in this organizationId
     * @param serverRequest
     * @return Pair of userId and superAdmin like <UserId, SuperAdmin> or <UserId, EMPTY>
     */
    public Mono<ServerResponse> areUsersSuperAdminInDefaultOrgId(ServerRequest serverRequest) {
        LOG.info("areUsersSuperAdminInDefaultOrgId {}", serverRequest.pathVariable("organizationId"));
        UUID organizationId = UUID.fromString(serverRequest.pathVariable("organizationId"));

        return serverRequest.bodyToMono(new ParameterizedTypeReference<List<UUID>>() {
                })
                .switchIfEmpty(Mono.error(new RoleException("user id List is empty")))
                .flatMap(list -> authzMgrRole.areUsersSuperAdminByOrgId(list, organizationId))
                .flatMap(role -> {
                            LOG.info("returning created");
                            return ServerResponse.created(URI.create("/authzmanagerroles/users"))
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(role);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("areUsersSuperAdminInDefaultOrgId failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    /**
     * This method return a boolean if the userId has the role of "SuperAdmin" in organizationId
     * @param serverRequest  request object
     * @return boolean to indicate if is a SuperAdmin or not
     */
    public Mono<ServerResponse> isSuperAdminInOrgId(ServerRequest serverRequest) {
        LOG.info("extract orgId and userId from serverRequest");
        UUID organizationId = UUID.fromString(serverRequest.pathVariable("organizationId"));
        UUID userId = UUID.fromString(serverRequest.pathVariable("userId"));

        return authzMgrRole.isUserSuperAdminByOrgId(userId, organizationId)
                .flatMap(value -> {
                            LOG.info("is user superAdmin? : {}", value);
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of("message", value));
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("isUserSuperAdminByOrgId call failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }



    public Mono<ServerResponse> getSuperAdminOrganizations(ServerRequest serverRequest) {
        LOG.info("get super admin organizations for logged-in user");
        Pageable pageable = Util.getPageable(serverRequest);

        return  authzMgrRole.getSuperAdminOrganizations(pageable)
                .flatMap(uuidPage -> {
                            LOG.info("get super admin organizations");
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(uuidPage);
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("getSuperAdminOrganizations for logged-in user failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getSuperAdminOrganizationsCount(ServerRequest serverRequest) {
        LOG.info("get super admin organizations count for logged-in user");

        return  authzMgrRole.getSuperAdminOrganizationsCount()
                .flatMap(count -> {
                            LOG.info("returning count of superadmin organizations count");
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", count));
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("getSuperAdminOrganizations count for logged-in user failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }

    public Mono<ServerResponse> getOrganizationWithRoleCount(ServerRequest serverRequest) {
        LOG.info("get a count of user in this organization with role and superAdmin roles");

        UUID orgId = UUID.fromString(serverRequest.pathVariable("organizationId"));

        return  roleManager.getCountOfUsersWithUserClientOrganizationRoleByOrgId(orgId)
                .flatMap(count -> {
                            LOG.info("returning count of items with orgId with client-organization-user-roles");
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON).bodyValue(Map.of("message", count));
                        }
                )
                .onErrorResume(throwable -> {
                    LOG.error("getOrganizationWithRoleCount for orgId failed, error: {}", throwable.getMessage());
                    return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", throwable.getMessage()));
                });
    }
}