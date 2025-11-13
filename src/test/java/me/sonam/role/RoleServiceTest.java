package me.sonam.role;

import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.handler.service.carrier.User;
import me.sonam.role.repo.AuthzManagerRoleOrganizationRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import me.sonam.role.repo.entity.Role;
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
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RoleServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoleServiceTest.class);
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository;

    @Autowired
    private AuthzManagerRoleOrganizationRepository authzManagerRoleOrganizationRepository;

    @Autowired
    ApplicationContext context;

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
    public void createRole() {
        LOG.info("create role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);

        UUID orgId = UUID.randomUUID();

        Role role = new Role(null, "admin role", orgId);
        assertThat(role.getId()).isNotNull();
        assertThat(role.isNew()).isTrue();
        Mono<Role> roleMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).post().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(role).exchange().returnResult(Role.class)
                .getResponseBody().single();

        StepVerifier.create(roleMono).assertNext(role1 -> {
            LOG.info("role1: {}", role1);
            assertThat(role1.getId()).isNotNull();
            assertThat(role1.getName()).isEqualTo("admin role");
            assertThat(role1.getOrganizationId()).isEqualTo(orgId);
        }).verifyComplete();
    }

    @Test
    public void getRole() {
        LOG.info("create role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);

        UUID orgId = UUID.randomUUID();

        Role role = new Role(null, "admin role", orgId);
        roleRepository.save(role).subscribe();

        assertThat(role.getId()).isNotNull();
        assertThat(role.isNew()).isTrue();

        Mono<Role> roleMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).get().uri("/roles/"+role.getId())
                .headers(addJwt(jwt)).exchange().returnResult(Role.class)
                .getResponseBody().single();

        StepVerifier.create(roleMono).assertNext(role1 -> {
            LOG.info("assert role1 exists: {}", role1);
            assertThat(role1.getId()).isNotNull();
            assertThat(role1.getId()).isEqualTo(role.getId());
            assertThat(role1.getName()).isEqualTo("admin role");
            assertThat(role1.getOrganizationId()).isEqualTo(orgId);

            assertThat(role1).isEqualTo(role);
        }).verifyComplete();
    }

    @Test
    public void updateRole() {
        LOG.info("update role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);

        UUID orgId = UUID.randomUUID();

        Role role = new Role(null, "admin role", orgId);
        roleRepository.save(role).subscribe();

        assertThat(role.getId()).isNotNull();
        assertThat(role.isNew()).isTrue();

        Mono<Role> roleMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).put().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(role).exchange().returnResult(Role.class)
                .getResponseBody().single();

        StepVerifier.create(roleMono).assertNext(role1 -> {
            LOG.info("role1: {}", role1);
            assertThat(role1.getId()).isNotNull();
            assertThat(role1.getName()).isEqualTo("admin role");
            assertThat(role1.getOrganizationId()).isEqualTo(orgId);
        }).verifyComplete();
    }

    @Test
    public void deleteRole() {
        LOG.info("update role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);

        UUID orgId = UUID.randomUUID();

        Role role = new Role(null, "admin role", orgId);
        roleRepository.save(role).subscribe();

        assertThat(role.getId()).isNotNull();
        assertThat(role.isNew()).isTrue();

        Mono<Map<String, String>> monoMap = webTestClient.mutateWith(mockJwt().jwt(jwt)).delete().uri("/roles/"+role.getId())
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<Map<String, String>>() {})
                .getResponseBody().single();

        StepVerifier.create(monoMap).assertNext(map -> {
            LOG.info("response body map contains: {}", map);
            assertThat(map.get("message")).isEqualTo("role deleted");
        }).verifyComplete();

        StepVerifier.create(roleRepository.existsById(role.getId())).assertNext(aBoolean ->
                {
                    LOG.info("role does not exists?: {}", aBoolean);
                    assertThat(aBoolean).isFalse();
                }
        ).verifyComplete();
    }

    @Test
    public void getOrganizationRoles() {
        LOG.info("get organization roles");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);

        UUID orgId = UUID.randomUUID();

        Role role1 = new Role(null, "admin role", orgId);
        roleRepository.save(role1).subscribe();

        Role role2 = new Role(null, "user role", orgId);
        roleRepository.save(role2).subscribe();

        Role role3 = new Role(null, "super admin role", orgId);
        roleRepository.save(role3).subscribe();

        Role role4 = new Role(null, "super user role", orgId);
        roleRepository.save(role4).subscribe();

        Role role5 = new Role(null, "dummy role", orgId);
        roleRepository.save(role5).subscribe();

        Mono<RestPage<Role>> pageMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).get().uri("/roles/organizations/"
                        +orgId+"?page=0&size=10")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<RestPage<Role>>() {})
                .getResponseBody().single();

        StepVerifier.create(pageMono).assertNext(page -> {
            LOG.info("assert roles with orgId: {}", page);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(1);
            assertThat(page.getNumberOfElements()).isEqualTo(5);

            List<Role> list = page.getContent();
            LOG.info("list contains {}", list);
            assertThat(list.size()).isEqualTo(5);
            assertThat(list.contains(role1)).isTrue();
            assertThat(list.contains(role2)).isTrue();
            assertThat(list.contains(role3)).isTrue();
            assertThat(list.contains(role4)).isTrue();
            assertThat(list.contains(role5)).isTrue();
        }).verifyComplete();
    }


    @Test
    public void createClientOrganizationUsers() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("organizationId", organizationId);
        map.put("userId", userId);
        map.put("roleId", roleId);

        ClientOrganizationUserWithRole clientOrganizationUserWithRole = new ClientOrganizationUserWithRole
                (null, clientId, organizationId, new User(userId), new Role(roleId, null, null));


        Mono<ClientOrganizationUserRole> mono = webTestClient.mutateWith(mockJwt().jwt(jwt)).post()
                .uri("/roles/clients/organizations/users/roles")
                .headers(addJwt(jwt)).bodyValue(clientOrganizationUserWithRole).exchange().returnResult(ClientOrganizationUserRole.class)
                .getResponseBody().single();

        StepVerifier.create(mono).assertNext(clientOrganizationUserRole -> {
            LOG.info("saved client organization user role: {}", clientOrganizationUserRole);
            assertThat(clientOrganizationUserRole.getId()).isNotNull();
            assertThat(clientOrganizationUserRole.getClientId()).isEqualTo(clientId);
            assertThat(clientOrganizationUserRole.getRoleId()).isEqualTo(roleId);
            assertThat(clientOrganizationUserRole.getOrganizationId()).isEqualTo(organizationId);
            assertThat(clientOrganizationUserRole.getUserId()).isEqualTo(userId);
        }).verifyComplete();
    }

    @Test
    public void getClientOrganizationUsers() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("organizationId", organizationId);
        map.put("userId", userId1);
        map.put("roleId", roleId);

        var role = new Role(roleId, "admin role", organizationId);
        role.setNew(true);

        roleRepository.save(role).subscribe();

        //e(UUID id, UUID roleId, UUID clientId, UUID organizationId, UUID userId) {
        var clientOrganizationUserRole1 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId1);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole1).subscribe();

        UUID userId2 = UUID.randomUUID();
        var clientOrganizationUserRole2 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId2);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole2).subscribe();

        UUID userId3 = UUID.randomUUID();
        var clientOrganizationUserRole3 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId3);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole3).subscribe();

        var userIdsCsv = userId1 + "," + userId2 +","+ userId3;

         List<ClientOrganizationUserWithRole> clientOrganizationUserWithRoles = webTestClient.mutateWith(mockJwt().jwt(jwt)).put()
                 .uri("/roles/clients/"+clientId+"/organizations/"+organizationId+"/users/roles")
                .headers(addJwt(jwt)).bodyValue(userIdsCsv).exchange().expectStatus().isOk()
                 .expectBody(new ParameterizedTypeReference<List<ClientOrganizationUserWithRole>>() {})
                .returnResult().getResponseBody();

         clientOrganizationUserWithRoles.forEach(clientOrganizationUserWithRole -> {
             LOG.info("check organization user role: {}", clientOrganizationUserWithRole);
             assertThat(clientOrganizationUserWithRole.getId()).isNotNull();
             assertThat(clientOrganizationUserWithRole.getClientId()).isEqualTo(clientId);
             assertThat(clientOrganizationUserWithRole.getRole().getId()).isEqualTo(roleId);
             assertThat(clientOrganizationUserWithRole.getOrganizationId()).isEqualTo(organizationId);

             assertThat(clientOrganizationUserWithRole.getUser().getId()).isIn(userId1, userId2, userId3);
         });

