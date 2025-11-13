package me.sonam.role.handler;

import jakarta.annotation.PostConstruct;
import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import me.sonam.role.rest.RestPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class AuthzMgrAppRole implements AuthzMgrRole{
    private static final Logger LOG = LoggerFactory.getLogger(AuthzMgrAppRole.class);

    @Autowired
    private AuthzManagerRoleRepository authzManagerRoleRepository;
    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;

   // @PostConstruct
    public void createSuperAdminRole() {
        final String superAdmin = "SuperAdmin";

       // authzManagerRoleRepository.deleteAll().subscribe();

        authzManagerRoleRepository.countByName(superAdmin).flatMap(count -> {
            if (count < 1) {
                LOG.info("creating {} role", superAdmin);
                return authzManagerRoleRepository.save(new AuthzManagerRole(null, "SuperAdmin"))
                        .thenReturn("saved");
            } else {
                LOG.info("{} role already exists", superAdmin);
                return Mono.just("superAdmin role already exists");
            }
        }).subscribe();
    }

    @Override
    public Mono<UUID> getAuthzManagerRoleId(String name) {
        LOG.info("get authzManagerRoleId for roleName {}", name);

        return authzManagerRoleRepository.findByName(name).map(AuthzManagerRole::getId);
    }

    @Override
    public Mono<AuthzManagerRole> createAuthzManagerRole(String name) {
        LOG.info("Create new AuthzManagerRole with name {}", name);
        return authzManagerRoleRepository.existsByName(name).filter(aBoolean -> {
            if (aBoolean) {
                LOG.error("authzManagerRole with name {} already exists", name);
            }
            return !aBoolean;
        })
                .flatMap(aBoolean -> Mono.just(new AuthzManagerRole(null, name)))
                .flatMap(authzManagerRole ->
                        authzManagerRoleRepository.save(authzManagerRole).thenReturn(authzManagerRole));
    }

    /**
     * This is called to transfer the ownership from a user to another user associated to a organization
     * The authzMangaerRoleId is assigned to a AuthzManagerRoleOrganization,
     * this will delete the auzthManagerRoleUser with authzManagerRoleId and userId
     * @param authzManagerRoleId
     * @param organizationId
     * @param userId
     * @return
     */
    @Override
    public Mono<AuthzManagerRoleOrganization> assignOrganizationToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID organizationId, UUID userId) {
        LOG.info("assigning organization role");
        return authzManagerRoleOrganizationRepository.existsByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId)
                .flatMap(aBoolean -> {
                    if(!aBoolean) {
                    LOG.info("create authzRoleManagerUser for authzManagerRoleId {}, userId {}", authzManagerRoleId, userId);
                    var authzManagerRoleOrganization = new AuthzManagerRoleOrganization
                            (null, authzManagerRoleId, organizationId, userId);
                    return authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization)
                                            .thenReturn(authzManagerRoleOrganization);
                    }
                else {
                    LOG.warn("authzManagerRoleUser with userId already exists");
                    return authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                            authzManagerRoleId, organizationId, userId).single();
                }
        });
    }

    @Override
    public Mono<AuthzManagerRoleOrganization> setUserAsAuthzManagerRoleNameForOrganization(String authzManagerRoleName, UUID organizationId, UUID userId) {
        LOG.info("set user.id {} as authzManagerRoleName: {} in organization: {}", authzManagerRoleName, organizationId, userId);
        return authzManagerRoleRepository.findByName(authzManagerRoleName).switchIfEmpty(Mono.error(new RoleException("No authzManagerRole with name "+authzManagerRoleName)))
                .flatMap(authzManagerRole -> authzManagerRoleOrganizationRepository.existsByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                        authzManagerRole.getId(), organizationId, userId).zipWith(Mono.just(authzManagerRole)))
                .flatMap(objects -> {
                    if(!objects.getT1()) {
                        LOG.info("create authzRoleManagerUser for authzManagerRoleId {}, userId {}", objects.getT2().getId(), userId);
                        var authzManagerRoleOrganization = new AuthzManagerRoleOrganization
                                (null, objects.getT2().getId(), organizationId, userId);
                        return authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization)
                                .thenReturn(authzManagerRoleOrganization);
                    }
                    else {
                        LOG.warn("authzManagerRoleUser with userId already exists");
                        return authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                                objects.getT2().getId(), organizationId, userId).single();
                    }
                });
    }

    /**
     * This is called to delete user from authzManagerRoleOrganization
     * @return
     */
    @Override
    public Mono<String> deleteUserFromAuthzManagerRoleOrganization(UUID authzManagerRoleOrganizationId) {
        LOG.info("delete authzManagerRoleOrganization by id {}", authzManagerRoleOrganizationId);

        return authzManagerRoleOrganizationRepository.deleteById(authzManagerRoleOrganizationId)
                .thenReturn("authzManagerRoleOrganizationId deleted");
    }

    /**
     * Only `SuperAdmin` authzManagerRoleId currently supported but allows for flexibility by supporting it as param
     * @param authzManagerRoleId AuthzManagerRole.id
     * @param organizationId Organization.id
     * @param pageable Page of request
     * @return return a flux of AuthzManagerRoleOrganization
     */
    @Override
    public Mono<Page<UUID>> getUserIdByAuthzManagerRoleIdAndOrgId(UUID authzManagerRoleId, UUID organizationId, Pageable pageable) {
        LOG.info("get all users that have SuperAdmin role");

        return authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId, organizationId, pageable)
                .collectList().doOnNext(list -> LOG.info("authzManagerRoleOrganization by authzManagerRoleId {}," +
                        "organizationId: {}", authzManagerRoleId, organizationId))
                .flatMap(list -> {
                    List<UUID> userIds = list.stream().map(AuthzManagerRoleOrganization::getUserId).toList();
                    return Mono.just(userIds);
                })
                .zipWith(authzManagerRoleOrganizationRepository.countByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId, organizationId))
                .map(objects -> new PageImpl<>(objects.getT1(), pageable, objects.getT2()));
    }

    @Override
    public Mono<Map<UUID, UUID>> areUsersSuperAdminByOrgId(List<UUID> userIdsList, UUID organizationId) {
        LOG.info("return a map of userId and SuperAdmin if they are superAdmin in the userIdList: {},\n orgId {}", userIdsList, organizationId);

        return authzManagerRoleRepository.findByName("SuperAdmin")
                .flatMap(authzManagerRole -> Mono.just(authzManagerRole.getId()))
                .flatMap(uuid -> authzManagerRoleOrganizationRepository.
                        findByUserIdInAndAuthzManagerRoleIdAndOrganizationId(userIdsList, uuid, organizationId).collectList())
                .flatMap(list -> {
                    LOG.info("list of userIds with AuthzManagerRoleId and orgId {}", list);
                    Map<UUID, UUID> map = new HashMap<>();
                    userIdsList.forEach(uuid -> {
                        if (!list.contains(uuid)) {
                            map.put(uuid, null);
                        }
                    });
                    LOG.info("found list of UserIds in organization");
                    list.forEach(authzManagerRoleOrganization -> {
                        map.put(authzManagerRoleOrganization.getUserId(), authzManagerRoleOrganization.getId());
                    });
                    return Mono.just(map);
                });
    }

    @Override
    public Mono<Page<UUID>> getSuperAdminOrganizations(Pageable pageable) {
        LOG.info("get super admin organization ids for logged-in user with jwt for pageNumber: {} and pageSize: {}", pageable.getPageNumber(), pageable.getPageSize());

        return ReactiveSecurityContextHolder.getContext().flatMap(securityContext -> {
            LOG.info("principal: {}", securityContext.getAuthentication().getPrincipal());
            Authentication authentication = securityContext.getAuthentication();
            Jwt jwt = (Jwt) authentication.getPrincipal();

            String userIdString = jwt.getClaim("userId");
            LOG.info("userIdString: {}", userIdString);

            UUID userId = UUID.fromString(userIdString);
            LOG.info("userId: {}", userId);

            return authzManagerRoleRepository.findByName("SuperAdmin")
                    .flatMap(authzManagerRole -> Mono.just(authzManagerRole.getId()))
                    .flatMap(authzManagerRoleId -> {
                            LOG.info("authzManagerRole {}", authzManagerRoleId);
                      return authzManagerRoleOrganizationRepository.findByUserIdAndAuthzManagerRoleId(userId,
                              authzManagerRoleId, pageable).collectList().zipWith(Mono.just(authzManagerRoleId));
                    })
                    .flatMap(objects -> authzManagerRoleOrganizationRepository.countByUserIdAndAuthzManagerRoleId(userId,
                            objects.getT2()).zipWith(Mono.just(objects.getT1())))
                    .flatMap(objects -> {
                        int count = objects.getT1();
                        List<AuthzManagerRoleOrganization> list = objects.getT2();

                        List<UUID> organizationIds = new ArrayList<>();
                        list.forEach(authzManagerRoleOrganization -> organizationIds.add(authzManagerRoleOrganization.getOrganizationId()));

                        LOG.info("got count of authzManagerRoleOrganization of userId and superAdmin role: {}", count);
                        Page<UUID> page = new RestPage<>(organizationIds, pageable.getPageNumber(),
                                pageable.getPageSize(), count, list.size(), pageable.getPageNumber());
                        return Mono.just(page);
                    });
            });
    }

    @Override
    public Mono<Boolean> isUserSuperAdminByOrgId(UUID userId, UUID organizationId) {
        LOG.info("check if userId {} is superadmin in organizationId {}", userId, organizationId);

        return authzManagerRoleRepository.findByName("SuperAdmin")
                .flatMap(authzManagerRole -> Mono.just(authzManagerRole.getId()))
                .flatMap(uuid -> authzManagerRoleOrganizationRepository.
                        existsByUserIdAndAuthzManagerRoleIdAndOrganizationId(userId, uuid, organizationId));
    }

    @Override
    public Mono<Integer> getSuperAdminOrganizationsCount() {
        LOG.info("get super admin organization ids count for logged-in user with jwt ");

        return ReactiveSecurityContextHolder.getContext().flatMap(securityContext -> {
            Authentication authentication = securityContext.getAuthentication();
            Jwt jwt = (Jwt) authentication.getPrincipal();

            String userIdString = jwt.getClaim("userId");
            LOG.info("userIdString: {}", userIdString);

            UUID userId = UUID.fromString(userIdString);
            LOG.info("userId: {}", userId);

            return authzManagerRoleRepository.findByName("SuperAdmin")
                    .flatMap(authzManagerRole -> Mono.just(authzManagerRole.getId()))
                    .flatMap(authzManagerRoleId -> {
                        LOG.info("authzManagerRole {}", authzManagerRoleId);
                        return authzManagerRoleOrganizationRepository.countByUserIdAndAuthzManagerRoleId(userId, authzManagerRoleId);
                    });

        });
    }
}
