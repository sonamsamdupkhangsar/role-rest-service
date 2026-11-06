package me.sonam.role;

import me.sonam.role.handler.service.RoleManagerService;
import me.sonam.role.repo.*;
import me.sonam.role.repo.entity.AuthzManagerRoleAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SharedOrganizationCleanupTest {
    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository = mock(ClientOrganizationUserRoleRepository.class);
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository = mock(AuthzManagerRoleAssignmentRepository.class);
    private final RoleManagerService roleManagerService = new RoleManagerService();

    SharedOrganizationCleanupTest() {
        ReflectionTestUtils.setField(roleManagerService, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(roleManagerService, "clientOrganizationUserRoleRepository", clientOrganizationUserRoleRepository);
        ReflectionTestUtils.setField(roleManagerService, "authzManagerRoleAssignmentRepository", authzManagerRoleAssignmentRepository);
        when(clientOrganizationUserRoleRepository.deleteByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Mono.just(1L));
        when(clientOrganizationUserRoleRepository.countByOrganizationId(orgId))
                .thenReturn(Mono.just(0L));
        when(authzManagerRoleAssignmentRepository.deleteByScopeTypeAndScopeIdAndUserId(AuthzManagerRoleAssignment.ORGANIZATION, orgId, userId))
                .thenReturn(Mono.just(1L));
    }

    @Test
    void sharedOrganizationKeepsRoleDefinitionsEvenWhenNoClientAssignmentsRemain() {
        assertThat(roleManagerService.deleteUserRoleData(orgId, userId, true)
                .block())
                .contains("success");
        verifyNoInteractions(roleRepository);
        verify(authzManagerRoleAssignmentRepository)
                .deleteByScopeTypeAndScopeIdAndUserId(AuthzManagerRoleAssignment.ORGANIZATION, orgId, userId);
    }

    @Test
    void organizationBeingDeletedRemovesUnassignedRoleDefinitions() {
        when(roleRepository.deleteByOrganizationId(orgId))
                .thenReturn(Mono.empty());
        assertThat(roleManagerService.deleteUserRoleData(orgId, userId, false)
                .block())
                .contains("success");
        verify(roleRepository)
                .deleteByOrganizationId(orgId);
    }

    @Test
    void defaultCleanupPreservesOrganizationDefinitions() {
        roleManagerService.deleteUserRoleData(orgId, userId)
                .block();
        verifyNoInteractions(roleRepository);
    }
}