/* this does not work here. don't know why
        Mono<List<ClientOrganizationUserWithRole>> mono = webTestClient.mutateWith(mockJwt().jwt(jwt)).put()
                .uri("/roles/clients/"+clientId+"/organizations/"+organizationId+"/users/roles")
                .headers(addJwt(jwt)).bodyValue(userIdsCsv).exchange().
                returnResult(new ParameterizedTypeReference<List<ClientOrganizationUserWithRole>>() {})
                .getResponseBody().single();

        StepVerifier.create(mono).assertNext(clientOrganizationUserWithRoleList -> {
             clientOrganizationUserWithRoleList.forEach(clientOrganizationUserWithRole -> {
                        LOG.info("saved client organization user role: {}", clientOrganizationUserWithRole);
                        assertThat(clientOrganizationUserWithRole.getId()).isNotNull();
                        assertThat(clientOrganizationUserWithRole.getClientId()).isEqualTo(clientId);
                        assertThat(clientOrganizationUserWithRole.getRole().getId()).isEqualTo(roleId);
                        assertThat(clientOrganizationUserWithRole.getOrganizationId()).isEqualTo(organizationId);

                        assertThat(clientOrganizationUserWithRole.getUser().getId()).isIn(userId1, userId2, userId3);
                    });
        }).verifyComplete();
*/

    }

    @Test
    public void deleteClientOrganizationUser() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("organizationId", organizationId);
        map.put("userId", userId1);
        map.put("roleId", roleId);

        var role = new Role(roleId, "admin role", organizationId);
        role.setNew(true);

        roleRepository.save(role).subscribe();

        var clientOrganizationUserRole1 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId1);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole1).subscribe();

        StepVerifier.create(clientOrganizationUserRoleRepository.existsById(clientOrganizationUserRole1.getId())).assertNext(exists ->
        {
            LOG.info("should exists prior to deleting: {}", exists);
            assertThat(exists).isTrue();
        }).verifyComplete();


        Mono<String> stringMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).delete()
                .uri("/roles/clients/organizations/users/roles/"+clientOrganizationUserRole1.getId())
                .headers(addJwt(jwt)).exchange().returnResult(String.class).getResponseBody().single();

        StepVerifier.create(stringMono).assertNext(string -> {
            LOG.info("assert deleted string response: {}", string);
            assertThat(string).contains("roleClientOrganizationUser deleted");
        }).verifyComplete();

        StepVerifier.create(clientOrganizationUserRoleRepository.existsById(clientOrganizationUserRole1.getId())).assertNext(exists ->
        {
            LOG.info("should not exists now after deletion: {}", exists);
            assertThat(exists).isFalse();
        }).verifyComplete();
    }

    //delete everything related to this org (Role, ClientOrganizationUserRole, AuthzManagerRoleOrganization)
    @Test
    public void deleteMyRole() {
        final String authenticationId = "sonam";
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(authenticationId, userId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        var role = new Role(roleId, "admin role", organizationId);
        role.setNew(true);

        roleRepository.save(role).subscribe();

        var clientOrganizationUserRole1 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole1).subscribe();

        //(UUID id, UUID authzManagerRoleId, UUID organizationId, UUID userId) {
        UUID authzManagerRoleId = UUID.randomUUID();

        var authzManagerRoleOrganization = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization).subscribe();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization.getId())).assertNext(boolValue -> {
            assertThat(boolValue).isTrue();
        }).verifyComplete();

        Mono<String> stringMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).delete()
                .uri("/roles/organizations/" +organizationId)
                .headers(addJwt(jwt)).exchange().returnResult(String.class).getResponseBody().single();

        StepVerifier.create(stringMono).assertNext(string -> {
            LOG.info("role deleted response: {}", string);
            assertThat(string).contains("delete my role success for orgId: '"+organizationId+"' and userId: '"+userId+"'");
        }).verifyComplete();

        //no data emitted after deleting objects associated with organizationId
        StepVerifier.create(roleRepository.findByOrganizationId(organizationId)).verifyComplete();

        StepVerifier.create(clientOrganizationUserRoleRepository.findById(clientOrganizationUserRole1.getId())).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization.getId())).assertNext(exists ->{
            assertThat(exists).isFalse();
        }).verifyComplete();

    }

    /**
     * in this test delete ClientOrganizationUserRole and AuthzManagerRoleOrganization associated
     * with the organization, leave the role alone as there will be other users associated to it.
     */
    @Test
    public void deleteMyClientOrganizationUserRoleAndAuthzManagerRoleOrganizationOnly() {
        final String authenticationId = "sonam";
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(authenticationId, userId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        var role = new Role(roleId, "admin role", organizationId);
        role.setNew(true);

        roleRepository.save(role).subscribe();

        var clientOrganizationUserRole1 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole1).subscribe();

        var clientOrganizationUserRole2 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, UUID.randomUUID());
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole2).subscribe();

        //(UUID id, UUID authzManagerRoleId, UUID organizationId, UUID userId) {
        UUID authzManagerRoleId = UUID.randomUUID();

        var authzManagerRoleOrganization = new AuthzManagerRoleOrganization(null, authzManagerRoleId, organizationId, userId);
        authzManagerRoleOrganizationRepository.save(authzManagerRoleOrganization).subscribe();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization.getId())).assertNext(boolValue -> {
            assertThat(boolValue).isTrue();
        }).verifyComplete();

        Mono<String> stringMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).delete()
                .uri("/roles/organizations/" +organizationId)
                .headers(addJwt(jwt)).exchange().returnResult(String.class).getResponseBody().single();

        StepVerifier.create(stringMono).assertNext(string -> {
            LOG.info("role deleted response: {}", string);
            assertThat(string).contains("delete my role success for orgId: '"+organizationId+"' and userId: '"+userId+"'");
        }).verifyComplete();

        //no data emitted after deleting objects associated with organizationId
        StepVerifier.create(roleRepository.countByOrganizationId(organizationId))
                .assertNext(count -> assertThat(count).isEqualTo(1)).verifyComplete(); //role should exist

        StepVerifier.create(clientOrganizationUserRoleRepository.findById(clientOrganizationUserRole1.getId())).verifyComplete();

        StepVerifier.create(clientOrganizationUserRoleRepository.findById(clientOrganizationUserRole2.getId()))
                .assertNext(clientOrganizationUserRole -> assertThat(clientOrganizationUserRole.getId())
                        .isEqualTo(clientOrganizationUserRole2.getId())).verifyComplete();

        StepVerifier.create(authzManagerRoleOrganizationRepository.existsById(authzManagerRoleOrganization.getId())).assertNext(exists ->{
            assertThat(exists).isFalse();
        }).verifyComplete();

    }

    //
    @Test
    public void getCountInClientOrganizatioUser() {
        final String authenticationId = "sonam";
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwt(authenticationId, userId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        var clientOrganizationUserRole1 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole1).subscribe();

        var clientOrganizationUserRole2 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, UUID.randomUUID());
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole2).subscribe();

        Mono<Map<String, Integer>> mapMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).get()
                .uri("/roles/organizations/"+organizationId+"/count")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<Map<String,  Integer>>() {}).getResponseBody().single();

        StepVerifier.create(mapMono).assertNext(map -> {
            LOG.info("got count: {}", map);
            assertThat(map.get("message")).isEqualTo(1);
        }).verifyComplete();

        clientOrganizationUserRoleRepository.deleteById(clientOrganizationUserRole2.getId()).subscribe();

        LOG.info("deleted one row, should have 0 now");

        mapMono = webTestClient.mutateWith(mockJwt().jwt(jwt)).get()
                .uri("/roles/organizations/"+organizationId+"/count")
                .headers(addJwt(jwt)).exchange().returnResult(new ParameterizedTypeReference<Map<String,  Integer>>() {}).getResponseBody().single();

        StepVerifier.create(mapMono).assertNext(map -> {
            LOG.info("got count: {}", map);
            assertThat(map.get("message")).isEqualTo(0);
        }).verifyComplete();
    }

    @Test
    public void getRoleNameForClientOrganizationUser() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("clientId", clientId);
        map.put("organizationId", organizationId);
        map.put("userId", userId1);
        map.put("roleId", roleId);

        var role = new Role(roleId, "admin role", organizationId);
        role.setNew(true);

        roleRepository.save(role).subscribe();

        //e(UUID id, UUID roleId, UUID clientId, UUID organizationId, UUID userId) {
        var clientOrganizationUserRole1 = new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId1);
        clientOrganizationUserRoleRepository.save(clientOrganizationUserRole1).subscribe();

        String roleName = webTestClient.mutateWith(mockJwt().jwt(jwt)).get()
                .uri("/roles/clients/"+clientId+"/organizations/"+organizationId+"/users/"+userId1+"/roles/name")
                .headers(addJwt(jwt)).exchange().expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(roleName).isEqualTo(role.getName());
        assertThat(roleName).isEqualTo("admin role");
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

