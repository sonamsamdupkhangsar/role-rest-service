package me.sonam.role.handler.service;

import me.sonam.role.handler.OrganizationRole;
import me.sonam.role.handler.RoleException;
import me.sonam.role.repo.RoleOrganizationRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.RoleClientUserRepository;
import me.sonam.role.repo.RoleUserRepository;
import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleOrganization;
import me.sonam.role.repo.entity.RoleClientUser;
import me.sonam.role.repo.entity.RoleUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
@Service
public class OrganizationRoleService implements OrganizationRole {
    private static final Logger LOG = LoggerFactory.getLogger(OrganizationRoleService.class);

    @Autowired
    private RoleClientUserRepository roleClientUserRepository;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RoleOrganizationRepository roleOrganizationRepository;

    @Autowired
    private RoleUserRepository roleUserRepository;

    @Override
    public Mono<Page<Role>> getOrganizationRoles(UUID organizationId, Pageable pageable) {
        LOG.info("get organization roles");

        return roleRepository.findByOrganizationId(organizationId, pageable)
                .collectList()
                .zipWith(roleOrganizationRepository.countByOrganizationId(organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));

    }

    @Override
    public Mono<Page<Role>> getUserRoles(UUID userId, Pageable pageable) {
        LOG.info("get user roles");

        return roleRepository.findByUserId(userId, pageable)
                .collectList()
                .zipWith(roleUserRepository.countByUserId(userId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Role> getRoleById(UUID id) {
        LOG.info("get role by id");
        return roleRepository.findById(id).
                switchIfEmpty(Mono.error(new RoleException("No role found with id")));
    }

    @Override
    public Mono<String> createRole(Mono<Map> mapMono) {
        LOG.info("create role");

        return createNewRole(mapMono).map(roleId -> roleId.toString());
    }

    public Mono<UUID> createNewRole(Mono<Map> mapMono) {
        LOG.info("save role with association");

        return mapMono.switchIfEmpty(Mono.error(new RoleException("map is empty"))).flatMap(map ->
                {
                    String roleName = map.get("name").toString();

                    if (map.get("organizationId") == null) {
                        UUID ownerUserId = UUID.fromString(map.get("userId").toString());

                        LOG.info("userId is present");
                        return createRoleForUser(ownerUserId, roleName);
                    }
                    else {
                        UUID orgId = UUID.fromString(map.get("organizationId").toString());
                        LOG.info("create role for organization");
                        return createRoleForOrganization(orgId, roleName);
                    }
                });
    }

    /**
     * This is for creating role under the organization id
     * @param orgId
     * @param roleName
     * @return
     */
    private Mono<UUID> createRoleForOrganization(UUID orgId, String roleName) {
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
             .flatMap(aBoolean -> {
                 var role = new Role(null, roleName);
                 Mono<Role> roleMono = roleRepository.save(role);
                 return roleMono.flatMap(role1 -> {
                     var roleOrganization = new RoleOrganization(null, role.getId(), orgId);
                     return roleOrganizationRepository.save(roleOrganization).flatMap(roleOrganization1 ->
                     {
                         LOG.info("saved roleOrganization relationship: {}", roleOrganization1);

                         return Mono.just(roleOrganization1.getRoleId());
                     });
                 });
             });
    }

    /**
     * this is for create role under user id (not associated to a organization)
     * @param ownerUserId
     * @param roleName
     * @return
     */

    private Mono<UUID> createRoleForUser(UUID ownerUserId, String roleName) {
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
                    var role = new Role(null, roleName);
                    Mono<Role> roleMono = roleRepository.save(role);
                    return roleMono.flatMap(role1 -> {
                        LOG.info("role saved, creating roleUser relationship");
                        var roleUser = new RoleUser(null, role.getId(), ownerUserId);
                        return roleUserRepository.save(roleUser).flatMap(roleUser1 ->
                        {
                            LOG.info("saved roleUser relationship: {}", roleUser1);
                            return Mono.just(roleUser1.getRoleId());
                        });
                    });
                });
    }

    /**
     * This only saves the Role entity
     * @param mapMono
     * @return
     */
    @Override
    public Mono<String> updateRole(Mono<Map> mapMono) {
        LOG.info("update role");

        return mapMono.flatMap(map ->
            roleRepository.save(new Role(UUID.fromString(map.get("id").toString()), map.get("name").toString())))
                .flatMap(role -> Mono.just(role.getId().toString()));
    }

    @Override
    public Mono<String> deleteRole(UUID roleId) {
        LOG.info("delete role by id");

        LOG.info("delete user association and then delete role");
        return roleClientUserRepository.deleteByRoleId(roleId).flatMap(integer -> {
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
     * this is a add method for associating a role with a user
     * @param mapMono
     * @return
     */

    @Override
    public Mono<String> addRoleClientUser(Mono<Map> mapMono) {
        LOG.info("add role user");
        return mapMono.flatMap(map -> roleClientUserRepository.existsByClientIdAndUserId(map.get("clientId").toString(),
                        UUID.fromString(map.get("userId").toString()))
                .filter(aBoolean -> !aBoolean)
                .switchIfEmpty(Mono.error(
                        new RoleException("User already has a role associated with clientId," +
                                " delete existing one or update it.")))
                .flatMap(aBoolean -> roleClientUserRepository.existsByClientIdAndRoleIdAndUserId(
                        map.get("clientId").toString(),
                        UUID.fromString(map.get("roleId").toString()), UUID.fromString(map.get("userId").toString())))
                .doOnNext(aBoolean -> LOG.info("exists by clientIdAndRoleIdAndUserId already?: {}", aBoolean))
                .filter(aBoolean -> !aBoolean)

                .map(aBoolean -> {
                    LOG.info("return a role user to be added");
                    return   new RoleClientUser
                            (null, map.get("clientId").toString(),
                                    UUID.fromString(map.get("userId").toString()),
                                    UUID.fromString(map.get("roleId").toString()));

                })
                .flatMap(roleClientUser -> roleClientUserRepository.save(roleClientUser)))
                .flatMap(roleClientUser -> Mono.just("roleUser updated"));
    }

    /**
     * this is for updating existing roleClientUser association
     * @param mapMono
     * @return
     */
    @Override
    public Mono<String> updateRoleClientUser(Mono<Map> mapMono) {
        LOG.info("update roleUser");

        return mapMono.flatMap(map -> {
            LOG.info("update user {} and role {}", map.get("userId"), map.get("userId"));
            //in update the applicationUser with appId and userId must exist
           return roleClientUserRepository.findByUserId(
                            UUID.fromString(map.get("userId").toString())).switchIfEmpty(Mono.error(new RoleException("no user found")))

                    .map(roleClientUser -> new RoleClientUser(roleClientUser.getId(),
                            map.get("clientId").toString(),
                            UUID.fromString(map.get("userId").toString()),
                            UUID.fromString(map.get("roleId").toString())))
                    .flatMap(roleClientUser -> {
                        LOG.info("save roleUser");
                        return roleClientUserRepository.save(roleClientUser);
                    })
                    .flatMap(roleClientUser -> {
                        LOG.info("updated roleUser: {}", roleClientUser);
                        return Mono.just("updated roleUser");
                    });
        });
    }

    /**
     * this is for deleting role user association with a client
     * @param roleId
     * @param userId
     * @return
     */
    @Override
    public Mono<String> deleteRoleClientUser(UUID roleId, UUID userId) {
        LOG.info("deleting roleUser using userId and roleId");
        return roleClientUserRepository.deleteByRoleIdAndUserId(roleId, userId).flatMap(rows -> {
            LOG.info("deleted {} rows by roleId and userId", rows);
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
    public Mono<Page<RoleClientUser>> getRoleUsers(String clientId, Pageable pageable) {
        LOG.info("get users assigned to clientId");

        return roleClientUserRepository.findByClientId(clientId, pageable)
                .flatMap(roleClientUser -> {
                    if (roleClientUser.getRoleId() != null) {
                        return roleRepository.findById(roleClientUser.getRoleId()).flatMap(role -> {
                            roleClientUser.setRoleName(role.getName());
                            return Mono.just(roleClientUser);
                        });
                    }
                    else {
                        return Mono.just(roleClientUser);
                    }
                })
                .collectList()
                .zipWith(roleClientUserRepository.countByClientId(clientId))

                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Flux<RoleClientUser> getRoleForUser(String clientId, UUID userId) {
        LOG.info("get role for user by clientId {} and userId: {}", clientId, userId);

        return roleClientUserRepository.findByClientIdAndUserId(clientId, userId)
                .flatMap(roleClientUser -> {
                    LOG.info("found roleUser by clientId {} and userId: {}", clientId, userId);

                            if (roleClientUser.getRoleId() != null) {
                                return roleRepository.findById(roleClientUser.getRoleId())
                                        .flatMap(role -> {
                                            LOG.info("setting roleName: {}", role.getName());
                                            roleClientUser.setRoleName(role.getName());
                                            return Mono.just(roleClientUser);
                                        });
                            } else {
                                LOG.info("roleId is null just retrun roleUser: {}", roleClientUser);
                                return Mono.just(roleClientUser);
                            }
                        }
                ).switchIfEmpty(Mono.error(new RoleException("no roleuser found by clientId and userId")));
    }
}
