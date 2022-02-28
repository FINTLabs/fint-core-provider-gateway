package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.entity.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.FintKafkaEntityProducerService;
import no.fintlabs.kafka.event.EventTopicNameParameters;
import no.fintlabs.kafka.event.FintKafkaEventProducerService;
import no.fintlabs.model.AdapterContract;
import no.fintlabs.model.AdapterPing;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FintCoreKafkaAdapterService {
    private final FintKafkaEventProducerService fintKafkaEventProducerService;
    private final FintKafkaEntityProducerService fintKafkaEntityProducerService;

    public FintCoreKafkaAdapterService(FintKafkaEventProducerService fintKafkaEventProducerService, FintKafkaEntityProducerService fintKafkaEntityProducerService) {
        this.fintKafkaEventProducerService = fintKafkaEventProducerService;
        this.fintKafkaEntityProducerService = fintKafkaEntityProducerService;
    }

    public void ping(AdapterPing adapterPing) throws JsonProcessingException {
        fintKafkaEventProducerService.send(
                EventTopicNameParameters
                        .builder()
                        .orgId(adapterPing.getOrgId())
                        .domainContext("fint-core")
                        .eventName("adapter-health")
                        .build(),
                adapterPing
        );
    }

    public void register(AdapterContract adapterContract) throws JsonProcessingException {
        fintKafkaEventProducerService.send(
                EventTopicNameParameters
                        .builder()
                        .orgId(adapterContract.getOrgId())
                        .domainContext("fint-core")
                        .eventName("adapter-register")
                        .build(),
                adapterContract
        );
    }

    public void entity(String orgId, String domain, String packageName, String entityName, HashMap<String, ?> entity) throws JsonProcessingException, KafkaException {
            fintKafkaEntityProducerService.send(
                    EntityTopicNameParameters
                            .builder()
                            .orgId(orgId)
                            .domainContext("fint-core")
                            .resource(String.format("%s-%s-%s", domain, packageName, entityName))
                            .build(),
                    getKey(entity),
                    entity
            );
    }

    private String getKey(HashMap<?, ?> resource) {
        HashMap<?, ?> links = (HashMap<?, ?>) resource.get("_links");
        List<HashMap<String, String>> selfLinks = (List<HashMap<String, String>>) links.get("self");
        List<String> selfLinksList = selfLinks.stream()
                .filter(o -> o.containsKey("href"))
                .map(o -> o.get("href"))
                .map(k -> k.replaceFirst("^https:/\\/.+\\.felleskomponent.no", ""))
                .sorted()
                .toList();
        return selfLinksList.get(0);


    }
}
