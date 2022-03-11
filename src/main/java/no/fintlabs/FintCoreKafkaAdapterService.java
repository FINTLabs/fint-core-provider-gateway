package no.fintlabs;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.AdapterContract;
import no.fintlabs.adapter.models.AdapterPing;
import no.fintlabs.kafka.entity.EntityProducer;
import no.fintlabs.kafka.entity.EntityProducerRecord;
import no.fintlabs.kafka.entity.EntityTopicNameParameters;
import no.fintlabs.kafka.entity.FintKafkaEntityProducerFactory;
import no.fintlabs.kafka.event.EventProducer;
import no.fintlabs.kafka.event.EventProducerRecord;
import no.fintlabs.kafka.event.EventTopicNameParameters;
import no.fintlabs.kafka.event.FintKafkaEventProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class FintCoreKafkaAdapterService {
    private final EventProducer<AdapterPing> adapterPingEventProducer;
    private final EventProducer<AdapterContract> adapterContractEventProducer;
    private final EntityProducer<Object> entityProducer;
    private final FintKafkaEntityProducerFactory entityProducerFactory;
    private final FintKafkaEventProducerFactory eventProducerFactory;

    public FintCoreKafkaAdapterService(FintKafkaEntityProducerFactory entityProducerFactory, FintKafkaEventProducerFactory eventProducerFactory) {
        this.adapterPingEventProducer = eventProducerFactory.createProducer(AdapterPing.class);
        this.adapterContractEventProducer = eventProducerFactory.createProducer(AdapterContract.class);
        this.entityProducer = entityProducerFactory.createProducer(Object.class);
        this.entityProducerFactory = entityProducerFactory;
        this.eventProducerFactory = eventProducerFactory;
    }





    public void ping(AdapterPing adapterPing) throws JsonProcessingException {
        //EventProducer<AdapterPing> adapterPingEventProducer = eventProducerFactory.createProducer(AdapterPing.class);
        adapterPingEventProducer.send(
                EventProducerRecord.<AdapterPing>builder()
                        .topicNameParameters(EventTopicNameParameters
                                .builder()
                                .orgId(adapterPing.getOrgId())
                                .domainContext("fint-core")
                                .eventName("adapter-health")
                                .build())
                        .value(adapterPing)
                        .build()
        );

//                .send(
//                EventTopicNameParameters
//                        .builder()
//                        .orgId(adapterPing.getOrgId())
//                        .domainContext("fint-core")
//                        .eventName("adapter-health")
//                        .build(),
//                adapterPing
//        );
    }

    public void register(AdapterContract adapterContract) throws JsonProcessingException {
        //EventProducer<AdapterContract> adapterContractEventProducer = eventProducerFactory.createProducer(AdapterContract.class);
        adapterContractEventProducer.send(
                EventProducerRecord.<AdapterContract>builder()
                        .topicNameParameters(EventTopicNameParameters
                                .builder()
                                .orgId(adapterContract.getOrgId())
                                .domainContext("fint-core")
                                .eventName("adapter-register")
                                .build())
                        .value(adapterContract)
                        .build()
        );
//        adapterPingEventProducer.send(
//                EventTopicNameParameters
//                        .builder()
//                        .orgId(adapterContract.getOrgId())
//                        .domainContext("fint-core")
//                        .eventName("adapter-register")
//                        .build(),
//                adapterContract
//        );
    }

    public ListenableFuture<SendResult<String, Object>> entity(String orgId, String domain, String packageName, String entityName, HashMap<String, Object> entity)  {
        return entityProducer.send(
                EntityProducerRecord.builder()
                        .topicNameParameters(EntityTopicNameParameters
                                .builder()
                                .orgId(orgId)
                                .domainContext("fint-core")
                                .resource(String.format("%s-%s-%s", domain, packageName, entityName))
                                .build())
                        .value(entity)
                        .key(getKey(entity))
                        .build()
        );
//        entityProducer.send(
//                EntityTopicNameParameters
//                        .builder()
//                        .orgId(orgId)
//                        .domainContext("fint-core")
//                        .resource(String.format("%s-%s-%s", domain, packageName, entityName))
//                        .build(),
//                getKey(entity),
//                entity
//        );
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
