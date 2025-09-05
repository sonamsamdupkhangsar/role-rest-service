package me.sonam.role;

import me.sonam.role.repo.entity.AuthzManagerRole;
import me.sonam.role.repo.entity.AuthzManagerRoleOrganization;
import me.sonam.role.repo.entity.AuthzManagerRoleUser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AuthzManagerRoleTest {
    private static final Logger LOG = LoggerFactory.getLogger(AuthzManagerRoleTest.class);

    @Test
    public void create() {
        LOG.info("when a user is created and logs in to create a any entity");
        UUID orgId = UUID.randomUUID();
        //create this in a init method
        AuthzManagerRole authzManagerRole = new AuthzManagerRole(null, "SuperAdmin");

        assertThat(authzManagerRole.getId()).isNotNull();
        assertThat(authzManagerRole.getName()).isEqualTo("SuperAdmin");

        //When a user logs-in to the AuthzManager create this entity
        UUID userId = UUID.randomUUID();
        AuthzManagerRoleUser authzManagerRoleUser = new AuthzManagerRoleUser(null,authzManagerRole.getId(),  userId);
        assertThat(authzManagerRoleUser.getId()).isNotNull();
        assertThat(authzManagerRoleUser.getAuthzManagerRoleId()).isEqualTo(authzManagerRole.getId());
        assertThat(authzManagerRoleUser.getUserId()).isEqualTo(userId);

        //when the user creates any entity such as client, role, organization, assign the authzManagerRoleUser.Id and create this entity
        AuthzManagerRoleOrganization authzManagerRoleOrganization  = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), orgId, userId);
        assertThat(authzManagerRoleOrganization.getId()).isNotNull();
        assertThat(authzManagerRoleOrganization.getAuthzManagerRoleId()).isEqualTo(authzManagerRole.getId());
        assertThat(authzManagerRoleOrganization.getOrganizationId()).isEqualTo(orgId);
        assertThat(authzManagerRoleOrganization.getUserId()).isEqualTo(userId);

        //when user creates a client assign the authzManagerRoleUser.id as the owner
        //new Client(ownerId=authzManagerRoleUser.id)

        UUID user2Id = UUID.randomUUID();
        //when the current SuperAdmin makes another user a SuperAdmin
        var authzManagerRoleOrganization2  = new AuthzManagerRoleOrganization(null, authzManagerRole.getId(), orgId, user2Id);
        assertThat(authzManagerRoleOrganization2.getId()).isNotNull();
        assertThat(authzManagerRoleOrganization2.getAuthzManagerRoleId()).isEqualTo(authzManagerRole.getId());
        assertThat(authzManagerRoleOrganization2.getOrganizationId()).isEqualTo(orgId);
        assertThat(authzManagerRoleOrganization2.getUserId()).isEqualTo(user2Id);


        //when looking up clients by owner get by authzmanagerRoleUserId, put this attribute in the token

        //when transferring SuperAdmin to another user
        //delete existing authzManagerRoleOrganizaiton entity assoicated with this userId by orgId

    }
}
