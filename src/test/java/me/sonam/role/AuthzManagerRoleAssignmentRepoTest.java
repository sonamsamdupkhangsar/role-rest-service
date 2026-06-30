package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthzManagerRoleAssignmentRepoTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerRoleAssignmentRepoTest.class);

    @Autowired
    private AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository;
    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    public void create() {
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        //AuthzManagerRoleAssignment(UUID id, UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId) {
        var authzManagerRoleAssignment = new AuthzManagerRoleAssignment(null, authzManagerRoleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment).subscribe();

        StepVerifier.create(authzManagerRoleAssignmentRepository.existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId)).assertNext(val -> {
            assertThat(val).isTrue();
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, null, AuthzManagerRoleAssignment.ORGANIZATION, organizationId)).assertNext(val -> {
            assertThat(val).isFalse();
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, UUID.randomUUID())).assertNext(val -> {
            assertThat(val).isFalse();
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.countByAuthzManagerRoleIdAndScopeTypeAndScopeId(authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId))
                        .assertNext(count -> {
                            assertThat(count).isEqualTo(1);
                        }).verifyComplete();


        Pageable pageable = Pageable.ofSize(2);

        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndScopeTypeAndScopeId(authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId, pageable).collectList())
                        .assertNext(list -> assertThat(list.size()).isEqualTo(1)).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId).collectList()).assertNext(list -> assertThat(list.size()).isEqualTo(1)).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndScopeTypeAndScopeId(authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId, Pageable.ofSize(10))
                .collectList()).assertNext(list -> assertThat(list.size()).isEqualTo(1)).verifyComplete();

        var authzManagerRoleAssignment2 = new AuthzManagerRoleAssignment(null, authzManagerRoleId,
                UUID.randomUUID(), AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment2).subscribe();

        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndScopeTypeAndScopeId(authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId, Pageable.ofSize(10))
                .collectList()).assertNext(list -> assertThat(list.size()).isEqualTo(2)).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndScopeTypeAndScopeId(authzManagerRoleId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId, Pageable.ofSize(1))
                .collectList()).assertNext(list -> {
                    list.forEach(authzManagerRoleAssignment1 -> LOG.info("authzManagerRoleOrg: {}", authzManagerRoleAssignment1));
            assertThat(list.size()).isEqualTo(1);

        }).verifyComplete();

    }


}
