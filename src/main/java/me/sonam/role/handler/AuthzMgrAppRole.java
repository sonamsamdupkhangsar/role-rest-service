package me.sonam.role.handler;

import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.AuthzManagerRoleUserRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import me.sonam.role.repo.entity.AuthzManagerRoleUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthzMgrAppRole implements AuthzMgrRole{
    private static final Logger LOG = LoggerFactory.getLogger(AuthzMgrAppRole.class);

    @Autowired
    private AuthzManagerRoleRepository authzManagerRoleRepository;
    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;
    @Autowired
    private AuthzManagerRoleUserRepository authzManagerRoleUserRepository;

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
     * This method is called to create a AuthzManagerRoleUser
     * @param authzManagerRoleId This is id from class @see{@link AuthzManagerRole#getId()}
     * @param userId This is the userId from class User user-rest-service
     * @return
     */
    @Override
    public Mono<AuthzManagerRoleUser> assignUsertToAuthzManagerRole(UUID authzManagerRoleId, UUID userId) {
        LOG.info("assign user to authzManagerRole.id {}", authzManagerRoleId);

        return authzManagerRoleUserRepository.existsByUserId(userId).flatMap(aBoolean -> {
            if(!aBoolean) {
                LOG.info("create authzRoleManagerUser for authzManagerRoleId {}, userId {}", authzManagerRoleId, userId);
                var authzManagerRoleUser = new AuthzManagerRoleUser(null, authzManagerRoleId, userId);
                return authzManagerRoleUserRepository.save(authzManagerRoleUser).thenReturn(authzManagerRoleUser);
            }
            else {
                LOG.warn("authzManagerRoleUser with userId already exists");
                return authzManagerRoleUserRepository.findByUserId(userId);
            }
        });
    }

    /**
     * This is called to transfer the ownership from a user to another user associated to a organization
     * The authzMangaerRoleId is assigned to a AuthzManagerRoleOrganization,
     * this will delete the auzthManagerRoleUser with authzManagerRoleId and userId
     * @param authzManagerRoleId
     * @param organizationId
     * @param userId
     * @param authzManagerRoleUserId
     * @return
     */
    @Override
    public Mono<AuthzManagerRoleOrganization> assignOrganizationToAuthzManagerRoleWithUser(
            UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId) {
        LOG.info("assigning organization role");
        return authzManagerRoleOrganizationRepository.existsByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                authzManagerRoleId, organizationId, userId, authzManagerRoleUserId)
                .flatMap(aBoolean -> {
                    if(!aBoolean) {
                    LOG.info("create authzRoleManagerUser for authzManagerRoleId {}, userId {}", authzManagerRoleId, userId);
                    var authzManagerRoleOrganization = new AuthzManagerRoleOrganization
                            (null, authzManagerRoleId, organizationId, userId, authzManagerRoleUserId);
                    return authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization)
                            .then(authzManagerRoleUserRepository.deleteByAuthzManagerRoleIdAndUserId(authzManagerRoleId, userId))
                                            .thenReturn(authzManagerRoleOrganization);
                    }
                else {
                    LOG.warn("authzManagerRoleUser with userId already exists");
                    return authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                            authzManagerRoleId, organizationId, userId, authzManagerRoleUserId).single();
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
    public Mono<Map<UUID, Boolean>> areUsersSuperAdminByOrgId(List<UUID> userIdsList, UUID organizationId) {
        LOG.info("return a map of userId and SuperAdmin if they are superAdmin in the orgId {}", organizationId);

        return authzManagerRoleRepository.findByName("SuperAdmin")
                .flatMap(authzManagerRole -> Mono.just(authzManagerRole.getId()))
                .flatMap(uuid -> authzManagerRoleOrganizationRepository.
                        findByUserIdInAndAuthzManagerRoleIdAndOrganizationId(userIdsList, uuid, organizationId).collectList())
                .flatMap(list -> {
                    Map<UUID, Boolean> map = new HashMap<>();
                    userIdsList.forEach(uuid -> {
                        if (!list.contains(uuid)) {
                            map.put(uuid, false);
                        }
                    });
                    LOG.info("found list of UserIds in organization");
                    list.forEach(authzManagerRoleOrganization -> {
                        map.put(authzManagerRoleOrganization.getUserId(), true);
                    });
                    return Mono.just(map);
                });
    }

}
