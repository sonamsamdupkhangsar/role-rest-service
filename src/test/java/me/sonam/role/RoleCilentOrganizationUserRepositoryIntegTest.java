package me.sonam.role;

import me.sonam.role.handler.RoleException;
import me.sonam.role.repo.RoleClientOrganizationUserRepository;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataR2dbcTest
public class RoleCilentOrganizationUserRepositoryIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoleCilentOrganizationUserRepositoryIntegTest.class);

    @Autowired
    private RoleClientOrganizationUserRepository roleClientOrganizationUserRepository;

    @Test
    public void testMultipleUserIds() {
        LOG.info("testing multiple userIds");
        UUID roleId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        UUID userId4 = UUID.randomUUID();
        UUID userId5 = UUID.randomUUID();

        List<UUID> uuidList = List.of(userId1, userId2, userId3, userId4, userId5);


        ClientOrganizationUserRole rou = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId1);
        roleClientOrganizationUserRepository.save(rou).subscribe(roleClientOrganizationUser -> LOG.info("saved rou1: {}", roleClientOrganizationUser));

        rou = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId2);
        roleClientOrganizationUserRepository.save(rou).subscribe(roleClientOrganizationUser -> LOG.info("saved rou2: {}", roleClientOrganizationUser));

        rou = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId3);
        roleClientOrganizationUserRepository.save(rou).subscribe(roleClientOrganizationUser -> LOG.info("saved rou3: {}", roleClientOrganizationUser));

        rou = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId4);
        roleClientOrganizationUserRepository.save(rou).subscribe(roleClientOrganizationUser -> LOG.info("saved rou4: {}", roleClientOrganizationUser));

        rou = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId5);
        roleClientOrganizationUserRepository.save(rou).subscribe(roleClientOrganizationUser -> LOG.info("saved rou5: {}", roleClientOrganizationUser));



        roleClientOrganizationUserRepository.findByClientIdAndOrganizationIdAndUserIdIn(clientId, organizationId, uuidList)
                .subscribe(roleClientOrganizationUser -> LOG.info("found roleClientOrganizationUser: {}", roleClientOrganizationUser));

        UUID userId6 = UUID.randomUUID();
        LOG.info("userId6: {}", userId6);

        Flux<ClientOrganizationUserRole> roleClientOrganizationUserFlux =  roleClientOrganizationUserRepository.findByClientIdAndOrganizationIdAndUserIdIn(clientId, organizationId, List.of(userId1, userId2, userId3, userId4, userId5, userId6));

        StepVerifier.create(roleClientOrganizationUserFlux).assertNext(roleClientOrganizationUser -> {
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            assertEquals(roleClientOrganizationUser.getRoleId(), roleId);

            assertTrue(uuidList.contains(roleClientOrganizationUser.getUserId()));
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            LOG.info("verifying complete: {}", roleClientOrganizationUser);
        }).assertNext(roleClientOrganizationUser -> {
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            assertEquals(roleClientOrganizationUser.getRoleId(), roleId);

            assertTrue(uuidList.contains(roleClientOrganizationUser.getUserId()));
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            LOG.info("verifying complete: {}", roleClientOrganizationUser);
        }).assertNext(roleClientOrganizationUser -> {
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            assertEquals(roleClientOrganizationUser.getRoleId(), roleId);

            assertTrue(uuidList.contains(roleClientOrganizationUser.getUserId()));
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            LOG.info("verifying complete: {}", roleClientOrganizationUser);
        }).assertNext(roleClientOrganizationUser -> {
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            assertEquals(roleClientOrganizationUser.getRoleId(), roleId);

            assertTrue(uuidList.contains(roleClientOrganizationUser.getUserId()));
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            LOG.info("verifying complete: {}", roleClientOrganizationUser);
        }).assertNext(roleClientOrganizationUser -> {
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            assertEquals(roleClientOrganizationUser.getRoleId(), roleId);

            assertTrue(uuidList.contains(roleClientOrganizationUser.getUserId()));
            assertEquals(roleClientOrganizationUser.getOrganizationId(), organizationId);
            LOG.info("verifying complete: {}", roleClientOrganizationUser);
            assertFalse(uuidList.contains(userId6));
        }).verifyComplete();


    }

    @Test
    public void empty() {
        UUID roleId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        UUID userId4 = UUID.randomUUID();
        UUID userId5 = UUID.randomUUID();

        List<UUID> uuidList = List.of(userId1, userId2, userId3, userId4, userId5);


        Flux<ClientOrganizationUserRole> roleClientOrganizationUserFlux =  roleClientOrganizationUserRepository.findByClientIdAndOrganizationIdAndUserIdIn(clientId, organizationId, List.of(userId1, userId2, userId3, userId4, userId5));
        roleClientOrganizationUserFlux.switchIfEmpty(Mono.error(new RoleException("empty hellooo"))).flatMap(roleClientOrganizationUser -> {
            LOG.info("got roleClientOrganization with no match: {}", roleClientOrganizationUser);
            return Mono.just("hello");
        }).
                onErrorResume(throwable -> {
                    LOG.info("got empty rsulst: ", throwable);
                    return Mono.just("data not available");
                }).subscribe(s -> LOG.info("hello"));

        roleClientOrganizationUserFlux =  roleClientOrganizationUserRepository.findByClientIdAndOrganizationIdAndUserIdIn(clientId, organizationId, List.of(userId1, userId2, userId3, userId4, userId5));
        StepVerifier.create(roleClientOrganizationUserFlux).expectSubscription().verifyComplete();
    }
}
