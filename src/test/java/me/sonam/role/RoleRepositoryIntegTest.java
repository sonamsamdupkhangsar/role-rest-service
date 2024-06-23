package me.sonam.role;

import me.sonam.role.repo.RoleOrganizationRepository;
import me.sonam.role.repo.RoleRepository;

import me.sonam.role.repo.entity.Role;
import me.sonam.role.repo.entity.RoleOrganization;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;

import java.util.UUID;

@SpringBootTest
public class RoleRepositoryIntegTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoleRepositoryIntegTest.class);

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RoleOrganizationRepository roleOrganizationRepository;

    @Test
    public void query() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        for (int i = 0; i < 100; i++) {
            Role role = new Role(null, "hello "+ i, userId);

            roleRepository.save(role).doOnNext(role1 -> {
               // LOG.info("save in roleOrganization repo");
                roleOrganizationRepository.save(new RoleOrganization(null, role1.getId(), organizationId)).subscribe();
            }).subscribe();
        }

        Pageable pageable = PageRequest.of(0, 50);
        roleRepository.queryMe(organizationId, pageable)
                .collectList().subscribe(roles -> LOG.info("crossjoin found count {} roles: {}", roles.stream().count(), roles));

        pageable = PageRequest.of(1, 50);
        roleRepository.queryMe(organizationId, pageable).collectList()
                .subscribe(roles -> LOG.info("crossjoin found count {} roles: {}", roles.stream().count(), roles));

        pageable = PageRequest.of(2, 50);
        roleRepository.queryMe(organizationId, pageable).collectList()
                .subscribe(roles -> LOG.info("crossjoin found count {} roles: {}", roles.stream().count(), roles));

        pageable = PageRequest.of(0, 25);
        Flux<RoleOrganization> roleOrganizationFlux = roleOrganizationRepository.findByOrganizationId(organizationId,
                pageable);

        roleOrganizationFlux.collectList().subscribe(roleOrganizations ->
                LOG.info("0 page count roleOr: {}, roleOrgs: {}", roleOrganizations.stream().count(), roleOrganizations));

        pageable = PageRequest.of(1, 25);
         roleOrganizationFlux = roleOrganizationRepository.findByOrganizationId(organizationId,
                pageable);

        roleOrganizationFlux.collectList().subscribe(roleOrganizations ->
                LOG.info("1 page count roleOr: {}, roleOrgs: {}", roleOrganizations.stream().count(), roleOrganizations));

        pageable = PageRequest.of(2, 25);
        roleOrganizationFlux = roleOrganizationRepository.findByOrganizationId(organizationId,
                pageable);

        roleOrganizationFlux.collectList().subscribe(roleOrganizations ->
                LOG.info("2 page count roleOr: {}, roleOrgs: {}", roleOrganizations.stream().count(), roleOrganizations));

        pageable = PageRequest.of(3, 25);
        roleOrganizationFlux = roleOrganizationRepository.findByOrganizationId(organizationId,
                pageable);

        roleOrganizationFlux.collectList().subscribe(roleOrganizations ->
                LOG.info("3 page count roleOr: {}, roleOrgs: {}", roleOrganizations.stream().count(), roleOrganizations));

        pageable = PageRequest.of(4, 25);
        roleOrganizationFlux = roleOrganizationRepository.findByOrganizationId(organizationId,
                pageable);

        roleOrganizationFlux.collectList().subscribe(roleOrganizations ->
                LOG.info("4 page count roleOr: {}, roleOrgs: {}", roleOrganizations.stream().count(), roleOrganizations));

        roleOrganizationRepository.count().subscribe(aLong -> LOG.info("there are {} rows of RoleOrganization", aLong));
    }

    @Test
    public void t1e() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        for (int i = 0; i < 100; i++) {
            Role role = new Role(null, "hello "+ i, userId);

            roleRepository.save(role).doOnNext(role1 -> {
                // LOG.info("save in roleOrganization repo");
                roleOrganizationRepository.save(new RoleOrganization(null, role1.getId(), organizationId)).subscribe();
            }).subscribe();
        }
        final Pageable pageable = PageRequest.of(0, 50);

        roleOrganizationRepository.findByOrganizationId(organizationId, pageable).collectList().subscribe(
                roleOrganizations -> LOG.info("in page {} found {} roleOrganizations with orgId: {}",
                        pageable.getPageNumber(), roleOrganizations.stream().count(), organizationId));

        final Pageable pageable2 = PageRequest.of(1, 50);

        roleOrganizationRepository.findByOrganizationId(organizationId, pageable2).collectList().subscribe(
                roleOrganizations -> LOG.info("in page {} found {} roleOrganizations with orgId: {}",
                        pageable2.getPageNumber(), roleOrganizations.stream().count(), organizationId));

        final Pageable pageable3 = PageRequest.of(2, 50);

        roleOrganizationRepository.findByOrganizationId(organizationId, pageable3).collectList().subscribe(
                roleOrganizations -> LOG.info("in page {} found {} roleOrganizations with orgId: {}",
                        pageable3.getPageNumber(), roleOrganizations.stream().count(), organizationId));

        roleOrganizationRepository.countByOrganizationId(organizationId).subscribe(aLong ->
                LOG.info("there are {} rows of organizationId: {}", aLong, organizationId));

        final Pageable page1 = PageRequest.of(0, 50);
        roleOrganizationRepository.findByOrganizationId(organizationId, page1)
                .flatMap(roleOrganization -> roleRepository.findById(roleOrganization.getRoleId()))
        .subscribe(role -> LOG.info("page 0 found role: {}", role));

        final Pageable page2 = PageRequest.of(1, 50);
        roleOrganizationRepository.findByOrganizationId(organizationId, page2)
                .flatMap(roleOrganization -> roleRepository.findById(roleOrganization.getRoleId()))
                .subscribe(role -> LOG.info("page 1 found role: {}", role));

        final Pageable page3 = PageRequest.of(2, 50);
        roleOrganizationRepository.findByOrganizationId(organizationId, page3)
                .flatMap(roleOrganization -> roleRepository.findById(roleOrganization.getRoleId()))
                .subscribe(role -> LOG.info("page 2 found role: {}", role));
    }



}
