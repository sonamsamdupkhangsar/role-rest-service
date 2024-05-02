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
        LOG.info("get organization roles with pageable pageNumber: {}, pageSize: {}, sort: {}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Pageable pageable1 = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        LOG.info("construct new pageable: {}", pageable1);
        //cross join didn't work so find by organizationId and get role

        return roleOrganizationRepository.findByOrganizationId(organizationId, pageable1)
                .switchIfEmpty(Flux.empty())
                .flatMap(roleOrganization -> roleRepository.findById(roleOrganization.getRoleId()))
                .collectList()
                .doOnNext(roles -> LOG.info("got roles: {}", roles))
                .zipWith(roleOrganizationRepository.countByOrganizationId(organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
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
                            LOG.info("add roleOrganization to role if there is");
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
        LOG.info("get role by id");
        return roleRepository.findById(id).
                switchIfEmpty(Mono.error(new RoleException("No role found with id")));
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
     * @param mapMono
     * @return
     */

    @Override
    public Mono<ClientUserRole> addClientUserRole(Mono<Map> mapMono) {
        LOG.info("add role user");
        return mapMono.flatMap(map -> {
            LOG.info("map: {}", map);
            return clientUserRoleRepository.existsByClientIdAndRoleIdAndUserId(
                        map.get("clientId").toString(),
                        UUID.fromString(map.get("roleId").toString()), UUID.fromString(map.get("userId").toString()))
                .doOnNext(aBoolean -> LOG.info("exists by clientIdAndRoleIdAndUserId already?: {}", aBoolean))
                .filter(aBoolean -> !aBoolean)
                .switchIfEmpty(Mono.error(
                        new RoleException("There is already row with the roleId, clientId and userId," +
                                " delete existing one or update it.")))
                .map(aBoolean -> {
                    LOG.info("return a role user to be added");
                    return   new ClientUserRole
                            (null, map.get("clientId").toString(),
                                    UUID.fromString(map.get("userId").toString()),
                                    UUID.fromString(map.get("roleId").toString()));

                })
                .flatMap(clientUserRole -> clientUserRoleRepository.save(clientUserRole));});

                //.flatMap(roleClientUser -> Mono.just("created new role client user row"));
    }

    /**
     * this is for updating existing roleClientUser association
     * @param mapMono
     * @return
     */
    @Override
    public Mono<String> updateClientUserRole(Mono<Map> mapMono) {
        LOG.info("update roleClientUser");

        return mapMono.flatMap(map -> Mono.just(map.get("id").toString())
                            .switchIfEmpty(Mono.error(new RoleException("id not found in update role client user")))
                            //in update the applicationUser with appId and userId must exist
                            .flatMap(s -> {
                                LOG.info("id: {}",s);
                                return clientUserRoleRepository.findById(UUID.fromString(s));}
                            )
                        .switchIfEmpty(Mono.error(new RoleException("no roleClientUser user found with id: "+ map.get("id"))))
                            .map(clientUserRole -> {
                                LOG.info("create roleClientUser");
                              return  new ClientUserRole(clientUserRole.getId(),
                                        map.get("clientId").toString(),
                                        UUID.fromString(map.get("userId").toString()),
                                        UUID.fromString(map.get("roleId").toString()));

                            })
                            .flatMap(clientUserRole -> {
                                LOG.info("save roleUser");
                                return clientUserRoleRepository.save(clientUserRole);
                            }).thenReturn("updated role client user"))
                .thenReturn("updated role client user with id");

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
    public Mono<Page<ClientUserRole>> getClientUserRolePage(String clientId, Pageable pageable) {
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
    public Flux<ClientUserRole> getClientUserRoles(String clientId, UUID userId) {
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
}


