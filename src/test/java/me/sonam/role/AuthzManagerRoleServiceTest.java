package me.sonam.role;


import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
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
    private AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository;

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
        final String name = "OrgAdmin";

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
        StepVerifier.create(authzManagerRoleRepository.existsByName("OrgAdmin"))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleRepository.existsByName("OrgAdminS"))
                .assertNext(aBoolean -> assertThat(aBoolean).isFalse()).verifyComplete();

        EntityExchangeResult<Map<String, UUID>> entityExchangeResult2 = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .put().uri("/roles/authzmanagerroles/name").bodyValue("OrgAdmin")
                .headers(addJwt(jwt)).exchange()
                .expectBody(new ParameterizedTypeReference<Map<String, UUID>>() {})
                .returnResult();

        assertThat(entityExchangeResult2.getResponseBody().get("message")).isNotNull();
        StepVerifier.create(authzManagerRoleRepository.findByName("OrgAdmin")).assertNext(authzManagerRole -> {
            assertThat(authzManagerRole.getId()).isEqualTo(entityExchangeResult2.getResponseBody().get("message"));
            LOG.info("verify that repo orgAdmin.id is equal to returned from rest call {}", authzManagerRole.getId(),
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

        EntityExchangeResult<AuthzManagerRoleAssignment> entityExchangeResult = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .post().uri("/roles/authzmanagerroles/users/organizations")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange()//.expectStatus().isEqualTo(201)
                .expectBody(AuthzManagerRoleAssignment.class)
                .returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody()).isNotNull();
        assertThat(entityExchangeResult.getResponseBody().getAuthzManagerRoleId()).isEqualTo(authzManagerRoleId);
        assertThat(entityExchangeResult.getResponseBody().getUserId()).isEqualTo(userId);
        assertThat(entityExchangeResult.getResponseBody().getId()).isNotNull();
        assertThat(entityExchangeResult.getResponseBody().getOrganizationId()).isEqualTo(organizationId);
        assertThat(entityExchangeResult.getResponseBody().getOrganizationId()).isNotEqualTo(userId);

        StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(entityExchangeResult.getResponseBody().getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
    }

    @Test
    public void setUserAsOrgAdminInOrganization() {
        LOG.info("set user as OrgAdmin role in Organization");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        final UUID userId = UUID.randomUUID();

        final UUID authzManagerRoleUserId = UUID.randomUUID();
        final UUID organizationId = UUID.randomUUID();

        authzManagerRoleRepository.existsByName("OrgAdmin").doOnNext(aBoolean -> {
            if (!aBoolean) {
                LOG.info("save OrgAdmin role if does not exist");
                authzManagerRoleRepository.save(new AuthzManagerRole(null, "OrgAdmin")).subscribe();
            }
        }).subscribe();

        var mapBody = Map.of("userId", userId.toString(),
                "authzManagerRoleName", "OrgAdmin",
                "authzManagerRoleUserId", authzManagerRoleUserId,
                "organizationId", organizationId);

        Mono<AuthzManagerRoleAssignment> authzManagerRoleAssignmentMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .post().uri("/roles/authzmanagerroles/names/users/organizations")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange()//.expectStatus().isEqualTo(201)
                        .returnResult(AuthzManagerRoleAssignment.class).getResponseBody().single();

        StepVerifier.create(authzManagerRoleAssignmentMono).assertNext(authzManagerRoleAssignment -> {

            LOG.info("authzManagerRoleAssignment: {}", authzManagerRoleAssignment);
            assertThat(authzManagerRoleAssignment).isNotNull();
            assertThat(authzManagerRoleAssignment.getAuthzManagerRoleId()).isNotNull();
            assertThat(authzManagerRoleAssignment.getUserId()).isEqualTo(userId);
            assertThat(authzManagerRoleAssignment.getId()).isNotNull();
            assertThat(authzManagerRoleAssignment.getOrganizationId()).isEqualTo(organizationId);
            assertThat(authzManagerRoleAssignment.getOrganizationId()).isNotEqualTo(userId);

            StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(authzManagerRoleAssignment.getId()))
                    .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();

            StepVerifier.create(authzManagerRoleAssignmentRepository.findById(authzManagerRoleAssignment.getId()))
                    .assertNext(authzManagerRoleAssignment1 -> {
                        assertThat(authzManagerRoleAssignment1.getAuthzManagerRoleId()).isNotNull();
                        assertThat(authzManagerRoleAssignment1.getUserId()).isEqualTo(userId);
                        assertThat(authzManagerRoleAssignment1.getId()).isNotNull();
                        assertThat(authzManagerRoleAssignment1.getOrganizationId()).isEqualTo(organizationId);
                        assertThat(authzManagerRoleAssignment1.getOrganizationId()).isNotEqualTo(userId);
                    }).verifyComplete();
        }).verifyComplete();
    }

    @Test
    public void deleteUserFromAuthzManagerRoleAssignment() {
        LOG.info("delete user from authzManagerRoleAssignment");

        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        var authzManagerRoleAssignment = new AuthzManagerRoleAssignment(null, authzManagerRoleId, userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment).subscribe();

        Mono<Map<String, String>> mapMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .delete().uri("/roles/authzmanagerroles/users/organizations/"+authzManagerRoleAssignment.getId())
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .returnResult(new ParameterizedTypeReference<Map<String, String>>() {}).getResponseBody().single();

        StepVerifier.create(mapMono).assertNext(map -> {
            LOG.info("assert the message is user removed from authzManagerRoleAssignment: {}", map);
            assertThat(map.get("message")).isEqualTo("User removed from AuthzManagerRoleAssignment");
        }).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(authzManagerRoleAssignment.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isFalse()).verifyComplete();
    }

    @Test
    public void getUserIdsFromAuthzManagerRoleAssignmentByOrdId() {
        LOG.info("get authzManagerRoleAssignment by its id and orgId");

        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        UUID authzManagerRoleId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        UUID userId4 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        var authzManagerRole = new AuthzManagerRole(null, "OrgAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        UUID organizationId = UUID.randomUUID();

        var authzManagerRoleAssignment1 = new AuthzManagerRoleAssignment(null, authzManagerRoleId, userId1, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment1).subscribe();

        var authzManagerRoleAssignment2 = new AuthzManagerRoleAssignment(null, authzManagerRoleId, userId2, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment2).subscribe();

        var authzManagerRoleAssignment3 = new AuthzManagerRoleAssignment(null, authzManagerRoleId, userId3, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment3).subscribe();

        var authzManagerRoleAssignment4 = new AuthzManagerRoleAssignment(null, authzManagerRoleId, userId4, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment4).subscribe();

        StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(authzManagerRoleAssignment1.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(authzManagerRoleAssignment2.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(authzManagerRoleAssignment3.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();
        StepVerifier.create(authzManagerRoleAssignmentRepository.existsById(authzManagerRoleAssignment4.getId()))
                .assertNext(aBoolean -> assertThat(aBoolean).isTrue()).verifyComplete();

        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId1, AuthzManagerRoleAssignment.ORGANIZATION, organizationId)).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId2, AuthzManagerRoleAssignment.ORGANIZATION, organizationId)).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId3, AuthzManagerRoleAssignment.ORGANIZATION, organizationId)).expectNextCount(1).verifyComplete();
        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(authzManagerRoleId, userId4, AuthzManagerRoleAssignment.ORGANIZATION, organizationId)).expectNextCount(1).verifyComplete();
        //negative test with a random userId
        StepVerifier.create(authzManagerRoleAssignmentRepository.findByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(
                authzManagerRoleId, UUID.randomUUID(), AuthzManagerRoleAssignment.ORGANIZATION, organizationId))
                .expectNextCount(0).verifyComplete();


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

        var authzManagerRole = new AuthzManagerRole(null, "OrgAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        UUID organizationId = UUID.randomUUID();

        var authzManagerRoleAssignment1 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId1, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment1).subscribe();

        var authzManagerRoleAssignment2 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId2, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment2).subscribe();

        var authzManagerRoleAssignment3 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId3, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment3).subscribe();

        var authzManagerRoleAssignment4 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId4, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment4).subscribe();

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
    public void isUserOrgAdmin() {
        final String authenticationId = "sonam";

        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(authenticationId, userId);
        UUID organizationId1 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        // this is to create orgAdmin role entity AuthzManagerRole
        var authzManagerRole = new AuthzManagerRole(null, "OrgAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        var authzManagerRoleAssignment1 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId1);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment1).subscribe();


        LOG.info("get list of organizations this user is orgAdmin for");

        Mono<Map<String, Boolean>> mapMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId1+"/org-admin")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                        .returnResult(new ParameterizedTypeReference<Map<String, Boolean>>() {}).getResponseBody().single();

        StepVerifier.create(mapMono).assertNext(map -> {
            LOG.info("assert we get a true for userId is a superadmin in  orgId: {}", map);
            assertThat(map.get("message")).isTrue();
        }).verifyComplete();

    }

    // this is a negative testing of isUserOrgAdmin call
    @Test
    public void isUserOrgAdminFalse() {
        final String authenticationId = "sonam";

        UUID userId = UUID.randomUUID();

        Jwt jwt = jwt(authenticationId, userId);
        UUID organizationId1 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        // this is to create orgAdmin role entity AuthzManagerRole
        var authzManagerRole = new AuthzManagerRole(null, "OrgAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        // we will skip saving of the following call to save AuthzManagerRoleAssignment relationship
        Mono<Map<String, Boolean>> monoMap = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/"+userId+"/organizations/"+organizationId1+"/org-admin")
                .headers(addJwt(jwt)).exchange()//.expectStatus().isEqualTo(201)
                .returnResult(new ParameterizedTypeReference<Map<String, Boolean>>() { }).getResponseBody().single();

        StepVerifier.create(monoMap).assertNext(map -> {
            LOG.info("assert we get a false for userId not a superadmin in orgId: {}", map);
            assertThat(map.get("message")).isFalse();
        }).verifyComplete();

    }

    @Test
    public void getOrgAdminOrganizationForLoggedInUser() {
        final String authenticationId = "sonam";

        UUID userId1 = UUID.randomUUID();

        Jwt jwt = jwt(authenticationId, userId1);
        UUID organizationId1 = UUID.randomUUID();
        UUID organizationId2 = UUID.randomUUID();
        UUID organizationId3 = UUID.randomUUID();

        authzManagerRoleRepository.deleteAll().subscribe();

        var authzManagerRole = new AuthzManagerRole(null, "OrgAdmin");
        authzManagerRoleRepository.save(authzManagerRole).subscribe();
        StepVerifier.create(authzManagerRoleRepository.existsById(authzManagerRole.getId())).assertNext(
                aBoolean -> {
                    assertThat(aBoolean).isTrue();
                    LOG.info("authzManagerRole.id: {}", authzManagerRole.getId());
                }
        ).verifyComplete();

        var authzManagerRoleAssignment1 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId1, AuthzManagerRoleAssignment.ORGANIZATION, organizationId1);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment1).subscribe();

        var authzManagerRoleAssignment2 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId1, AuthzManagerRoleAssignment.ORGANIZATION, organizationId2);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment2).subscribe();

        var authzManagerRoleAssignment3 = new AuthzManagerRoleAssignment(null, authzManagerRole.getId(), userId1, AuthzManagerRoleAssignment.ORGANIZATION, organizationId3);
        authzManagerRoleAssignmentRepository.save(authzManagerRoleAssignment3).subscribe();

        LOG.info("get list of organizations this user is orgAdmin for");

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
        final UUID[] firstPagedOrgId = new UUID[1];
        final UUID[] secondPagedOrgId = new UUID[1];
        final UUID[] thirdPagedOrgId = new UUID[1];

        pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=0&size=1")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(1);
            assertThat(List.of(organizationId1, organizationId2, organizationId3).contains(list.getFirst())).isTrue();
            firstPagedOrgId[0] = list.getFirst();
        }).verifyComplete();

        pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=1&size=1")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(1);
            assertThat(List.of(organizationId1, organizationId2, organizationId3).contains(list.getFirst())).isTrue();
            secondPagedOrgId[0] = list.getFirst();

        }).verifyComplete();

        pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt))
                .get().uri("/roles/authzmanagerroles/users/organizations?page=2&size=1")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<UUID>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            List<UUID> list = page.content();

            assertThat(list.size()).isEqualTo(1);
            assertThat(List.of(organizationId1, organizationId2, organizationId3).contains(list.getFirst())).isTrue();
            thirdPagedOrgId[0] = list.getFirst();

        }).verifyComplete();

        assertThat(List.of(firstPagedOrgId[0], secondPagedOrgId[0], thirdPagedOrgId[0])
                .containsAll(List.of(organizationId1, organizationId2, organizationId3))).isTrue();

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
