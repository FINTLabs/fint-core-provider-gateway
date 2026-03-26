package no.fintlabs.provider.register;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface ContractJpaRepository extends JpaRepository<ContractEntity, String> {
}