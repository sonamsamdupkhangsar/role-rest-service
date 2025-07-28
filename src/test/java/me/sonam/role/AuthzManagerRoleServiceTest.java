package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.AuthzManagerRoleUserRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import me.sonam.role.repo.entity.AuthzManagerRoleUser;
import me.sonam.role.repo.entity.Role;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;


@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthzManagerRoleServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerRoleServiceTest.class);
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    ApplicationContext context;

    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;

    @Autowired
    private AuthzManagerRoleRepository authzManagerRoleRepository;

    @Autowired
    private AuthzManagerRoleUserRepository authzManagerRoleUserRepository;

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        this.webTestClient = WebTestClient
                .bindToApplicationContext(this.context)
                // add Spring Security test Support
                .apply(springSecurity())
                .configureClient()
                .filter(basicAuthentication("user", "password"))
                .build();
    }

    @Test
    public void createAuthzManagerRole() {
        LOG.info("create authzManagerRole");
        final String authenticationId = "sonam";


        authzManagerRoleRepository.deleteAll().subscribe();

        Jwt jwt = jwt(authenticationId);
       // when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));
        final String name = "SuperAdmin";

        var mapBody = Map.of("name", name);

        EntityExchangeResult<AuthzManagerRole> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .post().uri("/authzmanagerroles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(AuthzManagerRole.class)
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).isNotNull();
        assertThat(entityExchangeResult.getResponseBody().getName()).isEqualTo(name);
        assertThat(entityExchangeResult.getResponseBody().getId()).isNotNull();

        StepVerifier.create(authzManagerRoleRepository.existsById(entityExchangeResult.getResponseBody().getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleRepository.existsByName("SuperAdmin"))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleRepository.existsByName("SuperAdminS"))
                .assertNext(aBoolean -> assertThat(aBoolean).isFalse()).verifyComplete();

    }

    @Test
    public void assignUserToAuthzManagerRole() {
        LOG.info("assign user to authzManagerRole");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        final UUID userId = UUID.randomUUID();
        final UUID authzManagerRoleId = UUID.randomUUID();

        var mapBody = Map.of("userId", userId.toString(),
                "authzManagerRoleId", authzManagerRoleId.toString());

        EntityExchangeResult<AuthzManagerRoleUser> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .post().uri("/authzmanagerroles/users")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(AuthzManagerRoleUser.class)
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).isNotNull();
        assertThat(entityExchangeResult.getResponseBody().getAuthzManagerRoleId()).isEqualTo(authzManagerRoleId);
        assertThat(entityExchangeResult.getResponseBody().getUserId()).isEqualTo(userId);
        assertThat(entityExchangeResult.getResponseBody().getId()).isNotNull();

        StepVerifier.create(authzManagerRoleUserRepository.existsById(entityExchangeResult.getResponseBody().getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
    }

    @Test
    public void assignOrganizationToAuthzManagerRole() {
        LOG.info("assign user to authzManagerRole");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        final UUID userId = UUID.randomUUID();
        final UUID authzManagerRoleId = UUID.randomUUID();
        final UUID authzManagerRoleUserId = UUID.randomUUID();
        final UUID organizationId = UUID.randomUUID();

        var mapBody = Map.of("userId", userId.toString(),
                "authzManagerRoleId", authzManagerRoleId.toString(),
                "authzManagerRoleUserId", authzManagerRoleUserId,
                "organizationId", organizationId);

        EntityExchangeResult<AuthzManagerRoleOrganization> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .post().uri("/authzmanagerroles/users/organizations")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(AuthzManagerRoleOrganization.class)
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).isNotNull();
        assertThat(entityExchangeResult.getResponseBody().getAuthzManagerRoleId()).isEqualTo(authzManagerRoleId);
        assertThat(entityExchangeResult.getResponseBody().getUserId()).isEqualTo(userId);
        assertThat(entityExchangeResult.getResponseBody().getId()).isNotNull();
        assertThat(entityExchangeResult.getResponseBody().getOrganizationId()).isEqualTo(organizationId);
        assertThat(entityExchangeResult.getResponseBody().getOrganizationId()).isNotEqualTo(userId);

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(entityExchangeResult.getResponseBody().getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
    }

    @Test
    public void deleteUserFromAuthzManagerRoleOrganization() {
        LOG.info("delete user from authzManagerRoleOrganization");

        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        var authzManagerRoleUser = new AuthzManagerRoleUser(null, authzManagerRoleId, userId);

        var authzManagerRoleOrganization = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId, authzManagerRoleUser.getId());
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization).subscribe();

        EntityExchangeResult<String> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .delete().uri("/authzmanagerroles/users/organizations/"+authzManagerRoleOrganization.getId())
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(String.class)
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).isEqualTo("User removed from AuthzManagerRoleOrganization");

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isFalse()).verifyComplete();
    }

    @Test
    public void getUserIdsFromAuthzManagerRoleOrganizationByOrdId() {
        LOG.info("get authzManagerRoleOrganization by its id and orgId");

        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        UUID userId4 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        var authzManagerRole = new AuthzManagerRole(null, "SuperAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        UUID organizationId = UUID.randomUUID();
        var authzManagerRoleUser = new AuthzManagerRoleUser(null, authzManagerRoleId, userId1);

        var authzManagerRoleOrganization1 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId1, authzManagerRoleUser.getId());
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization1).subscribe();

        var authzManagerRoleOrganization2 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId2, authzManagerRoleUser.getId());
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization2).subscribe();

        var authzManagerRoleOrganization3 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId3, authzManagerRoleUser.getId());
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization3).subscribe();

        var authzManagerRoleOrganization4 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId4, authzManagerRoleUser.getId());
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization4).subscribe();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization1.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization2.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization3.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization4.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                authzManagerRoleId, organizationId, userId1, authzManagerRoleUser.getId())).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                authzManagerRoleId, organizationId, userId2, authzManagerRoleUser.getId())).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                authzManagerRoleId, organizationId, userId3, authzManagerRoleUser.getId())).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                authzManagerRoleId, organizationId, userId4, authzManagerRoleUser.getId())).expectNextCount(1).verifyComplete();
        //negative test with a random userId
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserIdAndAuthzManagerRoleUserId(
                authzManagerRoleId, organizationId, UUID.randomUUID(), authzManagerRoleUser.getId())).expectNextCount(0).verifyComplete();


        EntityExchangeResult<RestPage<UUID>> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId)
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().forEach(uuid -> LOG.info("uuid: {}", uuid));

        RestPage<UUID> restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.getNumberOfElements()).isEqualTo(4);
        assertThat(restPage.getTotalElements()).isEqualTo(4);
        assertThat(userId1).isIn(restPage.getContent());
        assertThat(userId2).isIn(restPage.getContent());
        assertThat(userId3).isIn(restPage.getContent());
        assertThat(userId4).isIn(restPage.getContent());
        assertThat(UUID.randomUUID()).isNotIn(restPage.getContent()); //negative test with random userId

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=0&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.getNumberOfElements()).isEqualTo(2);
        assertThat(restPage.getTotalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.getContent());

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=1&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.getNumberOfElements()).isEqualTo(2);
        assertThat(restPage.getTotalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.getContent());

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=2&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.getNumberOfElements()).isEqualTo(0);
        assertThat(restPage.getTotalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.getContent());
    }

    private Jwt jwt(String subjectName, UUID userId) {
        return new Jwt("token", null, null,
                Map.of("alg", "none"), Map.of("sub", subjectName, "userId", userId.toString()));
    }

    private Jwt jwt(String subjectName) {
        return new Jwt("token", null, null,
                Map.of("alg", "none"), Map.of("sub", subjectName));
    }

    private Consumer<HttpHeaders> addJwt(Jwt jwt) {
        return headers -> headers.setBearerAuth(jwt.getTokenValue());
    }

}
