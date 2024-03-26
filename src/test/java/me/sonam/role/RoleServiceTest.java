package me.sonam.role;

import arrow.typeclasses.Hash;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sonam.role.repo.RoleOrganizationRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.RoleClientUserRepository;
import me.sonam.role.repo.RoleUserRepository;
import me.sonam.role.repo.entity.RoleClientUser;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private RoleClientUserRepository roleClientUserRepository;
    @Autowired
    private RoleOrganizationRepository roleOrganizationRepository;

    @Autowired
    private RoleUserRepository roleUserRepository;

    private UUID createRoleByOrganizationId(UUID creatorId, UUID clientId, boolean shouldExistBefore, UUID organizationId, String roleName, HttpStatus httpStatus) {
        LOG.info("create role {}", roleName);
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        //UUID clientId = UUID.randomUUID();
       // UUID userId = UUID.randomUUID();

        List<Map> list = getRolesOwnedByOrganization(organizationId);
        List<String> roleNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        LOG.info("should {} contain role '{}' role at first", shouldExistBefore, roleName);
        assertThat(roleNames.contains(roleName)).isEqualTo(shouldExistBefore);

        var mapBody = Map.of("organizationId", organizationId.toString(),
                "name", roleName,
                "clientId", clientId.toString(),
                "userId", creatorId.toString());

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.post().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isEqualTo(httpStatus).expectBody(Map.class)
                .returnResult();
        LOG.info("created roleName: {} with id: {}", roleName, entityExchangeResult.getResponseBody().get("id"));


        list  = getRolesOwnedByOrganization(organizationId);

        roleNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        LOG.info("now we should have a role '{}' from getPage call", roleName);
        assertThat(roleNames.contains(roleName)).isTrue();

        if (httpStatus.equals(HttpStatus.CREATED)) {
            assertRoles(Map.of(), jwt, clientId);
        }

        if (entityExchangeResult.getResponseBody().get("id") != null) {
            return UUID.fromString(entityExchangeResult.getResponseBody().get("id").toString());
        }
        else {
            LOG.error("returing a random uuid when null");
            return UUID.randomUUID();
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
    private UUID createRoleByUser(UUID creatorId, UUID clientId, boolean shouldExistBefore, String roleName, HttpStatus httpStatus) {
        LOG.info("create role {}", roleName);
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        List<Map> list = getRolesOwnedByUser(creatorId);
        List<String> roleNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        LOG.info("should {} contain role '{}' role at first", shouldExistBefore, roleName);
        assertThat(roleNames.contains(roleName)).isEqualTo(shouldExistBefore);

        var mapBody = Map.of(
                "name", roleName,
                "clientId", clientId.toString(),
                "userId", creatorId.toString());

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.post().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isEqualTo(httpStatus).expectBody(Map.class)
                .returnResult();
        LOG.info("created roleName: {} with id: {}", roleName, entityExchangeResult.getResponseBody().get("id"));

        list  = getRolesOwnedByUser(creatorId);

        roleNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        LOG.info("now we should have a role '{}' from getPage call", roleName);
        assertThat(roleNames.contains(roleName)).isTrue();

        if (httpStatus.equals(HttpStatus.CREATED)) {
            assertRoles(Map.of(), jwt, clientId);
        }

        if (entityExchangeResult.getResponseBody().get("id") != null) {
            return UUID.fromString(entityExchangeResult.getResponseBody().get("id").toString());
        }
        else {
            LOG.error("returing a random uuid when null");
            return UUID.randomUUID();
        }

    }
    @Test
    public void createRoleOwnedByOrganization() {
        LOG.info("create role owned by organization");

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        LOG.info("now create the same role in organizationId");
        UUID userRoleId2 = createRoleByOrganizationId(creatorId, clientId, true, organizationId, "user", HttpStatus.BAD_REQUEST);

    }

    @Test
    public void createRoleOwnedByUser() {
        LOG.info("create role owned by user");

        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        UUID userRoleId = createRoleByUser(creatorId, clientId, false, "user", HttpStatus.CREATED);

        LOG.info("now create the same role in organizationId");
        UUID userRoleId2 = createRoleByUser(creatorId, clientId, true,"user", HttpStatus.BAD_REQUEST);

    }

    @AfterEach
    public void deleteAllRoles() {
        roleRepository.deleteAll().subscribe();
        roleClientUserRepository.deleteAll().subscribe();
    }
    @Test
    public void update() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);


        LOG.debug("build a map with admin name for role update");
        var mapBody = Map.of("id", userRoleId.toString(), "organizationId", organizationId.toString(), "name", "admin");

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.put().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("update roleName: {}", entityExchangeResult.getResponseBody().get("message"));

        List<Map> list = getRolesOwnedByOrganization(organizationId);
        List<String> listOfNames = list.stream().map(map -> map.get("name").toString()).collect(toList());


        LOG.info("should not contain user role at first");
        assertThat(listOfNames.contains("admin")).isTrue();

        LOG.debug("we should not have user roleName after updating that value");
        assertThat(listOfNames.contains("user")).isFalse();
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
        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        StepVerifier.create(roleOrganizationRepository.existsByRoleIdAndOrganizationId(userRoleId, organizationId))
                .expectNext(true).verifyComplete();

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.delete().uri("/roles/"+userRoleId.toString())
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("update roleName: {}", entityExchangeResult.getResponseBody().get("message"));

        List<Map> list = getRolesOwnedByOrganization(organizationId);
        List<String> listOfNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        assertThat(listOfNames.contains("user")).isFalse();

        StepVerifier.create(roleUserRepository.existsByRoleIdAndUserId(userRoleId, creatorId))
                .expectNext(false).verifyComplete();
        StepVerifier.create(roleOrganizationRepository.existsByRoleIdAndOrganizationId(userRoleId, organizationId))
                .expectNext(false).verifyComplete();
    }

    @Test
    public void getById() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        LOG.info("create user role");
        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.get().uri("/roles/"+userRoleId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("retrieved role by id: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody().get("id")).isEqualTo(userRoleId.toString());
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
        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        UUID adminRoleId = createRoleByOrganizationId(creatorId, clientId, false,companyId1, "admin", HttpStatus.CREATED);
        UUID emplyeeRoleId = createRoleByOrganizationId(creatorId, clientId, false,companyId1, "employee", HttpStatus.CREATED);
        UUID managerRoleId = createRoleByOrganizationId(creatorId, clientId, false,companyId1, "manager", HttpStatus.CREATED);

        List<Map> roleList = getRolesOwnedByOrganization(companyId1);
        List<String> listOfNames = roleList.stream().map(map -> map.get("name").toString()).collect(toList());

        assertThat(listOfNames.contains("user")).isTrue();
        assertThat(listOfNames.contains("admin")).isTrue();
        assertThat(listOfNames.contains("employee")).isTrue();
        assertThat(listOfNames.contains("manager")).isTrue();
        assertThat(listOfNames.contains("person")).isFalse();

        UUID companyId2managerRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId2, "manager", HttpStatus.CREATED);
        UUID companyId2userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId2, "user", HttpStatus.CREATED);

        roleList = getRolesOwnedByOrganization(companyId2);
        listOfNames = roleList.stream().map(map -> map.get("name").toString()).collect(toList());
        assertThat(listOfNames.size()).isEqualTo(2);

        assertThat(listOfNames.contains("manager")).isTrue();
        assertThat(listOfNames.contains("user")).isTrue();

        UUID companyId3managerRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId3, "manager", HttpStatus.CREATED);

        roleList = getRolesOwnedByOrganization(companyId3);
        listOfNames = roleList.stream().map(map -> map.get("name").toString()).collect(toList());
        assertThat(listOfNames.size()).isEqualTo(1);

        assertThat(listOfNames.contains("manager")).isTrue();
    }
        public List<Map> getRolesOwnedByOrganization(UUID organizationId) {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

            LOG.info("get roles owned by organization");
        EntityExchangeResult<RestPage> entityExchangeResult = webTestClient.get().uri("/roles/organization/"+organizationId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(RestPage.class)
                .returnResult();

        LOG.info("roles found: {}", entityExchangeResult.getResponseBody().getContent());
        List<Map> list = entityExchangeResult.getResponseBody().getContent();

        return list;

    }

    public List<Map> getRolesOwnedByUser(UUID userId) {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        LOG.info("get roles owned by user");
        EntityExchangeResult<RestPage> entityExchangeResult = webTestClient.get().uri("/roles/user/"+userId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(RestPage.class)
                .returnResult();

        LOG.info("roles found: {}", entityExchangeResult.getResponseBody().getContent());
        List<Map> list = entityExchangeResult.getResponseBody().getContent();

        return list;

    }

    @Test
    public void roleClientUserAssociation() {
        UUID clientId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        UUID creatorId = UUID.randomUUID();
        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();
        UUID companyId3 = UUID.randomUUID();

        LOG.info("create user role");
        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        UUID adminRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "admin", HttpStatus.CREATED);

        List<Map> mapList = Arrays.asList(Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", userRoleId.toString()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId2.toString(),
                        "roleId", adminRoleId.toString()),
                Map.of("clientId", clientId.toString(),
                        "userId", userId3.toString(),
                        "roleId", userRoleId.toString()));

        Map<String, String> map1 = Map.of("clientId", clientId.toString(),
                "userId", userId1.toString(),
                "roleId", userRoleId.toString());

        Map<String, String> map2 = Map.of("clientId", clientId.toString(),
                        "userId", userId2.toString(),
                        "roleId", adminRoleId.toString());

        Map<String, String> map3 = Map.of("clientId", clientId.toString(),
                        "userId", userId3.toString(),
                        "roleId", userRoleId.toString());

        LOG.info("add users to role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        List<RoleClientUser> roleClientUsers = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        RoleClientUser roleClientUser1 = postAddRoleClientUser(jwt, map1, mapper);
        RoleClientUser roleClientUser2 = postAddRoleClientUser(jwt, map2, mapper);
        RoleClientUser roleClientUser3 = postAddRoleClientUser(jwt, map3, mapper);

        RoleClientUser roleClientUserUpdate = new RoleClientUser(roleClientUser1.getId(), roleClientUser1.getClientId(),
                roleClientUser1.getUserId(), adminRoleId);

        updateRoleClientUser(jwt, roleClientUserUpdate, mapper);
        roleClientUserUpdate = new RoleClientUser(roleClientUser2.getId(), roleClientUser2.getClientId(),
                roleClientUser2.getUserId(), userRoleId);

        updateRoleClientUser(jwt, roleClientUserUpdate, mapper);

        roleClientUserUpdate = new RoleClientUser(roleClientUser3.getId(), roleClientUser3.getClientId(),
                roleClientUser3.getUserId(), adminRoleId);

        updateRoleClientUser(jwt, roleClientUserUpdate, mapper);

        Map<UUID, RoleClientUser> map = getUserRoleClient(jwt, clientId,  mapper);
        assertThat(map.get(roleClientUser1.getId())).isNotNull();
        assertThat(map.get(roleClientUser1.getId()).getRoleId()).isNotNull();
        assertThat(map.get(roleClientUser1.getId()).getRoleId()).isEqualTo(adminRoleId);

        assertThat(map.get(roleClientUser2.getId())).isNotNull();
        assertThat(map.get(roleClientUser2.getId()).getRoleId()).isNotNull();
        assertThat(map.get(roleClientUser2.getId()).getRoleId()).isEqualTo(userRoleId);

        assertThat(map.get(roleClientUser3.getId())).isNotNull();
        assertThat(map.get(roleClientUser3.getId()).getRoleId()).isEqualTo(adminRoleId);


        deleteRoleClientUser(jwt, adminRoleId, userId3);
        map = getUserRoleClient(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(2);
        assertThat(map.get(roleClientUser3.getId())).isNull();
        assertThat(map.get(roleClientUser1.getId())).isNotNull();
        assertThat(map.get(roleClientUser2.getId())).isNotNull();

        deleteRoleClientUser(jwt, adminRoleId, userId1);
        map = getUserRoleClient(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get(roleClientUser3.getId())).isNull();
        assertThat(map.get(roleClientUser1.getId())).isNull();
        assertThat(map.get(roleClientUser2.getId())).isNotNull();

        LOG.info("deleteRoleClientUser with not associated userId 2 and admin roleId");
        deleteRoleClientUser(jwt, adminRoleId, userId2);
        map = getUserRoleClient(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get(roleClientUser3.getId())).isNull();
        assertThat(map.get(roleClientUser1.getId())).isNull();
        assertThat(map.get(roleClientUser2.getId())).isNotNull();

        deleteRoleClientUser(jwt, userRoleId, userId2);
        map = getUserRoleClient(jwt, clientId,  mapper);
        assertThat(map.size()).isEqualTo(0);
        assertThat(map.get(roleClientUser3.getId())).isNull();
        assertThat(map.get(roleClientUser1.getId())).isNull();
        assertThat(map.get(roleClientUser2.getId())).isNull();
    }

    //delete RoleClientUser by roleId and userId
    private void deleteRoleClientUser(final Jwt jwt, UUID adminRoleId, UUID userId) {
        webTestClient.delete()
                .uri("/roles/"+adminRoleId+"/users/"+userId).headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
    }

    private RoleClientUser postAddRoleClientUser(final Jwt jwt, Map<String, String> map, ObjectMapper mapper) {
        EntityExchangeResult<Map<String, Object>> entityExchangeResult = webTestClient.post().uri("/roles/user")
                .headers(addJwt(jwt)).bodyValue(map)
                .exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<Map<String, Object>>(){}).returnResult();
        LOG.info("response from adding user client roles: {}", entityExchangeResult.getResponseBody());
        assertThat(entityExchangeResult.getResponseBody().get("message")).isEqualTo("created new role client user row");

        RoleClientUser roleClientUser = mapper.convertValue(entityExchangeResult.getResponseBody().get("object"), RoleClientUser.class);

        assertThat(roleClientUser.getUserId()).isEqualTo(UUID.fromString(map.get("userId")));
        assertThat(roleClientUser.getClientId()).isEqualTo(map.get("clientId"));
        assertThat(roleClientUser.getRoleId()).isEqualTo(UUID.fromString(map.get("roleId")));
        LOG.info("assert role, clientId and userId matches from the map: {}", roleClientUser);

        return roleClientUser;
    }

    private void updateRoleClientUser(final Jwt jwt, RoleClientUser roleClientUser, ObjectMapper mapper) {
        LOG.info("update role client user");

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.put()
                .uri("/roles/user").headers(addJwt(jwt)).bodyValue(roleClientUser)
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        assertThat(entityExchangeResult.getResponseBody().get("message"))
                    .isEqualTo("updated role client user with id");
    }

    private Map<UUID, RoleClientUser> getUserRoleClient(final Jwt jwt, final UUID clientId, ObjectMapper mapper) {
        LOG.info("get role user client by page");
        EntityExchangeResult<RestPage> pageResult = webTestClient.get().uri("/roles/clientId/" + clientId + "/users")
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", pageResult.getResponseBody().getPageable());
        LOG.info("assert roles size for clientId: {}", pageResult.getResponseBody().getContent().size());
        //Assertions.assertThat(pageResult.getResponseBody().getContent().size()).isEqualTo(userIdRoleMap.size());

        Map<UUID, RoleClientUser> map = new HashMap<>();
        pageResult.getResponseBody().getContent().forEach(o -> {
            RoleClientUser roleClientUser = mapper.convertValue(o, RoleClientUser.class);
            LOG.info("roleClientUser from page: {}", roleClientUser);
            map.put(roleClientUser.getId(), roleClientUser);
        });

        return map;
    }

    private void modifyRoles(List<RoleClientUser> roleClientUsers, UUID clientId, UUID userId1, UUID userId2, UUID userId3, UUID adminRoleId, UUID userRoleId) {
        LOG.info("test with modified roles");

        List<RoleClientUser> updateRoleClientUsers = new ArrayList<>();
        Map<String, String> map = new HashMap<>();//Map.of(userId1.toString(), "admin", userId3.toString(), "user");

        for(RoleClientUser roleClientUser: roleClientUsers) {
            if (roleClientUser.getRoleId().equals(userRoleId)) {
                updateRoleClientUsers.add(new RoleClientUser(roleClientUser.getId(), roleClientUser.getClientId(), roleClientUser.getUserId(), adminRoleId));
                LOG.info("update role for roleClientUser from {} to {}", roleClientUser.getRoleId(), adminRoleId);

                map.put(roleClientUser.getUserId().toString(), adminRoleId.toString());
            }
            else if (roleClientUser.getRoleId().equals(adminRoleId)) {
                updateRoleClientUsers.add(new RoleClientUser(roleClientUser.getId(), roleClientUser.getClientId(), roleClientUser.getUserId(), userRoleId));
                LOG.info("update role for roleClientUser from {} to {}", roleClientUser.getRoleId(), userRoleId);
                map.put(roleClientUser.getUserId().toString(), userRoleId.toString());
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
        for (RoleClientUser roleClientUser: updateRoleClientUsers) {
            EntityExchangeResult<Map> entityExchangeResult = webTestClient.put()
                    .uri("/roles/user").headers(addJwt(jwt)).bodyValue(roleClientUser)
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

    private void assertRoles(Map<String, String> userIdRoleMap, Jwt jwt, UUID clientId) {

        LOG.info("get applications by id and all users in it, which should give 4 applicationUsers");
        EntityExchangeResult<RestPage> pageResult = webTestClient.get().uri("/roles/clientId/" + clientId + "/users")
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", pageResult.getResponseBody().getPageable());
        LOG.info("assert roles size for clientId: {}", pageResult.getResponseBody().getContent().size());
        Assertions.assertThat(pageResult.getResponseBody().getContent().size()).isEqualTo(userIdRoleMap.size());


        Set<String> keySet = userIdRoleMap.keySet();
        if (!keySet.isEmpty()) {
            LOG.info("map has keys");
            pageResult.getResponseBody().getContent().forEach(o -> {
                LinkedHashMap<String, String> linkedHashMap1 = (LinkedHashMap) o;

                LOG.info("linkedHashMap1: {}", linkedHashMap1);

                String userId = linkedHashMap1.get("userId").toString();

                if (keySet.contains(userId)) {
                    String roleName = linkedHashMap1.get("roleName");

                    Assertions.assertThat(roleName).isEqualTo(userIdRoleMap.get(userId));
                    LOG.info("matched page response userId: '{}' and role: '{}' with expected from " +
                            "map.role: '{}'", userId, roleName, userIdRoleMap.get(userId));
                } else {
                    fail("retrieved userId from page does not match");
                }
            });
        }
         else {
             LOG.info("map is empty");
        }
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
        UUID userRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        UUID adminRoleId = createRoleByOrganizationId(creatorId, clientId, false, companyId1, "admin", HttpStatus.CREATED);

        List<Map> mapList = Arrays.asList(Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", userRoleId.toString(),
                        "action", "add"),
                Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", adminRoleId.toString(),
                        "action", "add"),
                Map.of("clientId", clientId.toString(),
                        "userId", userId2.toString(),
                        "roleId", adminRoleId.toString(),
                        "action", "add"),
                Map.of("clientId", clientId.toString(),
                        "userId", userId3.toString(),
                        "roleId", userRoleId.toString(),
                        "action", "add"));

        LOG.info("add users to role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));


        mapList.forEach(map -> {
            EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/roles/user").headers(addJwt(jwt)).bodyValue(map)
                    .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
        });
        LOG.info("made role user associations");

        LOG.info("get role by clientId and userId");

        EntityExchangeResult<List> roleByClientIdAndUserIdResult = webTestClient.get()
                .uri("/roles/clientId/" + clientId + "/users/" + userId1).headers(addJwt(jwt))
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

@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
class RestPage<T> extends PageImpl<T> {
    private static final Logger LOG = LoggerFactory.getLogger(RestPage.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPage(@JsonProperty("content") List<T> content,
                    @JsonProperty("number") int page,
                    @JsonProperty("size") int size,
                    @JsonProperty("totalElements") long total,
                    @JsonProperty("numberOfElements") int numberOfElements,
                    @JsonProperty("pageNumber") int pageNumber
    ) {
        super(content, PageRequest.of(page, size), total);
    }

    public RestPage(Page<T> page) {
        super(page.getContent(), page.getPageable(), page.getTotalElements());
        LOG.info("page.content: {}", page.getContent());
    }


}

