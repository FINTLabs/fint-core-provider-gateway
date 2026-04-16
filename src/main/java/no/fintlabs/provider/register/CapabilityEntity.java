package no.fintlabs.provider.register;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.adapter.models.AdapterCapability;

import java.util.Optional;

@Getter
@Setter
@Entity
@Table(name = "capabilities")
public class CapabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_name", nullable = false)
    private ContractEntity contractEntity;

    private String domainName;
    private String pkgName;
    private String resourceName;
    private int fullSyncIntervalInDays;
    private String deltaSyncInterval;

    public CapabilityEntity(AdapterCapability capability) {
        this.domainName = capability.getDomainName();
        this.pkgName = capability.getPackageName();
        this.resourceName = capability.getResourceName();
        this.fullSyncIntervalInDays = capability.getFullSyncIntervalInDays();
        this.deltaSyncInterval = Optional.ofNullable(capability.getDeltaSyncInterval())
                .map(Object::toString)
                .orElse("Not Present");
    }

    public CapabilityEntity() {
    }
}