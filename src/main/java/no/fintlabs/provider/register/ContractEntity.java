package no.fintlabs.provider.register;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import no.fintlabs.adapter.models.AdapterContract;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "contract")
public class ContractEntity {

    @Id
    private String userName;
    private String adapterId;
    private String orgId;
    private int heartbeatIntervalInMinutes;

    @OneToMany(mappedBy = "contractEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CapabilityEntity> capabilityEntityset;

    public ContractEntity(AdapterContract adapterContract) {
        this.orgId = adapterContract.getOrgId();
        this.adapterId = adapterContract.getAdapterId();
        this.userName = adapterContract.getUsername();
        this.heartbeatIntervalInMinutes = adapterContract.getHeartbeatIntervalInMinutes();
        this.capabilityEntityset = adapterContract.getCapabilities().stream().map(capability -> {
            CapabilityEntity entity = new CapabilityEntity(capability);
            entity.setContractEntity(this);
            return entity;
        }).collect(Collectors.toSet());
    }

    public ContractEntity() {

    }
}