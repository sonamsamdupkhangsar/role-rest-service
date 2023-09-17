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
        return RouterFunctions.route(POST("/roles").and(accept(MediaType.APPLICATION_JSON)),
                handler::createRole)
                .andRoute(PUT("/roles")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::updateRole)
                .andRoute(DELETE("/roles/{id}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::deleteRole)
                .andRoute(GET("/roles/organization/{organizationId}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getOrganizationRoles)
                .andRoute(GET("/roles/{id}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getRole)
                //.andRoute(PUT("/roles/users"), handler::updateUser)
                .andRoute(GET("/roles/clientId/{clientId}/users")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getRoleUsers)
                .andRoute(GET("/roles/clientId/{clientId}/users/{userId}")
                        .and(accept(MediaType.APPLICATION_JSON)), handler::getRoleForUser)
                .andRoute(POST("/roles/user").and(accept(MediaType.APPLICATION_JSON)), handler::addRoleUser)
                .andRoute(PUT("/roles/user").and(accept(MediaType.APPLICATION_JSON)), handler::updateRoleUser)
                .andRoute(DELETE("/roles/{roleId}/users/{userId}").and(accept(MediaType.APPLICATION_JSON)), handler::deleteRoleUser);

    }
}
