package me.sonam.role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.RoleUserRepository;
import me.sonam.role.repo.entity.RoleUser;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
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
    private RoleUserRepository roleUserRepository;

    private UUID createRole(UUID creatorId, UUID clientId, boolean shouldExistBefore, UUID organizationId, String roleName, HttpStatus httpStatus) {
        LOG.info("create role {}", roleName);
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        //UUID clientId = UUID.randomUUID();
       // UUID userId = UUID.randomUUID();

        List<Map> list = getPage(organizationId);
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


        list  = getPage(organizationId);

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
    public void create() {
        LOG.info("create application");

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        UUID userRoleId = createRole(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        LOG.info("now create the same role in organizationId");
        UUID userRoleId2 = createRole(creatorId, clientId, true, organizationId, "user", HttpStatus.BAD_REQUEST);
    }

    @AfterEach
    public void deleteAllRoles() {
        roleRepository.deleteAll().subscribe();
        roleUserRepository.deleteAll().subscribe();
    }
    @Test
    public void update() {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID userRoleId = createRole(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);


        LOG.debug("build a map with admin name for role update");
        var mapBody = Map.of("id", userRoleId.toString(), "organizationId", organizationId.toString(), "name", "admin");

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.put().uri("/roles")
                .headers(addJwt(jwt)).bodyValue(mapBody).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("update roleName: {}", entityExchangeResult.getResponseBody().get("message"));

        List<Map> list = getPage(organizationId);
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

        LOG.info("create user role");
        UUID userRoleId = createRole(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.delete().uri("/roles/"+userRoleId.toString())
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("update roleName: {}", entityExchangeResult.getResponseBody().get("message"));

        List<Map> list = getPage(organizationId);
        List<String> listOfNames = list.stream().map(map -> map.get("name").toString()).collect(toList());

        assertThat(listOfNames.contains("user")).isFalse();
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
        UUID userRoleId = createRole(creatorId, clientId, false, organizationId, "user", HttpStatus.CREATED);

        EntityExchangeResult<Map> entityExchangeResult = webTestClient.get().uri("/roles/"+userRoleId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(Map.class)
                .returnResult();
        LOG.info("retrieved role by id: {}", entityExchangeResult.getResponseBody());

        assertThat(entityExchangeResult.getResponseBody().get("id")).isEqualTo(userRoleId.toString());
        assertThat(entityExchangeResult.getResponseBody().get("organizationId")).isNotNull();
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
        UUID userRoleId = createRole(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        UUID adminRoleId = createRole(creatorId, clientId, false,companyId1, "admin", HttpStatus.CREATED);
        UUID emplyeeRoleId = createRole(creatorId, clientId, false,companyId1, "employee", HttpStatus.CREATED);
        UUID managerRoleId = createRole(creatorId, clientId, false,companyId1, "manager", HttpStatus.CREATED);

        List<Map> roleList = getPage(companyId1);
        List<String> listOfNames = roleList.stream().map(map -> map.get("name").toString()).collect(toList());

        assertThat(listOfNames.contains("user")).isTrue();
        assertThat(listOfNames.contains("admin")).isTrue();
        assertThat(listOfNames.contains("employee")).isTrue();
        assertThat(listOfNames.contains("manager")).isTrue();
        assertThat(listOfNames.contains("person")).isFalse();

        UUID companyId2managerRoleId = createRole(creatorId, clientId, false, companyId2, "manager", HttpStatus.CREATED);
        UUID companyId2userRoleId = createRole(creatorId, clientId, false, companyId2, "user", HttpStatus.CREATED);

        roleList = getPage(companyId2);
        listOfNames = roleList.stream().map(map -> map.get("name").toString()).collect(toList());
        assertThat(listOfNames.size()).isEqualTo(2);

        assertThat(listOfNames.contains("manager")).isTrue();
        assertThat(listOfNames.contains("user")).isTrue();

        UUID companyId3managerRoleId = createRole(creatorId, clientId, false, companyId3, "manager", HttpStatus.CREATED);

        roleList = getPage(companyId3);
        listOfNames = roleList.stream().map(map -> map.get("name").toString()).collect(toList());
        assertThat(listOfNames.size()).isEqualTo(1);

        assertThat(listOfNames.contains("manager")).isTrue();
    }
        public List<Map> getPage(UUID organizationId) {
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        EntityExchangeResult<RestPage> entityExchangeResult = webTestClient.get().uri("/roles/organization/"+organizationId)
                .headers(addJwt(jwt)).exchange().expectStatus().isOk().expectBody(RestPage.class)
                .returnResult();

        LOG.info("roles found: {}", entityExchangeResult.getResponseBody().getContent());
        List<Map> list = entityExchangeResult.getResponseBody().getContent();

        return list;

    }

    @Test
    public void roleUserAssociation() {
        UUID clientId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        UUID creatorId = UUID.randomUUID();
        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();
        UUID companyId3 = UUID.randomUUID();

        LOG.info("create user role");
        UUID userRoleId = createRole(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        UUID adminRoleId = createRole(creatorId, clientId, false, companyId1, "admin", HttpStatus.CREATED);

        List<Map> mapList = Arrays.asList(Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", userRoleId.toString(),
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
                    EntityExchangeResult<String> entityExchangeResult = webTestClient.post().uri("/roles/user")
                            .headers(addJwt(jwt)).bodyValue(map)
                            .exchange().expectStatus().isOk().expectBody(String.class).returnResult();
                    LOG.info("result: {}", entityExchangeResult.getResponseBody());
                });

        Map<String, String> map = Map.of(userId1.toString(), "user", userId2.toString(), "admin", userId3.toString(), "user");
        LOG.info("assert the roles with the userId");
        assertRoles(map, jwt, clientId);

        modifyRoles(clientId, userId1, userId2, userId3, adminRoleId, userRoleId);
    }
    private void modifyRoles(UUID clientId, UUID userId1, UUID userId2, UUID userId3, UUID adminRoleId, UUID userRoleId) {
        LOG.info("test with modified roles");

        Map<String, String> map1 = Map.of("clientId", clientId.toString(),
                "userId", userId1.toString(),
                "roleId", adminRoleId.toString());

        var map2 = Map.of("clientId", clientId.toString(),
                "userId", userId3.toString(),
                "roleId", adminRoleId.toString());

        var map3 = Map.of("clientId", clientId.toString(),
                "userId", userId3.toString(),
                "roleId", userRoleId.toString());

        LOG.info("add users to role");
        final String authenticationId = "sonam";
        Jwt jwt = jwt(authenticationId);
        when(this.jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        LOG.info("update role user");
        EntityExchangeResult<Map> entityExchangeResult = webTestClient.put()
                .uri("/roles/user").headers(addJwt(jwt)).bodyValue(map1)
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        webTestClient.delete()
                .uri("/roles/"+adminRoleId+"/users/"+userId2).headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(Map.class).returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());

        LOG.info("should get a bad request when attempting to associate another role for userId3 and clientId to a new role");
        entityExchangeResult = webTestClient.post()
                .uri("/roles/user").headers(addJwt(jwt)).bodyValue(map2)
                .exchange().expectStatus().isBadRequest().expectBody(Map.class).returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        assertThat(entityExchangeResult.getResponseBody().get("error").toString())
                .isEqualTo("User already has a role associated with clientId, delete existing one or update it.");

        LOG.info("should get a bad request when attempting to reassociate role for userId3 and clientId to a new role");
        entityExchangeResult = webTestClient.post()
                .uri("/roles/user").headers(addJwt(jwt)).bodyValue(map3)
                .exchange().expectStatus().isBadRequest().expectBody(Map.class).returnResult();
        LOG.info("result: {}", entityExchangeResult.getResponseBody());
        assertThat(entityExchangeResult.getResponseBody().get("error").toString())
                .isEqualTo("User already has a role associated with clientId, delete existing one or update it.");


        Map<String, String> map = Map.of(userId1.toString(), "admin", userId3.toString(), "user");
        LOG.info("assert the roles with the userId");
        assertRoles(map, jwt, clientId);
    }

    private void assertRoles(Map<String, String> userIdRoleMap, Jwt jwt, UUID clientId) {

        LOG.info("get applications by id and all users in it, which should give 4 applicationUsers");
        EntityExchangeResult<RestPage> pageResult = webTestClient.get().uri("/roles/clientId/" + clientId + "/users")
                .headers(addJwt(jwt))
                .exchange().expectStatus().isOk().expectBody(RestPage.class).returnResult();

        LOG.info("pageResult pageable {}", pageResult.getResponseBody().getPageable());
        LOG.info("assert that only roleUser exists");
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
        UUID userRoleId = createRole(creatorId, clientId, false, companyId1, "user", HttpStatus.CREATED);
        UUID adminRoleId = createRole(creatorId, clientId, false, companyId1, "admin", HttpStatus.CREATED);

        List<Map> mapList = Arrays.asList(Map.of("clientId", clientId.toString(),
                        "userId", userId1.toString(),
                        "roleId", userRoleId.toString(),
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
        LinkedHashMap<String, String> linkedHashMap = list.get(0);

        LOG.info("role: {}", linkedHashMap);
        LOG.info("got roleUser for clientId and userId: {}", linkedHashMap);
        assertThat(linkedHashMap.get("roleId")).isNotNull();
        assertThat(linkedHashMap.get("roleName")).isEqualTo("user");
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

