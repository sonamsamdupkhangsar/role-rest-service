package me.sonam.role.handler.service;

import me.sonam.role.handler.RoleManager;
import me.sonam.role.handler.RoleException;
import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.handler.service.carrier.User;
import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import me.sonam.role.repo.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.UUID;
@Service
public class RoleManagerService implements RoleManager {
    private static final Logger LOG = LoggerFactory.getLogger(RoleManagerService.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository;

    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;

    @Override
    public Mono<Page<Role>> getRolesByOrganizationId(UUID organizationId, Pageable pageable) {
        LOG.info("get organization-id {} roles with pageable pageNumber: {}, pageSize: {}, sort: {}",
                organizationId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Pageable pageable1 = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        LOG.info("construct new pageable: {}", pageable1);
        //cross join didn't work so find by organizationId and get role

        return roleRepository.findByOrganizationId(organizationId, pageable)
                .collectList()
                .doOnNext(roles -> LOG.info("got roles: {}", roles))
                .zipWith(roleRepository.countByOrganizationId(organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Role> getRoleById(UUID id) {
        LOG.info("get role by id: {}", id);

        return roleRepository.findById(id)
                .switchIfEmpty(Mono.error(new RoleException("No role found with id "+ id)));
    }

    @Override
    public Mono<Role> createRole(Role role) {
        LOG.info("create role {}", role);

        Role role2 = new Role(null, role.getName(), role.getOrganizationId());
        return roleRepository.save(role2).flatMap(Mono::just);
    }

    /**
     * This only saves the Role entity
     * @param role to save
     * @return role object
     */
    @Override
    public Mono<Role> updateRole(Role role) {
        LOG.info("update role: {}", role);

        role.setNew(false);
        return roleRepository.save(role).flatMap(Mono::just);
    }

    @Override
    public Mono<String> deleteRole(UUID id) {
        LOG.info("delete role by id: {}", id);

        return roleRepository.deleteById(id).thenReturn("role deleted");
    }

    @Override
    public Mono<List<ClientOrganizationUserWithRole>> getClientOrganizationUserWithRoles(UUID clientId, UUID orgId, List<UUID> userUuids) {
        LOG.info("get roles by clientId, orgId, and userIds matching from the userIds list");

        return clientOrganizationUserRoleRepository.findByClientIdAndOrganizationIdAndUserIdIn(clientId, orgId, userUuids)
               // .switchIfEmpty(Mono.error(new RoleException("No role assigned found for clientId, organizationId and userIds"))) //if no rows are found
                .switchIfEmpty(Flux.just())
                .flatMap(roleClientOrganizationUser -> {
                    LOG.info("roleClientOrganizationUser: {}", roleClientOrganizationUser);
                    return roleRepository.findById(roleClientOrganizationUser.getRoleId())
                        .zipWith(Mono.just(new ClientOrganizationUserWithRole(roleClientOrganizationUser.getId(),
                                roleClientOrganizationUser.getClientId(),
                                roleClientOrganizationUser.getOrganizationId(), new User(roleClientOrganizationUser.getUserId()), null)));})
                .doOnNext(objects -> {
                    LOG.info("objects: {}", objects);

                    objects.getT2().setRole(objects.getT1());
                })
                .map(Tuple2::getT2)
                .collectList();

    }

    @Override
    public Mono<UUID> getRoleIdForClientOrganizationUser(UUID clientId, UUID orgId, UUID userId) {
        LOG.info("get role id for clientId, organizationId and userId tuple");

        return clientOrganizationUserRoleRepository.findByClientIdAndOrganizationIdAndUserId(clientId, orgId, userId)
                 .switchIfEmpty(Mono.error(new RoleException("No role assigned found for clientId, organizationId and userId"))) //if no rows are found
                .map(ClientOrganizationUserRole::getRoleId);
    }

    @Override
    public Mono<ClientOrganizationUserRole> addClientOrganizationUserRole(UUID clientId, UUID orgId, UUID roleId, UUID userId) {
        LOG.info("add role by clientId, orgId, and userId");

        ClientOrganizationUserRole clientOrganizationUserRole = new ClientOrganizationUserRole(null, roleId, clientId, orgId, userId);
        return clientOrganizationUserRoleRepository.save(clientOrganizationUserRole)
                .then(clientOrganizationUserRoleRepository.findById(clientOrganizationUserRole.getId()));
    }

    @Override
    public Mono<String> deleteClientOrganizationUserRoleById(UUID id) {
        LOG.info("delete by roleClientUserUser by id: {}", id);

        return clientOrganizationUserRoleRepository.deleteById(id).thenReturn("roleClientOrganizationUser deleted");
    }

    /**
     * this is the implementation to delete all rows related to the logged-in user by user-id, part of delete my info.
     * delete role created by logged-in user, then delete all roleOrganization and roleUser by role.id
     * @return response
     */
    @Override
    public Mono<String> deleteMyRole(UUID orgId) {
        LOG.info("delete my role for orgId: {}", orgId);

        return ReactiveSecurityContextHolder.getContext().flatMap(securityContext -> {
            LOG.info("principal: {}", securityContext.getAuthentication().getPrincipal());
            org.springframework.security.core.Authentication authentication = securityContext.getAuthentication();

            LOG.info("authentication: {}", authentication);
            LOG.info("authentication.principal: {}", authentication.getPrincipal());
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userIdString = jwt.getClaim("userId");
            LOG.info("delete user data for userId: {}", userIdString);

            UUID userId = UUID.fromString(userIdString);

            return clientOrganizationUserRoleRepository.deleteByOrganizationIdAndUserId(orgId, userId)
                    .flatMap(aLong -> clientOrganizationUserRoleRepository.countByOrganizationId(orgId))
                    .flatMap(aLong -> {
                        StringBuilder stringBuilder = new StringBuilder("clientOrganizationUserRole deleted with this orgId and userId");

                        if (aLong > 0) {
                            LOG.info(stringBuilder.toString());
                            return Mono.just(stringBuilder);
                        }
                        else {
                            stringBuilder.append(",deleting role");
                            LOG.info(stringBuilder.toString());
                            return roleRepository.deleteByOrganizationId(orgId).thenReturn(stringBuilder);
                        }
                    })
                    .flatMap(message -> {
                        message.append(", delete my role success for orgId: '").append(orgId)
                                .append("' and userId: '").append(userId).append("'");
                        return authzManagerRoleOrganizationRepository.deleteByOrganizationIdAndUserId(orgId, userId)
                                .thenReturn(message.toString());
                    });
        });

        /*
        return roleRepository.findByOrganizationId(orgId)
                .flatMap(role -> {
                    LOG.info("found role: {}", role);
                    return clientOrganizationUserRoleRepository.deleteByRoleId(role.getId()) //delete the role assigned to client-id, user, organization
                            .then(roleRepository.deleteById(role.getId()));
           }).collectList()
                .then(authzManagerRoleOrganizationRepository.deleteByOrganizationId(orgId))//delete authzManagerRole assigned to org and user
                .thenReturn("delete my role success for org id: " + orgId);*/
    }

    @Override
    public Mono<Long> getCountOfUsersWithUserClientOrganizationRoleByOrgId(UUID orgId) {
        LOG.info("get a count of roles for organization {}", orgId);

        return ReactiveSecurityContextHolder.getContext().flatMap(securityContext -> {
            LOG.info("principal: {}", securityContext.getAuthentication().getPrincipal());
            org.springframework.security.core.Authentication authentication = securityContext.getAuthentication();

            LOG.info("authentication: {}", authentication);
            LOG.info("authentication.principal: {}", authentication.getPrincipal());
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userIdString = jwt.getClaim("userId");
            LOG.info("delete user data for userId: {}", userIdString);

            UUID userId = UUID.fromString(userIdString);
            return clientOrganizationUserRoleRepository.countByOrganizationIdAndUserIdNot(orgId, userId);
        });
    }
}


