package me.sonam.role.repo;

import me.sonam.role.repo.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
@Repository
public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {


    Flux<Role> findByUserId(UUID userId, Pageable pageable);
    Mono<Boolean> existsByNameAndUserId(String name, UUID userId);
    Mono<Role> findByNameAndUserId(String name, UUID userId);

    Mono<Long> countByUserId(UUID userId);
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
   /* @Query("select r.* from Role r INNER JOIN Role_Organization ro" +
            " on ro.role_id=r.id where ro.organization_id = :organizationId")*/
    Flux<Role> findByOrganizationId(@Param("organizationId") UUID organizationId, Pageable pageable);
    @Query("select r.* from Role r,  Role_Organization ro " +
            "where ro.organization_id = :organizationId " +
            " and ro.role_id=r.id")
   /* @Query("select r.* from Role r INNER JOIN Role_Organization ro" +
            " on ro.role_id=r.id where ro.organization_id = :organizationId")*/
    Flux<Role> queryMe(@Param("organizationId") UUID organizationId, Pageable pageable);

    @Query("select r.* from Role r,  Role_User ru " +
            "where ru.user_id = :userId " +
            " and ru.role_id=r.id")
    Flux<Role> findRolesForUserId(@Param("userId") UUID userId, Pageable pageable);


}
