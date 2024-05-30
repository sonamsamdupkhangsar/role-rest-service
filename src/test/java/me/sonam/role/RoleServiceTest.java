package me.sonam.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.role.handler.service.carrier.ClientOrganizationUserWithRole;
import me.sonam.role.handler.service.carrier.User;
import me.sonam.role.repo.RoleOrganizationRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.ClientUserRoleRepository;
import me.sonam.role.repo.RoleUserRepository;
import me.sonam.role.repo.entity.ClientOrganizationUserRole;
import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.ClientUserRole;
import me.sonam.role.repo.entity.RoleOrganization;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
    private ClientUserRoleRepository clientUserRoleRepository;
    @Autowired
    private RoleOrganizationRepository roleOrganizationRepository;

    @Autowired
    private RoleUserRepository roleUserRepository;

    private Role createRoleByOrganizationId(UUID creatorId, UUID clientId, boolean shouldExistBefore, UUID organizationId, String roleName, HttpStatus httpStatus) {
        LOG.info("create role {}", roleName);
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        //UUID clientId = UUID.randomUUID();
       // UUID userId = UUID.randomUUID();

        Page<Role> roleRestPage = getRolesOwnedByOrganization(organizationId);

        List<String> roleNames = roleRestPage.getContent().stream().map(role ->role.getName()).collect(toList());

        LOG.info("should {} contain role '{}' role at first", shouldExistBefore, roleName);
        assertThat(roleNames.contains(roleName)).isEqualTo(shouldExistBefore);

        var mapBody = Map.of("organizationId", organizationId.toString(),
                "name", roleName,
                "clientId", clientId.toString(),
                "userId", creatorId.toString());

        EntityExchangeResult<Role> entityExchangeResult = webTestClient.post().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isEqualTo(httpStatus).expectBody(Role.class)
                .returnResult();
        LOG.info("created roleName: {} with id: {}", roleName, entityExchangeResult.getResponseBody().getId());


        roleRestPage = getRolesOwnedByOrganization(organizationId);

        roleNames = roleRestPage.getContent().stream().map(role -> role.getName()).collect(toList());

        LOG.info("now we should have a role '{}' from getPage call", roleName);
        assertThat(roleNames.contains(roleName)).isTrue();

        if (httpStatus.equals(HttpStatus.CREATED)) {
            assertRoles(Map.of(), jwt, clientId);
        }

        if (entityExchangeResult.getResponseBody().getId() != null) {
            //return UUID.fromString(entityExchangeResult.getResponseBody().getId().toString());
            return entityExchangeResult.getResponseBody();
        }
        else {
            LOG.error("returing a random uuid when null");
            //return UUID.randomUUID();
            return null;
        }

    }

    /**
     * this is for creating a role owned by a user (not by a organization)
     * @param creatorId
     * @param clientId
     * @param shouldExistBefore
     * @param roleName
     * @param httpStatus
     * @return
     */
    private Role createRoleByUser(UUID creatorId, UUID clientId, boolean shouldExistBefore, String roleName, HttpStatus httpStatus) {
        LOG.info("create role {}", roleName);
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        List<Role> list = getRolesOwnedByUser(creatorId);
        List<String> roleNames = list.stream().map(role -> role.getName()).collect(toList());

        LOG.info("should {} contain role '{}' role at first", shouldExistBefore, roleName);
        assertThat(roleNames.contains(roleName)).isEqualTo(shouldExistBefore);

        var mapBody = Map.of(
                "name", roleName,
                "clientId", clientId.toString(),
                "userId", creatorId.toString());

        EntityExchangeResult<Role> entityExchangeResult = webTestClient.post().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isEqualTo(httpStatus).expectBody(Role.class)
                .returnResult();
        LOG.info("created roleName: {} with id: {}", roleName, entityExchangeResult.getResponseBody().getId());

        list  = getRolesOwnedByUser(creatorId);

        roleNames = list.stream().map(role -> role.getName()).collect(toList());

        LOG.info("now we should have a role '{}' from getPage call", roleName);
        assertThat(roleNames.contains(roleName)).isTrue();

        if (httpStatus.equals(HttpStatus.CREATED)) {
            assertRoles(Map.of(), jwt, clientId);
        }

        return entityExchangeResult.getResponseBody();
       /* if (entityExchangeResult.getResponseBody().get("id") != null) {
            return UUID.fromString(entityExchangeResult.getResponseBody().get("id").toString());
        }
        else {
            LOG.error("returing a random uuid when null");
            return UUID.randomUUID();
        }
*/
    }
    @Test
    public void createRoleOwnedByOrganization() {
        LOG.info("create role owned by organization");

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Role role = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        LOG.info("now create the same role in organizationId");
        Role role2 = createRoleByOrganizationId(creatorId, clientId, true, organizationId, "user", HttpStatus.BAD_REQUEST);

    }

    @Test
    public void createRoleOwnedByUser() {
        LOG.info("create role owned by user");

        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Role userRoleId = createRoleByUser(creatorId, clientId, false, "user", HttpStatus.CREATED);

        LOG.info("now create the same role in organizationId");
        Role userRoleId2 = createRoleByUser(creatorId, clientId, true,"user", HttpStatus.BAD_REQUEST);

    }

    @AfterEach
    public void deleteAllRoles() {
        roleRepository.deleteAll().subscribe();
        clientUserRoleRepository.deleteAll().subscribe();
    }
    @Test
    public void updateRole() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Role role = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);


        LOG.debug("build a map with admin name for role update");
        var mapBody = Map.of("id", role.getId(), "userId", creatorId, "name", "admin");

        EntityExchangeResult<Role> entityExchangeResult = webTestClient.put().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isOk().expectBody(Role.class)
                .returnResult();
        LOG.info("update roleName: {}", entityExchangeResult.getResponseBody().getName());

        Page<Role> rolePage = getRolesOwnedByOrganization(organizationId);
        assertThat(rolePage.getTotalElements()).isEqualTo(1);
        assertThat(rolePage.getContent().get(0)).isNotEqualTo(role);
    }

    @Test
    public void delete() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        LOG.info("create role for organization as owner");
        Role role = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        StepVerifier.create(roleOrganizationRepository.existsByRoleIdAndOrganizationId(role.getId(), organizationId))
                .expectNext(true).verifyComplete();

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.delete().uri("/roles/"+role.getId())
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("update roleName: {}", entityExchangeResult.getResponseBody().get("message"));

        Page<Role> rolePage = getRolesOwnedByOrganization(organizationId);
        assertThat(rolePage.getTotalElements()).isEqualTo(0);

        /*List<String> listOfNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        assertThat(listOfNames.contains("user")).isFalse();
*/
        StepVerifier.create(roleUserRepository.existsByRoleIdAndUserId(role.getId(), creatorId))
                .expectNext(false).verifyComplete();
        StepVerifier.create(roleOrganizationRepository.existsByRoleIdAndOrganizationId(role.getId(), organizationId))
                .expectNext(false).verifyComplete();
    }

    @Test
    public void getRoleById() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        LOG.info("create user role");
        Role role = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.get().uri("/roles/"+role.getId())
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("retrieved role by id: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody().get("id")).isEqualTo(role.getId().toString());
        assertThat(entityExchangeResult.getResponseBody().get("organizationId")).isNull();
        assertThat(entityExchangeResult.getResponseBody().get("name")).isEqualTo("user");
    }


    @Test
    public void getByOrganiationIdWithPages() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();
        UUID companyId3 = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        LOG.info("create user role");
        Role userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        Role adminRoleId = createRoleByOrganizationId(creatorId, clientId, false,companyId1, "admin", HttpStatus.CREATED);
        Role emplyeeRoleId = createRoleByOrganizationId(creatorId, clientId, false,companyId1, "employee", HttpStatus.CREATED);
        Role managerRoleId = createRoleByOrganizationId(creatorId, clientId, false,companyId1, "manager", HttpStatus.CREATED);
        LOG.info("userRoleId: {}", userRoleId);

        Page<Role> roleRestPage = getRolesOwnedByOrganization(companyId1);
        assertThat(roleRestPage.getContent().containsAll(List.of(userRoleId, adminRoleId, emplyeeRoleId, managerRoleId))).isTrue();

        List<String> listOfNames = roleRestPage.getContent().stream().map(Role::getName).toList();

        assertThat(listOfNames.contains("user")).isTrue();
        assertThat(listOfNames.contains("admin")).isTrue();
        assertThat(listOfNames.contains("employee")).isTrue();
        assertThat(listOfNames.contains("manager")).isTrue();
        assertThat(listOfNames.contains("person")).isFalse();


        Role companyId2managerRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId2, "manager", HttpStatus.CREATED);
        Role companyId2userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId2, "user", HttpStatus.CREATED);

        roleRestPage = getRolesOwnedByOrganization(companyId2);
        assertThat(companyId2managerRoleId).isNotNull();
        assertThat(companyId2userRoleId).isNotNull();
        assertThat(roleRestPage.getContent().containsAll(List.of(companyId2managerRoleId, companyId2userRoleId))).isTrue();


        listOfNames = roleRestPage.getContent().stream().map(Role::getName).toList();
        assertThat(listOfNames.size()).isEqualTo(2);

        assertThat(listOfNames.contains("manager")).isTrue();
        assertThat(listOfNames.contains("user")).isTrue();


        Role companyId3managerRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId3, "manager", HttpStatus.CREATED);

        roleRestPage = getRolesOwnedByOrganization(companyId3);
        assertThat(roleRestPage.getContent().contains(companyId3managerRoleId)).isTrue();
        assertThat(roleRestPage.getTotalElements()).isEqualTo(1);

        listOfNames = roleRestPage.getContent().stream().map(Role::getName).toList();
        assertThat(listOfNames.size()).isEqualTo(1);

        assertThat(listOfNames.contains("manager")).isTrue();
    }

    @Test
    public void addRoleToOrganization() {
        LOG.info("add role to organization");

        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Role userRoleId = createRoleByUser(creatorId, clientId, false, "user", HttpStatus.CREATED);

        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID roleId = userRoleId.getId();
        UUID organizationId = UUID.randomUUID();

        Map<String, Object> map = new HashMap<>();
        map.put("roleId", roleId);
        map.put("organizationId", organizationId);

        EntityExchangeResult<RoleOrganization> entityExchangeResult = webTestClient.post().uri("/roles/organizations")
                .headers(addJwt(jwt)).bodyValue(map).exchange().expectStatus().isOk().expectBody(RoleOrganization.class)
                .returnResult();

        RoleOrganization roleOrganization = entityExchangeResult.getResponseBody();

        assertThat(roleOrganization.getId()).isNotNull();

        EntityExchangeResult<Role> roleResult = webTestClient.get().uri("/roles/"+userRoleId.getId())
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Role.class)
                .returnResult();
        LOG.info("retrieved role by id: {}", roleResult.getResponseBody());

        assertThat(roleResult.getResponseBody().getId()).isEqualTo(userRoleId.getId());
        assertThat(roleResult.getResponseBody().getRoleOrganization()).isNotNull();
        assertThat(roleResult.getResponseBody().getName()).isEqualTo("user");



        LOG.info("delete the created role organization");
        EntityExchangeResult<Map<String, String>> mapResult = webTestClient.delete().uri("/roles/"+userRoleId.getId()+"/organizations/"+roleOrganization.getOrganizationId())
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<Map<String, String>>(){})
                .returnResult();

        assertThat(mapResult.getResponseBody().get("message")).isEqualTo("roleOrganization deleted");

        mapResult = webTestClient.delete().uri("/roles/"+ userRoleId.getId()+"/organizations/"+roleOrganization.getOrganizationId())
                .headers(addJwt(jwt)).exchange().expectStatus().isBadRequest().expectBody(new ParameterizedTypeReference<Map<String, String>>(){})
                .returnResult();

        assertThat(mapResult.getResponseBody().get("error")).isNotEmpty();


    }



    public Page<Role> getRolesOwnedByOrganization(UUID organizationId) {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

            LOG.info("get roles owned by organization");
        EntityExchangeResult<RestPage<Role>> entityExchangeResult = webTestClient.get().uri("/roles/organizations/"+organizationId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<RestPage<Role>>() {
                })
                .returnResult();

        LOG.info("roles found: {}", entityExchangeResult.getResponseBody());

        return entityExchangeResult.getResponseBody();
    }

    public List<Role> getRolesOwnedByUser(UUID userId) {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        LOG.info("get roles owned by user");
        EntityExchangeResult<RestPage<Role>> entityExchangeResult = webTestClient.get().uri("/roles/user-id/"+userId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<RestPage<Role>>(){})
                .returnResult();

        RestPage<Role> roleRestPage = entityExchangeResult.getResponseBody();
        LOG.info("roles found: {}", roleRestPage.getContent());
        List<Role> list = roleRestPage.getContent();

        return list;

    }

    @Test
    public void clientUserRoleAssociation() {
        UUID clientId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        UUID creatorId = UUID.randomUUID();

        LOG.info("create user role");
        Role userRoleId =  createRoleByUser(creatorId, clientId, false, "user", HttpStatus.CREATED);
        //createRoleByOrganizationId(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        Role adminRoleId = createRoleByUser(creatorId, clientId, false, "admin", HttpStatus.CREATED);
        // createRoleByOrganizationId(creatorId, clientId, false, companyId1, "admin", HttpStatus.CREATED);

        List<Map> mapList = Arrays.asList(Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", userRoleId.getId()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId2.toString(),
                        "roleId", adminRoleId.getId()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId3.toString(),
                        "roleId", userRoleId.getId()));

        Map<String, Object> map1 = Map.of("clientId", clientId.toString(),
                "userId", userId1.toString(),
                "roleId", userRoleId.getId());

        Map<String, Object> map2 = Map.of("clientId", clientId.toString(),
                        "userId", userId2.toString(),
                        "roleId", adminRoleId.getId());

        Map<String, Object> map3 = Map.of("clientId", clientId.toString(),
                        "userId", userId3.toString(),
                        "roleId", userRoleId.getId());

        LOG.info("add users to role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        ObjectMapper mapper = new ObjectMapper();

        ClientUserRole clientUserRole1 = addClientUserRole(jwt, map1, mapper);
        ClientUserRole clientUserRole2 = addClientUserRole(jwt, map2, mapper);
        ClientUserRole clientUserRole3 = addClientUserRole(jwt, map3, mapper);

        ClientUserRole clientUserRoleUpdate = new ClientUserRole(clientUserRole1.getId(), clientUserRole1.getClientId(),
                clientUserRole1.getUserId(), adminRoleId.getId());

        updateClientUserRole(jwt, clientUserRoleUpdate, mapper);
        clientUserRoleUpdate = new ClientUserRole(clientUserRole2.getId(), clientUserRole2.getClientId(),
                clientUserRole2.getUserId(), userRoleId.getId());

        updateClientUserRole(jwt, clientUserRoleUpdate, mapper);

        clientUserRoleUpdate = new ClientUserRole(clientUserRole3.getId(), clientUserRole3.getClientId(),
                clientUserRole3.getUserId(), adminRoleId.getId());

        updateClientUserRole(jwt, clientUserRoleUpdate, mapper);

        Map<UUID, ClientUserRole> map = getClientUserRole(jwt, clientId,  mapper);
        assertThat(map.get(clientUserRole1.getId())).isNotNull();
        assertThat(map.get(clientUserRole1.getId()).getRoleId()).isNotNull();
        assertThat(map.get(clientUserRole1.getId()).getRoleId()).isEqualTo(adminRoleId.getId());

        assertThat(map.get(clientUserRole2.getId())).isNotNull();
        assertThat(map.get(clientUserRole2.getId()).getRoleId()).isNotNull();
        assertThat(map.get(clientUserRole2.getId()).getRoleId()).isEqualTo(userRoleId.getId());

        assertThat(map.get(clientUserRole3.getId())).isNotNull();
        assertThat(map.get(clientUserRole3.getId()).getRoleId()).isEqualTo(adminRoleId.getId());


        deleteClientUserRole(jwt, adminRoleId.getId(), userId3);
        map = getClientUserRole(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(2);
        assertThat(map.get(clientUserRole3.getId())).isNull();
        assertThat(map.get(clientUserRole1.getId())).isNotNull();
        assertThat(map.get(clientUserRole2.getId())).isNotNull();

        deleteClientUserRole(jwt, adminRoleId.getId(), userId1);
        map = getClientUserRole(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get(clientUserRole3.getId())).isNull();
        assertThat(map.get(clientUserRole1.getId())).isNull();
        assertThat(map.get(clientUserRole2.getId())).isNotNull();

        LOG.info("deleteRoleClientUser with not associated userId 2 and admin roleId");
        deleteClientUserRole(jwt, adminRoleId.getId(), userId2);
        map = getClientUserRole(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get(clientUserRole3.getId())).isNull();
        assertThat(map.get(clientUserRole1.getId())).isNull();
        assertThat(map.get(clientUserRole2.getId())).isNotNull();

        deleteClientUserRole(jwt, userRoleId.getId(), userId2);
        map = getClientUserRole(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(0);
        assertThat(map.get(clientUserRole3.getId())).isNull();
        assertThat(map.get(clientUserRole1.getId())).isNull();
        assertThat(map.get(clientUserRole2.getId())).isNull();
    }

    /**
     * this method will test clientOrganizationUsers add, getClientUsersRoles by orgId and userIds and delete by id
     */
    @Test
    public void addClientOrganizationUsers() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID clientId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();


        Role userRole = createRoleByOrganizationId(userId, clientId, false, organizationId, "user", HttpStatus.CREATED);
        assertThat(userRole).isNotNull();
        assertThat(userRole.getId()).isNotNull();

        User user = new User(userId, userRole);
        ClientOrganizationUserWithRole clientOrganizationUserWithRole = new ClientOrganizationUserWithRole(null, clientId, organizationId, user);

        ClientOrganizationUserRole clientOrganizationUserRole = addClientOrganizationUserWithRole(jwt, clientOrganizationUserWithRole);

        assertThat(clientOrganizationUserRole.getOrganizationId()).isEqualTo(organizationId);
        assertThat(clientOrganizationUserRole.getClientId()).isEqualTo(clientId);
        assertThat(clientOrganizationUserRole.getUserId()).isEqualTo(user.getId());
        assertThat(clientOrganizationUserRole.getRoleId()).isEqualTo(userRole.getId());

        List<ClientOrganizationUserWithRole> clientOrganizationUserWithRoles = getClientOrganizationUserWithRoles(clientId, jwt, organizationId, List.of(userId));
        assertThat(clientOrganizationUserWithRoles.size()).isEqualTo(1);
        assertThat(clientOrganizationUserWithRoles.get(0).getUser().getId()).isEqualTo(userId);
        assertThat(clientOrganizationUserWithRoles.get(0).getUser().getRole().getUserId()).isEqualTo(userId);
        assertThat(clientOrganizationUserWithRoles.get(0).getUser().getRole().getId()).isEqualTo(userRole.getId());
        assertThat(clientOrganizationUserWithRoles.get(0).getUser().getRole().getName()).isEqualTo(userRole.getName());
        assertThat(clientOrganizationUserWithRoles.get(0).getClientId()).isEqualTo(clientId);
        assertThat(clientOrganizationUserWithRoles.get(0).getOrganizationId()).isEqualTo(organizationId);

        deleteClientOrganizationUser(clientOrganizationUserWithRoles.get(0).getId(), jwt);
        clientOrganizationUserWithRoles = getClientOrganizationUserWithRoles(clientId, jwt, organizationId, List.of(userId));
        assertThat(clientOrganizationUserWithRoles.size()).isEqualTo(0);
    }

    private void deleteClientOrganizationUser(UUID id, Jwt jwt) {
        LOG.info("delete clientOrganizationUser by id");

        EntityExchangeResult<String> entityExchangeResult = webTestClient.delete().uri("/roles/client-organization-users/"+id).headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult();

        assertThat(entityExchangeResult.getResponseBody()).isEqualTo("roleClientOrganizationUser deleted");
    }

    private List<ClientOrganizationUserWithRole> getClientOrganizationUserWithRoles(UUID clientId, Jwt jwt, UUID organizationId, List<UUID> list) {
        LOG.info("call endpoint o get client organization user with roles");

        String csvUuid = list.stream().map(uuid -> uuid + ",").collect(Collectors.joining());
        LOG.info("csvUuid: {}", csvUuid);
        if (csvUuid.endsWith(",")) {
            int index = csvUuid.lastIndexOf(",");
            csvUuid = csvUuid.substring(0, index);
        }
        LOG.info("after removal of last , {}", csvUuid);

        return webTestClient.get().uri("/roles/client-organization-users/client-id/"+clientId+"/organization-id/"+organizationId+"/user-ids/"+ csvUuid)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<List<ClientOrganizationUserWithRole>>() {})
                .returnResult().getResponseBody();
    }

    private ClientOrganizationUserRole addClientOrganizationUserWithRole(Jwt jwt, ClientOrganizationUserWithRole clientOrganizationUserWithRole) {
        LOG.info("call endpoint to add clientOrganizationUserWithRole");

        EntityExchangeResult<ClientOrganizationUserRole> entityExchangeResult = webTestClient.post()
                .uri("/roles/client-organization-users")
                .headers(addJwt(jwt)).bodyValue(clientOrganizationUserWithRole)
                .exchange().expectStatus().isOk().expectBody(ClientOrganizationUserRole.class).returnResult();

        assertThat(entityExchangeResult.getResponseBody()).isNotNull();
        return entityExchangeResult.getResponseBody();
    }
    //delete ClientUserRole by roleId and userId
    private void deleteClientUserRole(final Jwt jwt, UUID adminRoleId, UUID userId) {
        webTestClient.delete()
                .uri("/roles/client-users/role-id/"+adminRoleId+"/user-id/"+userId).headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
    }

    private ClientUserRole addClientUserRole(final Jwt jwt, Map<String, Object> map, ObjectMapper mapper) {
        EntityExchangeResult<Map<String, Object>> entityExchangeResult = webTestClient.post().uri("/roles/client-users")
                .headers(addJwt(jwt)).bodyValue(map)
                .exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<Map<String, Object>>(){}).returnResult();
        LOG.info("response from adding user client roles: {}", entityExchangeResult.getResponseBody());
        assertThat(entityExchangeResult.getResponseBody().get("message")).isEqualTo("created new role client user row");

        ClientUserRole clientUserRole = mapper.convertValue(entityExchangeResult.getResponseBody().get("object"), ClientUserRole.class);

        assertThat(clientUserRole.getUserId()).isEqualTo(UUID.fromString(map.get("userId").toString()));
        assertThat(clientUserRole.getClientId()).isEqualTo(map.get("clientId"));
        assertThat(clientUserRole.getRoleId()).isEqualTo(UUID.fromString(map.get("roleId").toString()));
        LOG.info("assert role, clientId and userId matches from the map: {}", clientUserRole);

        return clientUserRole;
    }

    private void updateClientUserRole(final Jwt jwt, ClientUserRole clientUserRole, ObjectMapper mapper) {
        LOG.info("update role client user");

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.put()
                .uri("/roles/client-users").headers(addJwt(jwt)).bodyValue(clientUserRole)
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        assertThat(entityExchangeResult.getResponseBody().get("message"))
                    .isEqualTo("updated role client user with id");
    }

    private Map<UUID, ClientUserRole> getClientUserRole(final Jwt jwt, final UUID clientId, ObjectMapper mapper) {
        LOG.info("get role user client by page");
        EntityExchangeResult<RestPage<ClientUserRole>> pageResult = webTestClient.get().uri("/roles/client-users/client-id/" + clientId)
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<RestPage<ClientUserRole>>() {
                }).returnResult();

        LOG.info("pageResult pageable {}", pageResult.getResponseBody().getPageable());
        LOG.info("assert roles size for clientId: {}", pageResult.getResponseBody().getContent().size());
        //Assertions.assertThat(pageResult.getResponseBody().getContent().size()).isEqualTo(userIdRoleMap.size());

        Map<UUID, ClientUserRole> map = new HashMap<>();
        pageResult.getResponseBody().getContent().forEach(clientUserRole -> {
            LOG.info("roleClientUser from page: {}", clientUserRole);
            map.put(clientUserRole.getId(), clientUserRole);
        });

        return map;
    }

    private void modifyRoles(List<ClientUserRole> clientUserRoles, UUID clientId, UUID userId1, UUID userId2, UUID userId3, UUID adminRoleId, UUID userRoleId) {
        LOG.info("test with modified roles");

        List<ClientUserRole> updateClientUserRoles = new ArrayList<>();
        Map<UUID, String> map = new HashMap<>();//Map.of(userId1.toString(), "admin", userId3.toString(), "user");

        for(ClientUserRole clientUserRole : clientUserRoles) {
            if (clientUserRole.getRoleId().equals(userRoleId)) {
                updateClientUserRoles.add(new ClientUserRole(clientUserRole.getId(), clientUserRole.getClientId(), clientUserRole.getUserId(), adminRoleId));
                LOG.info("update role for roleClientUser from {} to {}", clientUserRole.getRoleId(), adminRoleId);

                map.put(clientUserRole.getUserId(), adminRoleId.toString());
            }
            else if (clientUserRole.getRoleId().equals(adminRoleId)) {
                updateClientUserRoles.add(new ClientUserRole(clientUserRole.getId(), clientUserRole.getClientId(), clientUserRole.getUserId(), userRoleId));
                LOG.info("update role for roleClientUser from {} to {}", clientUserRole.getRoleId(), userRoleId);
                map.put(clientUserRole.getUserId(), userRoleId.toString());
            }
            else {
                LOG.error("roleId didn't match to userRoleId or amdinRoleId");
            }

        }

        LOG.info("add users to role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));



        LOG.info("update role user");
        for (ClientUserRole clientUserRole : updateClientUserRoles) {
            EntityExchangeResult<Map> entityExchangeResult = webTestClient.put()
                    .uri("/roles/user").headers(addJwt(jwt)).bodyValue(clientUserRole)
                    .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
            LOG.info("result: {}", entityExchangeResult.getResponseBody());
            assertThat(entityExchangeResult.getResponseBody().get("message"))
                    .isEqualTo("updated role client user with id");

        }

        /*webTestClient.delete()
                .uri("/roles/"+adminRoleId+"/users/"+userId2).headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
*/


        LOG.info("assert the roles with the userId");
        assertRoles(map, jwt, clientId);
    }

    private void assertRoles(Map<UUID, String> userIdRoleMap, Jwt jwt, UUID clientId) {

        LOG.info("get applications by id and all users in it, which should give 4 applicationUsers");
        EntityExchangeResult<RestPage<ClientUserRole>> pageResult = webTestClient.get().uri("/roles/client-users/client-id/" + clientId)
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<RestPage<ClientUserRole>>() {
                }).returnResult();

        LOG.info("pageResult pageable {}", pageResult.getResponseBody().getPageable());
        LOG.info("assert roles size for clientId: {}", pageResult.getResponseBody().getContent().size());
        Assertions.assertThat(pageResult.getResponseBody().getContent().size()).isEqualTo(userIdRoleMap.size());

        pageResult.getResponseBody().forEach(clientUserRole -> {
            assertThat(userIdRoleMap.get(clientUserRole.getId())).isNotNull();
            assertThat(userIdRoleMap.get(clientUserRole.getId())).isNotNull();
            assertThat(userIdRoleMap.get(clientUserRole.getId())).isEqualTo(clientUserRole.getRoleName());
        });

    }

    @Test
    public void getRoleByClientIdAndUserId() {
        UUID clientId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        UUID creatorId = UUID.randomUUID();
        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();
        UUID companyId3 = UUID.randomUUID();

        LOG.info("create user role");
        Role userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        Role adminRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "admin", HttpStatus.CREATED);

        List<Map<String, Object>> mapList = Arrays.asList(
                Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", userRoleId.getId()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", adminRoleId.getId()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId2.toString(),
                        "roleId", adminRoleId.getId()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId3.toString(),
                        "roleId", userRoleId.getId()));

        LOG.info("add users to role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));


        mapList.forEach(map -> {
            EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/roles/client-users").headers(addJwt(jwt)).bodyValue(map)
                    .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        });
        LOG.info("made role user associations");

        LOG.info("get role by clientId and userId");

        EntityExchangeResult<List> roleByClientIdAndUserIdResult = webTestClient.get()
                .uri("/roles/client-users/client-id/" + clientId + "/user-id/" + userId1).headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(List.class).returnResult();

        LOG.info("list {}", roleByClientIdAndUserIdResult.getResponseBody());

        List<LinkedHashMap> list = roleByClientIdAndUserIdResult.getResponseBody();
        assertThat(list.size()).isEqualTo(2);// 2 roles should be found admin and user
        LinkedHashMap<String, String> linkedHashMap = list.get(0);

        LOG.info("role: {}", linkedHashMap);
        assertThat(linkedHashMap.get("roleId")).isNotNull();
        assertThat(linkedHashMap.get("roleName")).isEqualTo("user");
        assertThat(linkedHashMap.get("clientId")).isEqualTo(clientId.toString());
        assertThat(linkedHashMap.get("userId")).isEqualTo(userId1.toString());

        linkedHashMap = list.get(1);

        LOG.info("role: {}", linkedHashMap);
        assertThat(linkedHashMap.get("roleId")).isNotNull();
        assertThat(linkedHashMap.get("roleName")).isEqualTo("admin");
        assertThat(linkedHashMap.get("clientId")).isEqualTo(clientId.toString());
        assertThat(linkedHashMap.get("userId")).isEqualTo(userId1.toString());
    }

    private Jwt jwt(String subjectName) {
        return new Jwt("token", null, null,
                Map.of("alg", "none"), Map.of("sub", subjectName));
    }

    private Consumer<HttpHeaders> addJwt(Jwt jwt) {
        return headers -> headers.setBearerAuth(jwt.getTokenValue());
    }


}

