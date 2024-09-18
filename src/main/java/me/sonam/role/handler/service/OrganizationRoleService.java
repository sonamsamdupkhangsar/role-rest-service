package me.sonam.role.handler.service;

import me.sonam.role.handler.OrganizationRole;
import me.sonam.role.handler.RoleException;
import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.handler.service.carrier.User;
import me.sonam.role.repo.*;
import me.sonam.role.repo.entity.*;
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
import java.util.Map;
import java.util.UUID;
@Service
public class OrganizationRoleService implements OrganizationRole {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationRoleService.class);

    @Autowired
    private ClientUserRoleRepository clientUserRoleRepository;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RoleOrganizationRepository roleOrganizationRepository;

    @Autowired
    private RoleUserRepository roleUserRepository;

    @Autowired
    private RoleClientOrganizationUserRepository roleClientOrganizationUserRepository;

    @Override
    public Mono<Page<Role>> getOrganizationRoles(UUID organizationId, Pageable pageable) {
        LOG.info("get organization-id {} roles with pageable pageNumber: {}, pageSize: {}, sort: {}",
                organizationId, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Pageable pageable1 = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        LOG.info("construct new pageable: {}", pageable1);
        //cross join didn't work so find by organizationId and get role

        return roleOrganizationRepository.findByOrganizationId(organizationId, pageable1)
                .flatMap(roleOrganization -> {
                    LOG.info("roleOrganization {}", roleOrganization);
                    if (roleOrganization.getRoleId() == null) {
                        LOG.error("roleOrganization.roleId is null, must be bad data, just delete this row");
                        return roleOrganizationRepository.delete(roleOrganization).then(Mono.empty());
                    }
                    else {
                        return roleRepository.findById(roleOrganization.getRoleId());
                    }
                })
                .collectList()
                .doOnNext(roles -> LOG.info("got roles: {}", roles))
                .zipWith(roleOrganizationRepository.countByOrganizationId(organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<RoleOrganization> addRoleToOrganization(RoleOrganization roleOrganization) {
        LOG.info("add role to organization: {}", roleOrganization);

        return roleOrganizationRepository.deleteByRoleId(roleOrganization.getRoleId())
                .doOnNext(aLong -> {
                    LOG.info("deleted rows: {}", aLong);
                })
                .flatMap(aLong -> roleOrganizationRepository.save(new RoleOrganization(null, roleOrganization.getRoleId(),
                                roleOrganization.getOrganizationId())))
                .flatMap(roleOrganization1 -> {
                    LOG.info("saved roleOrganization: {}", roleOrganization1);
                    roleOrganizationRepository.findByRoleId(roleOrganization1.getRoleId()).doOnNext(roleOrganization2 ->
                            LOG.info("found roleOrganization by roleId: {}", roleOrganization2)).subscribe();
                    return roleOrganizationRepository.findByRoleId(roleOrganization1.getRoleId()).single();
                });
    }

    @Override
    public Mono<String> deleteRoleOrganization(UUID roleId, UUID organizationId) {
        LOG.info("delete roleOrganization by roleId {} and organizationId: {}", roleId, organizationId);

        return roleOrganizationRepository.existsByRoleIdAndOrganizationId(roleId, organizationId)
                .filter(aBoolean -> {
                    LOG.info("exists by roleId: '{}' and organizationId: '{}': {}", roleId, organizationId, aBoolean);
                    return aBoolean;
                })
                .switchIfEmpty(Mono.error(new RoleException("no roleorganization exists with id")))
                .flatMap(aBoolean -> roleOrganizationRepository.deleteByRoleIdAndOrganizationId(roleId, organizationId))
                .thenReturn("roleOrganization deleted");
    }

    @Override
    public Mono<Page<Role>> getUserAssociatedRoles(UUID userId, Pageable pageable) {
        LOG.info("get user roles");

        return roleRepository.findRolesForUserId(userId, pageable)
                .collectList()
                .zipWith(roleUserRepository.countByUserId(userId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Page<Role>> getRolesByUserId(UUID userId, Pageable pageable) {
        LOG.info("get roles by userId: {}", userId);

        return roleRepository.findByUserId(userId, pageable)
                .doOnNext(role -> {
                    LOG.info("found role: {}", role);
                            roleOrganizationRepository.findByRoleId(role.getId())
                                    .single().doOnNext(role::setRoleOrganization);
                        }

                )
                .collectList()
                .zipWith(roleRepository.countByUserId(userId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Role> getRoleById(UUID id) {
        LOG.info("get role by id: {}", id);

        LOG.info("find roleOrganization by roleId: {}", id);
        return roleRepository.findById(id)
                .flatMap(role ->
                    roleOrganizationRepository.findByRoleId(role.getId())
                                    .switchIfEmpty(Mono.just(new RoleOrganization()))
                                    .doOnNext(roleOrganization -> {
                                        if (roleOrganization.getId() != null) {
                                            role.setRoleOrganization(roleOrganization);
                                        }
                                    }).then(Mono.just(role))
                        );
    }

    @Override
    public Mono<Role> createRole(Map<String, Object> map) {
        LOG.info("create role");

        return createNewRole(map);
    }

    public Mono<Role> createNewRole(Map<String, Object> map) {
        LOG.info("save role with association");

        String roleName = map.get("name").toString();
        if (map.get("userId") == null) {
            LOG.error("userId not found");
            return Mono.error(new RoleException("userId not found"));
        }
        if (map.get("name") == null) {
            LOG.error("role name not found");
            return Mono.error(new RoleException("role name not found"));
        }

        if (map.get("organizationId") == null) {
            UUID ownerUserId = UUID.fromString(map.get("userId").toString());
            LOG.info("userId is present");
            return createRoleForUser(ownerUserId, roleName);
        }
        else {
            if (map.get("organizationId") == null) {
                return Mono.error(new RoleException("organizationId not found"));
            }
            UUID userId = UUID.fromString(map.get("userId").toString());
            UUID orgId = UUID.fromString(map.get("organizationId").toString());
            LOG.info("create role for organization");
            return createRoleForOrganization(orgId, roleName, userId);
        }
    }

    /**
     * This is for creating role under the organization id
     * @param orgId
     * @param roleName
     * @return
     */
    private Mono<Role> createRoleForOrganization(UUID orgId, String roleName, UUID userId) {
        return roleRepository.findByOrganizationIdAndName(orgId, roleName).hasElements()
                .flatMap(aBoolean-> {
                    LOG.info("exists by name: {}, is it true? {}", roleName, aBoolean);
                    if (aBoolean) {
                        return Mono.error(new RoleException("role with name already exists for organizationId"));
                    } else {
                        LOG.info("role does not exist, return false");
                        return Mono.just(false);
                }
            })
             .map(aBoolean -> new Role(null, roleName, userId))
                .flatMap(role -> roleRepository.save(role))
                .flatMap(role -> Mono.just(new RoleOrganization(null, role.getId(), orgId)).zipWith(Mono.just(role)))
                .flatMap(objects -> roleOrganizationRepository.save(objects.getT1()).thenReturn(objects.getT2()));

    }

    /**
     * this is for create role under user id (not associated to a organization)
     * @param ownerUserId
     * @param roleName
     * @return
     */

    private Mono<Role> createRoleForUser(UUID ownerUserId, String roleName) {

        return roleRepository.findByUserIdAndName(ownerUserId, roleName).hasElements()
                .flatMap(aBoolean-> {
                    LOG.info("exists by name: {}, is it true? {}", roleName, aBoolean);
                    if (aBoolean) {
                        return Mono.error(new RoleException("role with name already exists for ownerId"));
                    } else {
                        LOG.info("role does not exist, return false");
                        return Mono.just(false);
                    }
                }) .flatMap(aBoolean -> {
                    var role = new Role(null, roleName, ownerUserId);
                    //Mono<Role> roleMono =
                      return      roleRepository.save(role)
                                    .flatMap(saveRole ->
                                            Mono.just(new RoleUser(null, saveRole.getId(), ownerUserId))
                                                    .zipWith(Mono.just(saveRole))
                                    .flatMap(objects -> roleUserRepository.save(objects.getT1())
                                            .thenReturn(objects.getT2())));
                });
    }

    /**
     * This only saves the Role entity
     * @param
     * @return
     */
    @Override
    public Mono<Role> updateRole(Mono<Role> roleMono) {//Mono<Map<String, Object>> mapMono) {
        LOG.info("update role");

        /*return roleMono.flatMap(role -> roleRepository.save(role).doOnNext(role1 ->
                        role1.setRoleOrganization(role.getRoleOrganization())))
                .flatMap(role1 -> Mono.just(role1.getRoleOrganization()).zipWith(Mono.just(role1)))
                .flatMap(objects -> roleOrganizationRepository.save(objects.getT1()).thenReturn(objects.getT2()));
        */
        return roleMono.flatMap(role -> roleRepository.save(role)
                        .flatMap(savedRole -> {
                            LOG.info("savedRole: {}\n, role: {}", savedRole, role);
                            savedRole.setRoleOrganization(role.getRoleOrganization());
                            return Mono.just(savedRole);
                        }))
                .flatMap(updatedRole -> {
                    if (updatedRole.getRoleOrganization() != null && updatedRole.getRoleOrganization().getOrganizationId() != null) {
                        LOG.info("save roleOrganization: {}", updatedRole.getRoleOrganization());

                        return roleOrganizationRepository.deleteByRoleId(updatedRole.getId())
                                .flatMap(rows ->
                         Mono.just(new RoleOrganization(null, updatedRole.getId(),
                                        updatedRole.getRoleOrganization().getOrganizationId())))
                                .flatMap(roleOrganization ->{
                                    LOG.info("saving roleOrganization: {}", roleOrganization);
                                    return roleOrganizationRepository.save(roleOrganization);
                                })
                                .thenReturn(updatedRole);
                    }
                    else {
                        return Mono.just(updatedRole);
                    }
                });

    }

    @Override
    public Mono<String> deleteRole(UUID roleId) {
        LOG.info("delete role by id");

        LOG.info("delete user association and then delete role");
        return clientUserRoleRepository.deleteByRoleId(roleId).flatMap(integer -> {
            LOG.info("delete by role id in roleUser");
            roleRepository.deleteById(roleId).subscribe();
            LOG.info("delete from roleOrganization and roleUser if there is any association");

            roleOrganizationRepository.deleteByRoleId(roleId).subscribe(unused1 ->
                        LOG.info("deleted roleId in roleOrganization"));
            roleUserRepository.deleteByRoleId(roleId).subscribe(unused -> LOG.info("deleted roleId in roleUser"));

            return Mono.just("deleted roles");
        })                      .thenReturn("role and organization association deleted");
    }

    /**
     * this is a add method for associating a role with a user.
     * Return the roleClientUser object so that updates can use the id.
     */

    @Override
    public Mono<ClientUserRole> addClientUserRole(UUID clientId, UUID roleId, UUID userId) {
        LOG.info("add role user");
        return clientUserRoleRepository.existsByClientIdAndRoleIdAndUserId(clientId, roleId, userId)
                .doOnNext(aBoolean -> LOG.info("exists by clientIdAndRoleIdAndUserId already?: {}", aBoolean))
                .filter(aBoolean -> !aBoolean)
                .switchIfEmpty(Mono.error(
                        new RoleException("There is already row with the roleId, clientId and userId," +
                                " delete existing one or update it.")))
                .map(aBoolean -> {
                    LOG.info("return a role user to be added");
                    return   new ClientUserRole(null,clientId, userId, roleId);

                })
                .flatMap(clientUserRole -> clientUserRoleRepository.save(clientUserRole));
    }

    /**
     * this is for updating existing roleClientUser association
     */
    @Override
    public Mono<String> updateClientUserRole(UUID id, UUID clientId, UUID roleId, UUID userId) {
        LOG.info("update roleClientUser");

        return clientUserRoleRepository.findById(id)
                .switchIfEmpty(Mono.error(new RoleException("no roleClientUser user found with id: "+ id)))
                .map(clientUserRole -> {
                    LOG.info("create roleClientUser");
                    return  new ClientUserRole(clientUserRole.getId(), clientId, userId, roleId);
                })
                .flatMap(clientUserRole -> {
                    LOG.info("save roleUser");
                    return clientUserRoleRepository.save(clientUserRole);
                }).thenReturn("updated role client user with id");
    }

    /**
     * this is for deleting roleClientUser with roleId and userId
     * @param roleId
     * @param userId
     * @return
     */
    @Override
    public Mono<String> deleteClientUserRole(UUID roleId, UUID userId) {
        LOG.info("deleting clientUserRole by userId and roleId");
        return clientUserRoleRepository.deleteByRoleIdAndUserId(roleId, userId).flatMap(rows -> {
            //LOG.info("deleted {} rows by roleId and userId", rows);
            return Mono.just("row deleted");
        });
    }

    /**
     * this gets all the roles associated with clientId
     * @param clientId
     * @param pageable
     * @return
     */

    @Override
    public Mono<Page<ClientUserRole>> getClientUserRolePage(UUID clientId, Pageable pageable) {
        LOG.info("get users assigned to clientId");

        return clientUserRoleRepository.findByClientId(clientId, pageable)
                .flatMap(clientUserRole -> {
                    if (clientUserRole.getRoleId() != null) {
                        return roleRepository.findById(clientUserRole.getRoleId()).flatMap(role -> {
                            clientUserRole.setRoleName(role.getName());
                            return Mono.just(clientUserRole);
                        });
                    }
                    else {
                        return Mono.just(clientUserRole);
                    }
                })
                .collectList()
                .zipWith(clientUserRoleRepository.countByClientId(clientId))

                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    /**
     * get all RoleClientUsers by clientId and userId
     * @param clientId
     * @param userId
     * @return
     */
    @Override
    public Flux<ClientUserRole> getClientUserRoles(UUID clientId, UUID userId) {
        LOG.info("get role for user by clientId {} and userId: {}", clientId, userId);

        return clientUserRoleRepository.findByClientIdAndUserId(clientId, userId)
                .flatMap(clientUserRole -> {
                    LOG.info("found roleUser by clientId {} and userId: {}", clientId, userId);

                            if (clientUserRole.getRoleId() != null) {
                                return roleRepository.findById(clientUserRole.getRoleId())
                                        .flatMap(role -> {
                                            LOG.info("setting roleName: {}", role.getName());
                                            clientUserRole.setRoleName(role.getName());
                                            return Mono.just(clientUserRole);
                                        });
                            } else {
                                LOG.info("roleId is null just retrun roleUser: {}", clientUserRole);
                                return Mono.just(clientUserRole);
                            }
                        }
                ).switchIfEmpty(Mono.error(new RoleException("no roleuser found by clientId and userId")));
    }

    @Override
    public Mono<List<ClientOrganizationUserWithRole>> getClientOrganizationUserWithRoles(UUID clientId, UUID orgId, List<UUID> userUuids) {
        LOG.info("get roles by clientId, orgId, and userIds matching from the userIds list");

        return roleClientOrganizationUserRepository.findByClientIdAndOrganizationIdAndUserIdIn(clientId, orgId, userUuids)
               // .switchIfEmpty(Mono.error(new RoleException("No role assigned found for clientId, organizationId and userIds"))) //if no rows are found
                .switchIfEmpty(Flux.just())
                .flatMap(roleClientOrganizationUser -> {
                    LOG.info("roleClientOrganizationUser: {}", roleClientOrganizationUser);
                    return roleRepository.findById(roleClientOrganizationUser.getRoleId())
                        .zipWith(Mono.just(new ClientOrganizationUserWithRole(roleClientOrganizationUser.getId(),
                                roleClientOrganizationUser.getClientId(),
                                roleClientOrganizationUser.getOrganizationId(), new User(roleClientOrganizationUser.getUserId(),null))));})
                .doOnNext(objects -> {
                    LOG.info("objects: {}", objects);
                    objects.getT2().getUser().setRole(objects.getT1());
                })
                .map(Tuple2::getT2)
                .collectList();

    }


    @Override
    public Mono<ClientOrganizationUserRole> addClientOrganizationUserRole(UUID clientId, UUID orgId, UUID roleId, UUID userId) {
        LOG.info("add role by clientId, orgId, and userId");

        return roleClientOrganizationUserRepository.save(new ClientOrganizationUserRole(null, roleId, clientId, orgId, userId));
    }

    @Override
    public Mono<String> deleteClientOrganizationUserRoleById(UUID id) {
        LOG.info("delete by roleClientUserUser by id: {}", id);

        return roleClientOrganizationUserRepository.deleteById(id).thenReturn("roleClientOrganizationUser deleted");
    }

    /**
     * this is the implementation to delete all rows related to the logged-in user by user-id, part of delete my info.
     * delete role created by logged-in user, then delete all roleOrganization and roleUser by role.id
     * @return response
     */
    @Override
    public Mono<String> deleteMyRole() {
        LOG.info("delete my role");
        return ReactiveSecurityContextHolder.getContext().flatMap(securityContext -> {
            LOG.info("principal: {}", securityContext.getAuthentication().getPrincipal());
            org.springframework.security.core.Authentication authentication = securityContext.getAuthentication();

            LOG.info("authentication: {}", authentication);
            LOG.info("authentication.principal: {}", authentication.getPrincipal());
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userIdString = jwt.getClaim("userId");
            LOG.info("delete user data for userId: {}", userIdString);

            UUID userId = UUID.fromString(userIdString);

           return roleRepository.findByUserId(userId).flatMap(role -> {
               LOG.info("found role: {}", role);

               return roleUserRepository.deleteByRoleId(role.getId())
                       .then(roleOrganizationRepository.deleteByRoleId(role.getId()))
                       .then(clientUserRoleRepository.deleteByRoleId(role.getId()))
                       .then(roleClientOrganizationUserRepository.deleteByRoleId(role.getId()))
                       .then(clientUserRoleRepository.deleteByRoleId(role.getId()))
                       .then(roleRepository.deleteById(role.getId()));

           }).collectList()
                   .thenReturn("delete my role success for user id: " + userId);
        });
    }
}


