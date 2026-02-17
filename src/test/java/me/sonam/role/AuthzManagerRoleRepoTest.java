package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;
import org.slf4j.Logger;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthzManagerRoleRepoTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerRoleRepoTest.class);

    @Autowired
    private AuthzManagerRoleRepository authzManagerRoleRepository;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    public void create() {
        var authzManagerRole1 = new AuthzManagerRole(null, "SuperAdmin");

        authzManagerRoleRepository.save(authzManagerRole1).subscribe();
        var authzManagerRole2 = new AuthzManagerRole(null, "SuperAdmin2");
        authzManagerRoleRepository.save(authzManagerRole2).subscribe();

        StepVerifier.create(authzManagerRoleRepository.findByName("SuperAdmin")).assertNext(authzManagerRole -> {
            assertThat(authzManagerRole1.getId()).isNotNull();
            assertThat(authzManagerRole1.getName()).isEqualTo("SuperAdmin");
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleRepository.findByName("SuperAdmin")).expectNextCount(1).verifyComplete();

        StepVerifier.create(authzManagerRoleRepository.existsByName("SuperAdmin")).assertNext(aBoolean ->
                assertThat(aBoolean).isTrue()).verifyComplete();
    }



}
