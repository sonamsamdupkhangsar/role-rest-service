package me.sonam.role;

import me.sonam.role.handler.AdminRoleDeletionHandler;
import me.sonam.role.repo.*;
import me.sonam.role.repo.entity.*;
import me.sonam.role.security.InternalServiceAuthorization;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.*;

class AdminRoleDeletionTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final UUID orgAdminId = UUID.randomUUID();
    private final UUID subdomainAdminId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final AuthzManagerRoleRepository authzManagerRoleRepository = mock(AuthzManagerRoleRepository.class);
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository = mock(AuthzManagerRoleAssignmentRepository.class);
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository = mock(ClientOrganizationUserRoleRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final InternalServiceAuthorization internalServiceAuthorization = mock(InternalServiceAuthorization.class);

    AdminRoleDeletionTest() {
        when(internalServiceAuthorization.requireInternalServiceIdentity())
                .thenReturn(Mono.empty());
        when(authzManagerRoleRepository.findAll())
                .thenReturn(Flux.just(
                new AuthzManagerRole(orgAdminId, "OrgAdmin"), new AuthzManagerRole(subdomainAdminId, "SubdomainAdmin")));
        when(authzManagerRoleAssignmentRepository.findByUserId(userId))
                .thenReturn(Flux.just(assignment(userId)));
        when(authzManagerRoleAssignmentRepository.findByScopeTypeAndScopeId(AuthzManagerRoleAssignment.ORGANIZATION, organizationId))
                .thenReturn(Flux.just(assignment(userId)));
        when(clientOrganizationUserRoleRepository.findByUserId(userId))
                .thenReturn(Flux.empty());
        when(clientOrganizationUserRoleRepository.findByOrganizationId(organizationId))
                .thenReturn(Flux.empty());
        when(roleRepository.findByOrganizationId(organizationId))
                .thenReturn(Flux.just(new Role(roleId, "Application role", organizationId)));
        when(clientOrganizationUserRoleRepository.findByRoleId(roleId))
                .thenReturn(Flux.empty());
    }

    @Test
    void clientAssignmentCleanupPreservesRoleDefinitionsAndAdminAssignments() {
        UUID clientId = UUID.randomUUID();
        when(clientOrganizationUserRoleRepository.findByClientId(clientId))
                .thenReturn(Flux.just(new ClientOrganizationUserRole(null, roleId, clientId, organizationId, userId)));
        when(clientOrganizationUserRoleRepository.deleteByClientIdAndOrganizationIdAndUserId(clientId, organizationId, userId))
                .thenReturn(Mono.just(1L));

        client(true, "")
                .delete()
                .uri(path() + "/clients/" + clientId + "/assignments")
                .exchange()
                .expectStatus().isNoContent();

        verify(clientOrganizationUserRoleRepository).deleteByClientIdAndOrganizationIdAndUserId(clientId, organizationId, userId);
        verify(roleRepository, never()).deleteByOrganizationId(any());
        verify(authzManagerRoleAssignmentRepository, never()).deleteByScopeTypeAndScopeIdAndUserId(any(), any(), any());
    }

    @Test
    void clientAssignmentCleanupRejectsOtherUsersAndOrganizations() {
        UUID clientId = UUID.randomUUID();
        when(clientOrganizationUserRoleRepository.findByClientId(clientId))
                .thenReturn(Flux.just(new ClientOrganizationUserRole(null, roleId, clientId, organizationId, UUID.randomUUID())));

        client(true, "")
                .delete()
                .uri(path() + "/clients/" + clientId + "/assignments")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        when(clientOrganizationUserRoleRepository.findByClientId(clientId))
                .thenReturn(Flux.just(new ClientOrganizationUserRole(null, roleId, clientId, UUID.randomUUID(), userId)));

        client(true, "")
                .delete()
                .uri(path() + "/clients/" + clientId + "/assignments")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        verify(clientOrganizationUserRoleRepository, never()).deleteByClientIdAndOrganizationIdAndUserId(any(), any(), any());
    }

    @Test
    void organizationInventoryUsesDescriptiveValidationField() {
        client(true, "")
                .get()
                .uri(path())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.adminRoleAssignmentsValid")
                .isEqualTo(true)
                .jsonPath("$.complete")
                .doesNotExist();
    }

    @Test
    void cleanupDeletesOwnedRoleDataButNeverGlobalAdminDefinitions() {
        when(clientOrganizationUserRoleRepository.deleteByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Mono.just(1L));
        when(roleRepository.deleteByOrganizationId(organizationId))
                .thenReturn(Mono.just(1L));
        when(authzManagerRoleAssignmentRepository.deleteByScopeTypeAndScopeIdAndUserId(
                AuthzManagerRoleAssignment.ORGANIZATION, organizationId, userId))
                        .thenReturn(Mono.just(1L));

        client(true, "")
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isNoContent();

        verify(roleRepository).deleteByOrganizationId(organizationId);
        verify(authzManagerRoleRepository, never()).deleteById(any(UUID.class));
        verify(authzManagerRoleAssignmentRepository).deleteByScopeTypeAndScopeIdAndUserId(
                AuthzManagerRoleAssignment.ORGANIZATION, organizationId, userId);
    }

    @Test
    void unauthorizedCallerCannotInspectOrDelete() {
        when(internalServiceAuthorization.requireInternalServiceIdentity())
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
        clearInvocations(authzManagerRoleRepository, authzManagerRoleAssignmentRepository,
                clientOrganizationUserRoleRepository, roleRepository);

        WebTestClient webTestClient = client(true, "");
        webTestClient.get()
                .uri("/roles/internal/admin-deletion/users/" + userId)
                .exchange()
                .expectStatus()
                .isForbidden();
        webTestClient.delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isForbidden();

        verifyNoInteractions(authzManagerRoleRepository, authzManagerRoleAssignmentRepository,
                clientOrganizationUserRoleRepository, roleRepository);
    }

    @Test
    void subdomainAdministratorIsBlocked() {
        when(authzManagerRoleAssignmentRepository.findByUserId(userId))
                .thenReturn(Flux.just(
                assignment(userId), new AuthzManagerRoleAssignment(null, subdomainAdminId, userId,
                        AuthzManagerRoleAssignment.SUBDOMAIN, UUID.randomUUID())));

        client(true, "")
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);

        noDeletion();
    }

    @Test
    void anotherAdministratorWithoutMembershipIsBlocked() {
        when(authzManagerRoleAssignmentRepository.findByScopeTypeAndScopeId(AuthzManagerRoleAssignment.ORGANIZATION, organizationId))
                .thenReturn(Flux.just(assignment(userId), assignment(UUID.randomUUID())));

        client(true, "")
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);

        noDeletion();
    }

    @Test
    void roleUsedInAnotherOrganizationIsBlocked() {
        when(clientOrganizationUserRoleRepository.findByRoleId(roleId))
                .thenReturn(Flux.just(
                new ClientOrganizationUserRole(null, roleId, UUID.randomUUID(), UUID.randomUUID(), userId)));

        client(true, "")
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);

        noDeletion();
    }

    @Test
    void otherClientRoleUserIsBlocked() {
        when(clientOrganizationUserRoleRepository.findByOrganizationId(organizationId))
                .thenReturn(Flux.just(
                new ClientOrganizationUserRole(null, roleId, UUID.randomUUID(), organizationId, UUID.randomUUID())));

        client(true, "")
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);

        noDeletion();
    }

    @Test
    void unconfiguredOrProtectedOrganizationIsBlocked() {
        client(false, "UNCONFIGURED")
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);
        client(true, organizationId.toString())
                .delete()
                .uri(path())
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);

        noDeletion();
    }

    @Test
    void unknownAdministrativeAssignmentIsNotIgnored() {
        when(authzManagerRoleAssignmentRepository.findByUserId(userId))
                .thenReturn(Flux.just(
                new AuthzManagerRoleAssignment(null, UUID.randomUUID(), userId, "UNRECOGNIZED", organizationId)));

        client(true, "")
                .get()
                .uri("/roles/internal/admin-deletion/users/" + userId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.hasUnresolvedAdminResponsibilities")
                .isEqualTo(true)
                .jsonPath("$.roleInventoryLoaded")
                .isEqualTo(true)
                .jsonPath("$.complete")
                .doesNotExist();

        noDeletion();
    }

    private AuthzManagerRoleAssignment assignment(UUID assignedUserId) {
        return new AuthzManagerRoleAssignment(null, orgAdminId, assignedUserId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId);
    }

    private WebTestClient client(boolean configured, String protectedIds) {
        AdminRoleDeletionHandler adminRoleDeletionHandler = new AdminRoleDeletionHandler(
                authzManagerRoleRepository, authzManagerRoleAssignmentRepository, clientOrganizationUserRoleRepository,
                roleRepository, internalServiceAuthorization, configured, protectedIds);

        return WebTestClient.bindToRouterFunction(new Router().adminAccountDeletionRoutes(adminRoleDeletionHandler))
                .build();
    }

    private String path() {
        return "/roles/internal/admin-deletion/users/" + userId + "/organizations/" + organizationId;
    }

    private void noDeletion() {
        verify(roleRepository, never()).deleteByOrganizationId(any());
        verify(clientOrganizationUserRoleRepository, never()).deleteByOrganizationIdAndUserId(any(), any());
        verify(authzManagerRoleAssignmentRepository, never()).deleteByScopeTypeAndScopeIdAndUserId(any(), any(), any());
    }
}
