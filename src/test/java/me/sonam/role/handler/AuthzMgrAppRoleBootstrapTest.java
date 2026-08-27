package me.sonam.role.handler;

import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthzMgrAppRoleBootstrapTest {

    private final AuthzManagerRoleRepository roleRepository = mock(AuthzManagerRoleRepository.class);
    private final AuthzManagerRoleAssignmentRepository assignmentRepository =
            mock(AuthzManagerRoleAssignmentRepository.class);
    private final AuthzMgrAppRole service = new AuthzMgrAppRole(roleRepository, assignmentRepository);

    @Test
    void createsBothAdministratorRolesWhenMissing() {
        when(roleRepository.countByName("OrgAdmin")).thenReturn(Mono.just(0L));
        when(roleRepository.countByName("SubdomainAdmin")).thenReturn(Mono.just(0L));
        doAnswer(invocation -> Mono.just(invocation.getArgument(0, AuthzManagerRole.class)))
                .when(roleRepository).save(any(AuthzManagerRole.class));

        StepVerifier.create(service.bootstrapAdminRoles()).verifyComplete();

        verify(roleRepository).save(argThat(role -> "OrgAdmin".equals(role.getName())));
        verify(roleRepository).save(argThat(role -> "SubdomainAdmin".equals(role.getName())));
    }

    @Test
    void preservesBothAdministratorRolesWhenTheyAlreadyExist() {
        AuthzManagerRole orgAdmin = new AuthzManagerRole(null, "OrgAdmin");
        AuthzManagerRole subdomainAdmin = new AuthzManagerRole(null, "SubdomainAdmin");
        when(roleRepository.countByName("OrgAdmin")).thenReturn(Mono.just(1L));
        when(roleRepository.countByName("SubdomainAdmin")).thenReturn(Mono.just(1L));
        when(roleRepository.findByName("OrgAdmin")).thenReturn(Mono.just(orgAdmin));
        when(roleRepository.findByName("SubdomainAdmin")).thenReturn(Mono.just(subdomainAdmin));

        StepVerifier.create(service.bootstrapAdminRoles()).verifyComplete();

        verify(roleRepository, never()).save(orgAdmin);
        verify(roleRepository, never()).save(subdomainAdmin);
    }
}
