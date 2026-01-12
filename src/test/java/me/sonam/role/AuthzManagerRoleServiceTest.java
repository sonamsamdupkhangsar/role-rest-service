package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
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

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    ApplicationContext context;

    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;

    @Autowired
    private AuthzManagerRoleRepository authzManagerRoleRepository;


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
                .post().uri("/roles/authzmanagerroles")
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

        EntityExchangeResult<Map<String, UUID>> entityExchangeResult2 = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .put().uri("/roles/authzmanagerroles/name").bodyValue("SuperAdmin")
                .headers(addJwt(jwt)).exchange()
                .expectBody(new ParameterizedTypeReference<Map<String, UUID>>() {})
                .returnResult();

        assertThat(entityExchangeResult2.getResponseBody().get("message")).isNotNull();
        StepVerifier.create(authzManagerRoleRepository.findByName("SuperAdmin")).assertNext(authzManagerRole -> {
            assertThat(authzManagerRole.getId()).isEqualTo(entityExchangeResult2.getResponseBody().get("message"));
            LOG.info("verify that repo superAdmin.id is equal to returned from rest call {}", authzManagerRole.getId(),
                    entityExchangeResult2.getResponseBody().get("message"));
        }).verifyComplete();

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
                .post().uri("/roles/authzmanagerroles/users/organizations")
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
    public void setUserAsSuperAdminInOrganization() {
        LOG.info("set user as SuperAdmin role in Organization");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        final UUID userId = UUID.randomUUID();

        final UUID authzManagerRoleUserId = UUID.randomUUID();
        final UUID organizationId = UUID.randomUUID();

        authzManagerRoleRepository.existsByName("SuperAdmin").doOnNext(aBoolean -> {
            if (!aBoolean) {
                LOG.info("save SuperAdmin role if does not exist");
                authzManagerRoleRepository.save(new AuthzManagerRole(null, "SuperAdmin")).subscribe();
            }
        }).subscribe();

        var mapBody = Map.of("userId", userId.toString(),
                "authzManagerRoleName", "SuperAdmin",
                "authzManagerRoleUserId", authzManagerRoleUserId,
                "organizationId", organizationId);

        Mono<AuthzManagerRoleOrganization> authzManagerRoleOrganizationMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .post().uri("/roles/authzmanagerroles/names/users/organizations")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange()//.expectStatus().isEqualTo(201)
                        .returnResult(AuthzManagerRoleOrganization.class).getResponseBody().single();

        StepVerifier.create(authzManagerRoleOrganizationMono).assertNext(authzManagerRoleOrganization -> {

            LOG.info("authzManagerRoleOrganization: {}", authzManagerRoleOrganization);
            assertThat(authzManagerRoleOrganization).isNotNull();
            assertThat(authzManagerRoleOrganization.getAuthzManagerRoleId()).isNotNull();
            assertThat(authzManagerRoleOrganization.getUserId()).isEqualTo(userId);
            assertThat(authzManagerRoleOrganization.getId()).isNotNull();
            assertThat(authzManagerRoleOrganization.getOrganizationId()).isEqualTo(organizationId);
            assertThat(authzManagerRoleOrganization.getOrganizationId()).isNotEqualTo(userId);

            StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization.getId()))
                    .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();

            StepVerifier.create(authzManagerRoleOrganizationRepository.findById(authzManagerRoleOrganization.getId()))
                    .assertNext(authzManagerRoleOrganization1 -> {
                        assertThat(authzManagerRoleOrganization1.getAuthzManagerRoleId()).isNotNull();
                        assertThat(authzManagerRoleOrganization1.getUserId()).isEqualTo(userId);
                        assertThat(authzManagerRoleOrganization1.getId()).isNotNull();
                        assertThat(authzManagerRoleOrganization1.getOrganizationId()).isEqualTo(organizationId);
                        assertThat(authzManagerRoleOrganization1.getOrganizationId()).isNotEqualTo(userId);
                    }).verifyComplete();
        }).verifyComplete();
    }

    @Test
    public void deleteUserFromAuthzManagerRoleOrganization() {
        LOG.info("delete user from authzManagerRoleOrganization");

        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        var authzManagerRoleOrganization = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization).subscribe();

        Mono<Map<String, String>> mapMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .delete().uri("/roles/authzmanagerroles/users/organizations/"+authzManagerRoleOrganization.getId())
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .returnResult(new ParameterizedTypeReference<Map<String, String>>() {}).getResponseBody().single();

        StepVerifier.create(mapMono).assertNext(map -> {
            LOG.info("assert the message is user removed from authzManagerRoleOrganization: {}", map);
            assertThat(map.get("message")).isEqualTo("User removed from AuthzManagerRoleOrganization");
        }).verifyComplete();

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

        var authzManagerRoleOrganization1 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId1);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization1).subscribe();

        var authzManagerRoleOrganization2 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId2);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization2).subscribe();

        var authzManagerRoleOrganization3 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId3);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization3).subscribe();

        var authzManagerRoleOrganization4 = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId4);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization4).subscribe();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization1.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization2.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization3.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization4.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId1)).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId2)).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId3)).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, userId4)).expectNextCount(1).verifyComplete();
        //negative test with a random userId
        StepVerifier.create(authzManagerRoleOrganizationRepository.findByAuthzManagerRoleIdAndOrganizationIdAndUserId(
                authzManagerRoleId, organizationId, UUID.randomUUID())).expectNextCount(0).verifyComplete();


        EntityExchangeResult<RestPage<UUID>> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId)
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().content().forEach(uuid -> LOG.info("uuid: {}", uuid));

        RestPage<UUID> restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.numberOfElements()).isEqualTo(4);
        assertThat(restPage.totalElements()).isEqualTo(4);
        assertThat(userId1).isIn(restPage.content());
        assertThat(userId2).isIn(restPage.content());
        assertThat(userId3).isIn(restPage.content());
        assertThat(userId4).isIn(restPage.content());
        assertThat(UUID.randomUUID()).isNotIn(restPage.content()); //negative test with random userId

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=0&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().content().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.numberOfElements()).isEqualTo(2);
        assertThat(restPage.totalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.content());

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=1&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().content().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.numberOfElements()).isEqualTo(2);
        assertThat(restPage.totalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.content());

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=2&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().content().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.numberOfElements()).isEqualTo(0);
        assertThat(restPage.totalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.content());

        entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/"+authzManagerRoleId+"/users/organizations/"+organizationId +
                        "?page=1&size=2")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<RestPage<UUID>>() {
                })
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        entityExchangeResult.getResponseBody().content().forEach(uuid -> LOG.info("uuid: {}", uuid));

        restPage = entityExchangeResult.getResponseBody();
        assertThat(restPage.numberOfElements()).isEqualTo(2);
        assertThat(restPage.totalElements()).isEqualTo(4);
        assertThat(UUID.randomUUID()).isNotIn(restPage.content());
    }

    @Test
    public void checkUserAdmin() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        UUID userId4 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        var authzManagerRole = new AuthzManagerRole(null, "SuperAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        UUID organizationId = UUID.randomUUID();

        var authzManagerRoleOrganization1 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId, userId1);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization1).subscribe();

        var authzManagerRoleOrganization2 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId, userId2);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization2).subscribe();

        var authzManagerRoleOrganization3 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId, userId3);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization3).subscribe();

        var authzManagerRoleOrganization4 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId, userId4);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization4).subscribe();

        LOG.info("get map of UUID with boolean value if they are superadmins");
        UUID userId5Invalid = UUID.randomUUID();
        EntityExchangeResult<Map<UUID, UUID>> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .put().uri("/roles/authzmanagerroles/users/organizations/"+organizationId)
                .bodyValue(List.of(userId1, userId2, userId3, userId4, userId5Invalid)).headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<Map<UUID, UUID>>() {
                })
                .returnResult();

        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        Map<UUID, UUID> map = entityExchangeResult.getResponseBody();
        assertThat(map.size()).isEqualTo(5);
        assertThat(map.get(userId1)).isNotNull();
        assertThat(map.get(userId2)).isNotNull();
        assertThat(map.get(userId3)).isNotNull();
        assertThat(map.get(userId4)).isNotNull();
        assertThat(map.get(userId5Invalid)).isNull();
    }

    //test to see if the user is a superadmin based on orgId
    @Test
    public void isUserSuperAdmin() {
        final String authenticationId = "sonam";

        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(authenticationId, userId);
        UUID organizationId1 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        // this is to create superAdmin role entity AuthzManagerRole
        var authzManagerRole = new AuthzManagerRole(null, "SuperAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        var authzManagerRoleOrganization1 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId1, userId);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization1).subscribe();


        LOG.info("get list of organizations this user is superAdmin for");

        Mono<Map<String, Boolean>> mapMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId1)
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                        .returnResult(new ParameterizedTypeReference<Map<String, Boolean>>() {}).getResponseBody().single();

        StepVerifier.create(mapMono).assertNext(map -> {
            LOG.info("assert we get a true for userId is a superadmin in  orgId: {}", map);
            assertThat(map.get("message")).isTrue();
        }).verifyComplete();

    }

    // this is a negative testing of isUserSuperAdmin call
    @Test
    public void isUserSuperAdminFalse() {
        final String authenticationId = "sonam";

        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(authenticationId, userId);
        UUID organizationId1 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        // this is to create superAdmin role entity AuthzManagerRole
        var authzManagerRole = new AuthzManagerRole(null, "SuperAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        // we will skip saving of the following call to save AuthzManagerRoleOrganization relationship
        Mono<Map<String, Boolean>> monoMap = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId1)
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .returnResult(new ParameterizedTypeReference<Map<String, Boolean>>() { }).getResponseBody().single();

        StepVerifier.create(monoMap).assertNext(map -> {
            LOG.info("assert we get a false for userId not a superadmin in orgId: {}", map);
            assertThat(map.get("message")).isFalse();
        }).verifyComplete();

    }

    @Test
    public void getSuperAdminOrganizationForLoggedInUser() {
        final String authenticationId = "sonam";

        UUID userId1 = UUID.randomUUID();

        Jwt jwt = jwt(authenticationId, userId1);
        UUID organizationId1 = UUID.randomUUID();
        UUID organizationId2 = UUID.randomUUID();
        UUID organizationId3 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        var authzManagerRole = new AuthzManagerRole(null, "SuperAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        var authzManagerRoleOrganization1 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId1, userId1);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization1).subscribe();

        var authzManagerRoleOrganization2 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId2, userId1);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization2).subscribe();

        var authzManagerRoleOrganization3 = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), organizationId3, userId1);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization3).subscribe();

        LOG.info("get list of organizations this user is superAdmin for");

        Mono<RestPage<UUID>> pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=0&size=10")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            LOG.info("got page of uuids {}", page.content());
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(3);
            assertThat(list.contains(organizationId1)).isTrue();
            assertThat(list.contains(organizationId2)).isTrue();
            assertThat(list.contains(organizationId3)).isTrue();

            UUID invalidOrgId = UUID.randomUUID();
            assertThat(list.contains(invalidOrgId)).isFalse();

        }).verifyComplete();

        LOG.info("get only 1 item in the page");
        pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=0&size=1")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(1);
            assertThat(list.contains(organizationId1)).isTrue();
        }).verifyComplete();

        pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=1&size=1")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(1);
            assertThat(list.contains(organizationId2)).isTrue();

        }).verifyComplete();

        pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=2&size=1")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(1);
            assertThat(list.contains(organizationId3)).isTrue();

        }).verifyComplete();

        LOG.info("get the total size of organizations assoicated to logged-in user");

        EntityExchangeResult<Map<String, Integer>> entityExchangeResult3 = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations/count")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(new ParameterizedTypeReference<Map<String, Integer>>() {
                })
                .returnResult();

        LOG.info("map contains: {}", entityExchangeResult3.getResponseBody());
        Map<String, Integer> map = entityExchangeResult3.getResponseBody();
        assertThat(map.get("message")).isEqualTo(3);
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
