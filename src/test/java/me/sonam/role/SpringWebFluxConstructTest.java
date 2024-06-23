package me.sonam.role;


import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleOrganization;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class SpringWebFluxConstructTest {
    private static final Logger LOG = LoggerFactory.getLogger(SpringWebFluxConstructTest.class);

    @Test
    public void testConstruct() {
        Mono<Role> roleMono = Mono.just(new Role(UUID.randomUUID(), "admin", UUID.randomUUID()));

        roleMono.flatMap(role -> save(role).doOnNext(role1 -> {
            LOG.info("role1: {}", role1);
                        role1.setRoleOrganization(role.getRoleOrganization());}))
                .flatMap(role -> {
                    if (role.getRoleOrganization() != null) {
                       return saveR(role.getRoleOrganization()).thenReturn(role);
                    }
                    else {
                        return Mono.just(role);
                    }
                })
                .subscribe(role -> LOG.info("saved role: {}", role));

    }

    private Mono<Role> save(Role role) {
        role.setName("Saved");
        return Mono.just(role);
    }

    private Mono<RoleOrganization> saveR(RoleOrganization roleOrganization) {
        return Mono.just(roleOrganization);
    }
}
