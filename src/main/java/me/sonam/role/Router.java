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

                //.andRoute(GET("/roles/{id}/organization-id").and(accept(MediaType.APPLICATION_JSON)), handler::getRoleClientUsersByClientAndUserId)
                .andRoute(GET("/roles/organizations/{organizationId}").and(accept(MediaType.APPLICATION_JSON)), handler::getOrganizationRoles) //returns page of roles for organization-id
                .andRoute(POST("/roles/organizations").and(accept(MediaType.APPLICATION_JSON)), handler::addRoleToOrganization)
                .andRoute(DELETE("/roles/{roleId}/organizations/{organizationId}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteRoleOrganization)
                .andRoute(GET("/roles/users/{userId}").and(accept(MediaType.APPLICATION_JSON)), handler::getRolesForUser)  //gets roles for user by user-id
                .andRoute(GET("/roles/user-id/{userId}"), handler::getRolesByUserId)


                .andRoute(GET("/roles/client-users/client-id/{clientId}").and(accept(MediaType.APPLICATION_JSON)), handler::getClientUserRolePage)// get RoleClientUsers by clientId
                //this method is called in authentication-rest-service in authentication
                .andRoute(GET("/roles/client-users/client-id/{clientId}/user-id/{userId}").and(accept(MediaType.APPLICATION_JSON)), handler::getClientUserRoles) // get RoleClientUsers by clientId and userId

                .andRoute(POST("/roles/client-users").and(accept(MediaType.APPLICATION_JSON)), handler::addClientUserRole)
                .andRoute(PUT("/roles/client-users").and(accept(MediaType.APPLICATION_JSON)), handler::updateClientUserRole)
                .andRoute(DELETE("/roles/client-users/role-id/{roleId}/user-id/{userId}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteClientUserRole)

                .andRoute(POST("/roles/client-organization-users").and(accept(MediaType.APPLICATION_JSON)), handler::addClientOrganizationUserRole)
                .andRoute(DELETE("/roles/client-organization-users/{id}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteClientOrganizationUserRoleById)
                .andRoute(GET("/roles/client-organization-users/client-id/{clientId}/organization-id/{organizationId}/user-ids/{userIds}").and(accept(MediaType.APPLICATION_JSON)), handler::getClientOrganziationUserWithRoles)
                .andRoute(DELETE("/roles").and(accept(MediaType.APPLICATION_JSON)), handler::deleteMyRole)
                .andRoute(POST("/roles/authzmanagerroles").and(accept(MediaType.APPLICATION_JSON)), handler::createAuthzManagerRole)
                .andRoute(PUT("/roles/authzmanagerroles/name").and(accept(MediaType.APPLICATION_JSON)), handler::getAuthzManagerRoleIdForName)
                .andRoute(POST("/roles/authzmanagerroles/users/organizations"), handler::assignOrganizationToAuthzManagerRoleWithUser)
                .andRoute(DELETE("/roles/authzmanagerroles/users/organizations/{id}"), handler::deleteUserFromAuthzManagerRoleOrganization)
                .andRoute(GET("/roles/authzmanagerroles/{authzManagerRoleId}/users/organizations/{organizationId}"), handler::getAuthzManagerRoleByOrgId)
                .andRoute(PUT("/roles/authzmanagerroles/users/organizations/{organizationId}"), handler::areUsersSuperAdminInDefaultOrgId)
                .andRoute(GET("/roles/authzmanagerroles/users/{userId}/organizations/{organizationId}"), handler::isSuperAdminInDefaultOrgId)
                .andRoute(GET("/roles/authzmanagerroles/users/organizations"), handler::getSuperAdminOrganizations)
                .andRoute(GET("/roles/authzmanagerroles/users/organizations/count"), handler::getSuperAdminOrganizationsCount);




    }
}
