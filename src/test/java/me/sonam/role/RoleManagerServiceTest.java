package me.sonam.role;

import me.sonam.role.config.RoleLimitProperties;
import me.sonam.role.handler.RoleException;
import me.sonam.role.handler.service.RoleManagerService;
import me.sonam.role.repo.RoleRepository;
import me.sonam.role.repo.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleManagerServiceTest {

    @Test
    void createRoleFailsWhenDemoIssuerReachesLimit() {
        UUID organizationId = UUID.randomUUID();
        Role role = new Role(null, "third role", organizationId);
        RoleRepository roleRepository = mock(RoleRepository.class);
        RoleLimitProperties roleLimitProperties = new RoleLimitProperties();
        roleLimitProperties.setDefaultMaxRoles(5);
        roleLimitProperties.getHosts().put("demo.openissuer.com", 2);

        when(roleRepository.countByOrganizationId(organizationId)).thenReturn(Mono.just(2L));

        RoleManagerService service = new RoleManagerService();
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "roleLimitProperties", roleLimitProperties);

        StepVerifier.create(service.createRole(role)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuthentication(
                                "sonam", "https://demo.openissuer.com"))))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(RoleException.class);
                    assertThat(throwable.getMessage()).isEqualTo("Max number of roles reached");
                })
                .verify();

        verify(roleRepository, never()).save(role);
    }

    @Test
    void createRoleUsesDefaultLimitWhenIssuerHasNoOverride() {
        UUID organizationId = UUID.randomUUID();
        Role role = new Role(null, "business role", organizationId);
        RoleRepository roleRepository = mock(RoleRepository.class);
        RoleLimitProperties roleLimitProperties = new RoleLimitProperties();
        roleLimitProperties.setDefaultMaxRoles(5);
        roleLimitProperties.getHosts().put("demo.openissuer.com", 2);

        when(roleRepository.countByOrganizationId(organizationId)).thenReturn(Mono.just(2L));
        when(roleRepository.save(org.mockito.ArgumentMatchers.any(Role.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));

        RoleManagerService service = new RoleManagerService();
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "roleLimitProperties", roleLimitProperties);

        StepVerifier.create(service.createRole(role)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(jwtAuthentication(
                                "sonam", "https://business1.openissuer.com"))))
                .assertNext(savedRole -> {
                    assertThat(savedRole.getName()).isEqualTo("business role");
                    assertThat(savedRole.getOrganizationId()).isEqualTo(organizationId);
                })
                .verifyComplete();
    }

    private Authentication jwtAuthentication(String subjectName, String issuer) {
        Jwt jwt = new Jwt("token", null, null,
                Map.of("alg", "none"), Map.of("sub", subjectName, "iss", issuer));
        return new JwtAuthenticationToken(jwt);
    }
}
