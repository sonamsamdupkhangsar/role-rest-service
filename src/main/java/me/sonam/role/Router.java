package me.sonam.role;

import me.sonam.role.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

/**
 * Routes for manage role
 */
@Configuration
public class Router {
    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

    @Bean
    public RouterFunction<ServerResponse> route(Handler handler) {
        LOG.info("building router function");
        return RouterFunctions
                .route(POST("/roles").and(accept(MediaType.APPLICATION_JSON)), handler::createRole)
                .andRoute(PUT("/roles").and(accept(MediaType.APPLICATION_JSON)), handler::updateRole)
                .andRoute(GET("/roles/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::getRole)
                .andRoute(DELETE("/roles/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteRole)
                .andRoute(GET("/roles/organizations/{organizationId}").and(accept(MediaType.APPLICATION_JSON)),
                        handler::getRolesByOrganizationId) //returns page of roles for organization-id


                .andRoute(POST("/roles/clients/organizations/users/roles")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::addClientOrganizationUserRole)
//                .andRoute(GET("/roles/client-organization-users/client-id/{clientId}/organization-id/{organizationId}/user-ids/{userIds}")
                .andRoute(PUT("/roles/clients/{clientId}/organizations/{organizationId}/users/roles")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getClientOrganziationUserWithRoles)
                .andRoute(GET("/roles/clients/{clientId}/organizations/{organizationId}/users/{userId}/roles")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getRoleIdForClientOrganziationUser)
                .andRoute(GET("/roles/clients/{clientId}/organizations/{organizationId}/users/{userId}/roles/name")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getRoleNameForClientOrganizationUser)
                .andRoute(DELETE("/roles/clients/organizations/users/roles/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteClientOrganizationUserRoleById)

                .andRoute(DELETE("/roles/organizations/{organizationId}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteMyRole)

                .andRoute(POST("/roles/authzmanagerroles").and(accept(MediaType.APPLICATION_JSON)), handler::createAuthzManagerRole)
                .andRoute(PUT("/roles/authzmanagerroles/name").and(accept(MediaType.APPLICATION_JSON)), handler::getAuthzManagerRoleIdForName)
                .andRoute(POST("/roles/authzmanagerroles/users/organizations"), handler::assignOrganizationToAuthzManagerRoleWithUser)
                .andRoute(POST("/roles/authzmanagerroles/names/users/organizations"), handler::setUserAsAuthzManagerRoleForOrganization)
                .andRoute(DELETE("/roles/authzmanagerroles/users/organizations/{id}"), handler::deleteUserFromAuthzManagerRoleOrganization)
                .andRoute(GET("/roles/authzmanagerroles/{authzManagerRoleId}/users/organizations/{organizationId}"), handler::getAuthzManagerRoleByOrgId)
                .andRoute(PUT("/roles/authzmanagerroles/users/organizations/{organizationId}"), handler::areUsersSuperAdminInDefaultOrgId)
                .andRoute(GET("/roles/authzmanagerroles/users/{userId}/organizations/{organizationId}"), handler::isSuperAdminInOrgId)
                .andRoute(GET("/roles/authzmanagerroles/users/organizations"), handler::getSuperAdminOrganizations)
                .andRoute(GET("/roles/authzmanagerroles/users/organizations/count"), handler::getSuperAdminOrganizationsCount)
                .andRoute(GET("/roles/organizations/{organizationId}/count"), handler::getOrganizationWithRoleCount);




    }
}
