package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.AuthzManagerRoleUserRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleUser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthzManagerRoleUserRepoTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerRoleUserRepoTest.class);

    @Autowired
    private AuthzManagerRoleUserRepository authzManagerRoleUserRepository;

    @Test
    public void create() {
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var authzManagerRoleUser = new AuthzManagerRoleUser(null, authzManagerRoleId, userId);
        authzManagerRoleUserRepository.save(authzManagerRoleUser).subscribe();

        StepVerifier.create(authzManagerRoleUserRepository.existsByUserId(userId)).assertNext(val -> {
            assertThat(val).isTrue();
        }).verifyComplete();

        authzManagerRoleUserRepository.deleteByAuthzManagerRoleIdAndUserId(authzManagerRoleId, userId).subscribe();


        StepVerifier.create(authzManagerRoleUserRepository.existsByUserId(userId)).assertNext(val -> {
            assertThat(val).isFalse();
                }).verifyComplete();

    }


}
