package me.sonam.role.handler.service;

import me.sonam.role.handler.RoleException;
import me.sonam.role.handler.OrganizationRole;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.RoleUserRepository;
import me.sonam.role.repo.entity.Role;
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
    private RoleUserRepository roleUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Mono<Page<Role>> getOrganizationRoles(UUID organizationId, Pageable pageable) {
        LOG.info("get organization roles");

        return roleRepository.findAllByOrganizationId(organizationId, pageable).collectList()
                .zipWith(roleRepository.countByOrganizationId(organizationId))
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

        return createNewRole(mapMono).map(role -> role.getId().toString());
    }

    public Mono<Role> createNewRole(Mono<Map> mapMono) {
        LOG.info("save role with user association, hello");


        return mapMono.switchIfEmpty(Mono.error(new RoleException("map is empty"))).flatMap(map ->
                {
                    LOG.info("map: {}", map);
                    return roleRepository.existsByOrganizationIdAndName(UUID.fromString(
                                    map.get("organizationId").toString()), map.get("name").toString())
                            .flatMap(aBoolean -> {
                                LOG.info("exists by name: {}, true?: {}", map.get("name"), aBoolean);
                                if (aBoolean) {
                                    return Mono.error(new RoleException("role with name already exists for organizationId"));
                                } else {
                                    LOG.info("role does not exist, return false");
                                    return Mono.just(aBoolean);
                                }
                            })


                            .flatMap(aBoolean -> {
                                var role = new Role(null, UUID.fromString(map.get("organizationId").toString()), map.get("name").toString());

                                return roleRepository.save(role);
                            });
                });

    }
    /**
     * @param mapMono
     * @return
     */
    @Override
    public Mono<String> updateRole(Mono<Map> mapMono) {
        LOG.info("update role");

        return mapMono.flatMap(map ->
            roleRepository.save(new Role(UUID.fromString(map.get("id").toString()),
                    UUID.fromString(map.get("organizationId").toString()), map.get("name").toString())))
                .flatMap(role -> Mono.just(role.getId().toString()));
    }

    @Override
    public Mono<String> deleteRole(UUID roleId) {
        LOG.info("delete role by id");

        LOG.info("delete user association and then delete role");
        return roleUserRepository.deleteByRoleId(roleId).then(
            roleRepository.deleteById(roleId).thenReturn("role deleted"));
    }

    @Override
    public Mono<String> updateRoleUser(Flux<Map> mapMono) {
        LOG.info("updated users in organization");

        return mapMono.doOnNext(map -> {
            LOG.info("save role user updates");

            if (map.get("action").equals("add")) {
                LOG.info("add role user");
                roleUserRepository.existsByRoleIdAndUserId(
                                UUID.fromString(map.get("roleId").toString()), UUID.fromString(map.get("userId").toString()))
                        .doOnNext(aBoolean -> LOG.info("exists by roleIdAndUserId already?: {}", aBoolean))
                        .filter(aBoolean -> !aBoolean)

                        .map(aBoolean -> {
                            LOG.info("return a role user to be added");
                         return   new RoleUser
                                    (null, UUID.fromString(map.get("clientId").toString()),
                                            UUID.fromString(map.get("userId").toString()),
                                            UUID.fromString(map.get("roleId").toString()));

                        })
                        .flatMap(roleUser -> roleUserRepository.save(roleUser))
                        .subscribe(organizationUser -> LOG.info("saved roleUser"));

            } else if (map.get("action").equals("delete")) {
                if (map.get("id") != null) {
                    roleUserRepository.existsById(UUID.fromString(map.get("id").toString()))
                            .filter(aBoolean -> aBoolean)
                            .map(aBoolean -> roleUserRepository.deleteById(UUID.fromString(map.get("id").toString())))
                            .subscribe(organizationUser -> LOG.info("deleted roleUser"));
                }
                else {
                    LOG.info("deleting using userId and roleId");
                    roleUserRepository.deleteByRoleIdAndUserId(
                            UUID.fromString(map.get("roleId").toString()), UUID.fromString(map.get("userId").toString()))
                            .subscribe(rows -> LOG.info("deleted {} rows by roleId and userId", rows));
                }
            }
            else if (map.get("action").equals("update")) {
                LOG.info("update user {} and role {}", map.get("userId"), map.get("userId"));
                //in update the applicationUser with appId and userId must exist
                roleUserRepository.findByUserId(
                                UUID.fromString(map.get("userId").toString())).switchIfEmpty(Mono.error(new RoleException("no user found")))

                        .map(roleUser ->  new RoleUser(roleUser.getId(),
                                        UUID.fromString(map.get("clientId").toString()),
                                        UUID.fromString(map.get("userId").toString()),
                                        UUID.fromString(map.get("roleId").toString())))
                        .flatMap(roleUser -> {LOG.info("save roleUser"); return roleUserRepository.save(roleUser); })
                        .subscribe(roleUser -> LOG.info("updated roleUser: {}", roleUser));
            }
            else {
                throw new RoleException("UserUpdate action invalid: " + map.get("update"));
            }
        }).then(Mono.just("roleUser update done"));
    }

    @Override
    public Mono<Page<RoleUser>> getRoleUsers(UUID clientId, Pageable pageable) {
        LOG.info("get users assigned to clientId");

        return roleUserRepository.findByClientId(clientId, pageable)
                .flatMap(roleUser -> {
                    if (roleUser.getRoleId() != null) {
                        return roleRepository.findById(roleUser.getRoleId()).flatMap(role -> {
                            roleUser.setRoleName(role.getName());
                            return Mono.just(roleUser);
                        });
                    }
                    else {
                        return Mono.just(roleUser);
                    }
                })
                .collectList()
                .zipWith(roleUserRepository.countByClientId(clientId))

                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<RoleUser> getRoleForUser(UUID clientId, UUID userId) {
        LOG.info("get role for user by clientId and userId");

        return roleUserRepository.findByClientIdAndUserId(clientId, userId)
                .flatMap(roleUser -> {
                            if (roleUser.getRoleId() != null) {
                                return roleRepository.findById(roleUser.getRoleId())
                                        .flatMap(role -> {
                                            roleUser.setRoleName(role.getName());
                                            return Mono.just(roleUser);
                                        });
                            } else {
                                return Mono.just(roleUser);
                            }
                        }
                );
    }
}
