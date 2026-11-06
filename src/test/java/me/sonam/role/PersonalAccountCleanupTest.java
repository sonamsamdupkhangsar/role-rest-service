package me.sonam.role;

import me.sonam.role.handler.PersonalAccountCleanupHandler;
import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.security.InternalServiceAuthorization;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.*;

class PersonalAccountCleanupTest {
    private final UUID userId = UUID.randomUUID();
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository = mock(AuthzManagerRoleAssignmentRepository.class);
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository = mock(ClientOrganizationUserRoleRepository.class);
    private final InternalServiceAuthorization internalServiceAuthorization = mock(InternalServiceAuthorization.class);
    private final PersonalAccountCleanupHandler personalAccountCleanupHandler = new PersonalAccountCleanupHandler(
            authzManagerRoleAssignmentRepository,
            clientOrganizationUserRoleRepository,
            internalServiceAuthorization);
    private final WebTestClient webTestClient = WebTestClient
            .bindToRouterFunction(new Router().accountDeletionRoutes(personalAccountCleanupHandler))
            .build();

    @Test
    void deletesOnlyPersonalAssignmentsWhenNoAdminRolesExist() {
        when(internalServiceAuthorization.requireService())
                .thenReturn(Mono.empty());
        when(authzManagerRoleAssignmentRepository.countByUserId(userId))
                .thenReturn(Mono.just(0L));
        when(clientOrganizationUserRoleRepository.deleteByUserId(userId))
                .thenReturn(Mono.just(3L));

        webTestClient.delete()
                .uri("/roles/internal/accounts/{userId}/assignments", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        verify(clientOrganizationUserRoleRepository).deleteByUserId(userId);
        verify(authzManagerRoleAssignmentRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void adminCountIsReadOnly() {
        when(internalServiceAuthorization.requireService())
                .thenReturn(Mono.empty());
        when(authzManagerRoleAssignmentRepository.countByUserId(userId))
                .thenReturn(Mono.just(2L));

        webTestClient.get()
                .uri("/roles/internal/accounts/{userId}/admin-count", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);

        verifyNoInteractions(clientOrganizationUserRoleRepository);
    }

    @Test
    void adminCannotDeleteAssignments() {
        when(internalServiceAuthorization.requireService())
                .thenReturn(Mono.empty());
        when(authzManagerRoleAssignmentRepository.countByUserId(userId))
                .thenReturn(Mono.just(1L));

        webTestClient.delete()
                .uri("/roles/internal/accounts/{userId}/assignments", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(clientOrganizationUserRoleRepository);
    }

    @Test
    void unauthorizedCallerCannotReadOrMutateTargetUser() {
        when(internalServiceAuthorization.requireService())
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));

        webTestClient.get()
                .uri("/roles/internal/accounts/{userId}/admin-count", userId)
                .exchange()
                .expectStatus().isForbidden();

        webTestClient.delete()
                .uri("/roles/internal/accounts/{userId}/assignments", userId)
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(authzManagerRoleAssignmentRepository, clientOrganizationUserRoleRepository);
    }

    @Test
    void missingAdminCountFailsClosed() {
        when(internalServiceAuthorization.requireService())
                .thenReturn(Mono.empty());
        when(authzManagerRoleAssignmentRepository.countByUserId(userId))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/roles/internal/accounts/{userId}/assignments", userId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verifyNoInteractions(clientOrganizationUserRoleRepository);
    }

    @Test
    void malformedUserIdReturnsBadRequestWithoutRepositoryAccess() {
        when(internalServiceAuthorization.requireService())
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/roles/internal/accounts/not-a-uuid/assignments")
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(authzManagerRoleAssignmentRepository, clientOrganizationUserRoleRepository);
    }
}
