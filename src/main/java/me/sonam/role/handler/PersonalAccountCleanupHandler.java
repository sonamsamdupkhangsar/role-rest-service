package me.sonam.role.handler;

import me.sonam.role.repo.AuthzManagerRoleAssignmentRepository;
import me.sonam.role.repo.ClientOrganizationUserRoleRepository;
import me.sonam.role.security.InternalServiceAuthorization;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Service
public class PersonalAccountCleanupHandler {
    private final AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository;
    private final ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository;
    private final InternalServiceAuthorization internalServiceAuthorization;

    public PersonalAccountCleanupHandler(
            AuthzManagerRoleAssignmentRepository authzManagerRoleAssignmentRepository,
            ClientOrganizationUserRoleRepository clientOrganizationUserRoleRepository,
            InternalServiceAuthorization internalServiceAuthorization) {
        this.authzManagerRoleAssignmentRepository = authzManagerRoleAssignmentRepository;
        this.clientOrganizationUserRoleRepository = clientOrganizationUserRoleRepository;
        this.internalServiceAuthorization = internalServiceAuthorization;
    }

    public Mono<ServerResponse> adminCount(ServerRequest request) {
        return internalServiceAuthorization.requireService()
                .then(Mono.defer(() ->
                        authzManagerRoleAssignmentRepository.countByUserId(userId(request))))
                .flatMap(count -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("count", count)));
    }

    public Mono<ServerResponse> deleteAssignments(ServerRequest request) {
        return internalServiceAuthorization.requireService()
                .then(Mono.defer(() ->
                        authzManagerRoleAssignmentRepository.countByUserId(userId(request))))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)))
                .flatMap(count -> {
                    if (count == 0) {
                        return clientOrganizationUserRoleRepository.deleteByUserId(userId(request))
                                .then();
                    }

                    return Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Transfer or remove your admin roles before deleting your account"));
                })
                .then(ServerResponse.noContent()
                        .build());
    }

    private UUID userId(ServerRequest request) {
        try {
            return UUID.fromString(request.pathVariable("userId"));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID", exception);
        }
    }
}
