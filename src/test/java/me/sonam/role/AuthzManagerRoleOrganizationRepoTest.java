package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthzManagerRoleOrganizationRepoTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerRoleOrganizationRepoTest.class);

    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;

    @Test
    public void create() {
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        //AuthzManagerRoleOrganization(UUID id, UUID authzManagerRoleId, UUID organizationId, UUID userId, UUID authzManagerRoleUserId) {
        var authzManagerRoleOrganization = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization).subscribe();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId)).assertNext(val -> {
            assertThat(val).isTrue();
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, null)).assertNext(val -> {
            assertThat(val).isFalse();
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, UUID.randomUUID(), userId)).assertNext(val -> {
            assertThat(val).isFalse();
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.countByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId, organizationId))
                        .assertNext(count -> {
                            assertThat(count).isEqualTo(1);
                        }).verifyComplete();


        Pageable pageable = Pageable.ofSize(2);

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId, organizationId, pageable).collectList())
                        .assertNext(list -> assertThat(list.size()).isEqualTo(1)).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId).collectList()).assertNext(list -> assertThat(list.size()).isEqualTo(1)).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId,
                organizationId, Pageable.ofSize(10))
                .collectList()).assertNext(list -> assertThat(list.size()).isEqualTo(1)).verifyComplete();

        var authzManagerRoleOrganization2 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, UUID.randomUUID());
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization2).subscribe();

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId,
                        organizationId, Pageable.ofSize(10))
                .collectList()).assertNext(list -> assertThat(list.size()).isEqualTo(2)).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationId(authzManagerRoleId,
                        organizationId, Pageable.ofSize(1))
                .collectList()).assertNext(list -> {
                    list.forEach(authzManagerRoleOrganization1 -> LOG.info("authzManagerRoleOrg: {}", authzManagerRoleOrganization1));
            assertThat(list.size()).isEqualTo(1);

        }).verifyComplete();

    }


}
