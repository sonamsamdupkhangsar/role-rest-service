package me.sonam.role;

import me.sonam.role.repo.RoleOrganizationRepository;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleOrganization;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Flux;

@AutoConfigureWebTestClient
@EnableAutoConfiguration
@SpringBootTest( classes = SpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepoTest {
    private static final Logger LOG = LoggerFactory.getLogger(RepoTest.class);

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleOrganizationRepository roleOrganizationRepository;

    @Autowired
    private DatabaseClient client;
    @Test
    public void repo() {
        LOG.info("save role and roleOrganization");

        var role = new Role(null, "user", UUID.randomUUID());
        roleRepository.save(role).subscribe(role1 -> LOG.info("saved role"));
        UUID orgId = UUID.fromString("db00af55-9a86-419e-9f35-5142eecb2d3d");//UUID.randomUUID();
        var roleOrganization = new RoleOrganization(null, role.getId(), orgId);
        roleOrganizationRepository.save(roleOrganization).subscribe(roleOrganization1 -> LOG.info("saved roleOrganization"));

        LOG.info("find by orgId {} and name {}", orgId, "user");
        roleRepository.findByOrganizationIdAndName(orgId, "user").subscribe(role1 -> LOG.info("found findByOrganizationIdAndName role: {}", role1));

       // databaseClientCheck();
    }

    private void databaseClientCheck() {
        /*
        select r.* from Role r,  Role_Organization ro " +

        "where ro.organization_id ='db00af55-9a86-419e-9f35-5142eecb2d3d'  " +
                " and r.name ='user' " +
                " and ro.role_id=r.id"
        */
        final String query = "select r.* from Role r, Role_Organization ro where " +
                "  ro.organization_id ='db00af55-9a86-419e-9f35-5142eecb2d3d'  " +
                " and r.name ='user'" +
                " and ro.role_id=r.id";

        LOG.info("query with databaseClient: {}", query);

        Flux<String> names = client.sql(query).map(row ->row.get("name", String.class)).all();
        names.subscribe(name-> LOG.info("found db.query name: {}", name));

        roleRepository.findAll().subscribe(role -> LOG.info("found repository role: {}", role));
    }

}
