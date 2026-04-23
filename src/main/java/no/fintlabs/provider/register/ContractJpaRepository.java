package no.fintlabs.provider.register;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

@EnableJpaRepositories
public interface ContractJpaRepository extends JpaRepository<ContractEntity, String> {

    @Query("SELECT DISTINCT adapterId FROM ContractEntity")
    Set<String> getAdapterIds();

    @Query("SELECT DISTINCT userName FROM ContractEntity")
    boolean existsByUsername(String username);

    @Query("""
        select distinct c
        from ContractEntity c
        left join fetch c.capabilityEntityset
        where c.userName = :userName
    """)
    Optional<ContractEntity> findByUserNameWithCapabilities(@Param("userName") String userName);

}