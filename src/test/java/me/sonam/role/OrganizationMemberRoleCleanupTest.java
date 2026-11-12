package me.sonam.role;

import java.util.UUID;
import me.sonam.role.handler.OrganizationMemberRoleCleanupHandler;
import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.AuthzManagerRoleRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrganizationMemberRoleCleanupTest {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final AuthzManagerRoleRepository authzManagerRoleRepository = mock(AuthzManagerRoleRepository.class);
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository = mock(AuthzManagerRoleAssignmentRepository.class);
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository = mock(ClientOrganizationUserRoleRepository.class);
    private final OrganizationMemberRoleCleanupHandler organizationMemberRoleCleanupHandler =
            new OrganizationMemberRoleCleanupHandler(authzManagerRoleRepository,
                    authzManagerRoleAssignmentRepository, clientOrganizationUserRoleRepository);

    @Test
    void anonymousCallerCannotDeleteAssignments() {
        StepVerifier.create(organizationMemberRoleCleanupHandler.removeMemberClientRoles(request()))
                .expectError(org.springframework.web.server.ResponseStatusException.class)
                .verify();

        verifyNoInteractions(clientOrganizationUserRoleRepository);
    }

    @Test
    void organizationAdminDeletesOnlyTargetMembersAssignments() {
        stubAdmin(true);
        when(clientOrganizationUserRoleRepository.deleteByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Mono.just(2L));

        StepVerifier.create(organizationMemberRoleCleanupHandler.removeMemberClientRoles(request())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .assertNext(response -> assertThat(response.statusCode().value()).isEqualTo(204))
                .verifyComplete();

        verify(clientOrganizationUserRoleRepository).deleteByOrganizationIdAndUserId(organizationId, userId);
        verifyNoMoreInteractions(clientOrganizationUserRoleRepository);
    }

    @Test
    void nonAdminCannotDeleteAssignments() {
        stubAdmin(false);

        StepVerifier.create(organizationMemberRoleCleanupHandler.removeMemberClientRoles(request())
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .expectError(org.springframework.web.server.ResponseStatusException.class)
                .verify();

        verifyNoInteractions(clientOrganizationUserRoleRepository);
    }

    private void stubAdmin(boolean allowed) {
        when(authzManagerRoleRepository.findByName("OrgAdmin"))
                .thenReturn(Mono.just(new AuthzManagerRole(roleId, "OrgAdmin")));
        when(authzManagerRoleAssignmentRepository.existsByAuthzManagerRoleIdAndUserIdAndScopeTypeAndScopeId(
                roleId, callerId, AuthzManagerRoleAssignment.ORGANIZATION, organizationId))
                .thenReturn(Mono.just(allowed));
    }

    private MockServerRequest request() {
        return MockServerRequest.builder()
                .pathVariable("organizationId", organizationId.toString())
                .pathVariable("userId", userId.toString())
                .build();
    }

    private JwtAuthenticationToken authentication() {
        return new JwtAuthenticationToken(Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("admin")
                .claim("userId", callerId.toString())
                .build());
    }
}
