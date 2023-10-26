package me.sonam.role.repo;

import me.sonam.role.repo.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {
    @Query("select r.* from Role r,  Role_Organization ro " +
            "where ro.organization_id = :id " +
            " and r.name = :name " +
            " and ro.role_id=r.id")
    Flux<Role> findByOrganizationIdAndName(@Param("id") UUID id, @Param("name") String name);

    @Query("select r.* from Role r,  Role_User ru " +
            "where ru.user_id = :userId " +
            " and r.name = :name " +
            " and ru.role_id=r.id")
    Flux<Role> findByUserIdAndName(@Param("userId") UUID userId, @Param("name") String name);


    @Query("select r.* from Role r,  Role_Organization ro " +
            "where ro.organization_id = :organizationId " +
            " and ro.role_id=r.id")
    Flux<Role> findByOrganizationId(@Param("organizationId") UUID organizationId, Pageable pageable);


    @Query("select r.* from Role r,  Role_User ru " +
            "where ru.user_id = :userId " +
            " and ru.role_id=r.id")
    Flux<Role> findByUserId(@Param("userId") UUID userId, Pageable pageable);


}
