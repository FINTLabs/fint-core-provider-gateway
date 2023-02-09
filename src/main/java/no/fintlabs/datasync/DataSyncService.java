package no.fintlabs.datasync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.utils.AdapterRequestValidator;
import no.fintlabs.adapter.models.DeltaSyncPageOfObject;
import no.fintlabs.adapter.models.FullSyncPageOfObject;
import no.fintlabs.adapter.models.SyncPageMetadata;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataSyncService {

    private final AdapterRequestValidator validator;
    private final SyncPageService syncPageService;

    public void registerDeltaSync(Jwt jwt, DeltaSyncPageOfObject entities, final String domain, final String packageName, final String entity) {
        logEntities("Delta sync", entities.getMetadata(), entities.getResources().size());
        validator.validateOrgId(jwt, entities.getMetadata().getOrgId());
        syncPageService.doDeltaSync(entities, domain, packageName, entity);
    }

    public void registerFullSync(Jwt jwt, FullSyncPageOfObject entities, final String domain, final String packageName, final String entity) {
        logEntities("Full sync", entities.getMetadata(), entities.getResources().size());
        validator.validateOrgId(jwt, entities.getMetadata().getOrgId());
        syncPageService.doFullSync(entities, domain, packageName, entity);
    }

    private static void logEntities(String syncType, SyncPageMetadata metadata, int resourceSize) {
        log.info("{}: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
                syncType,
                metadata.getCorrId(),
                metadata.getOrgId(),
                metadata.getUriRef(),
                metadata.getTotalSize(),
                resourceSize,
                metadata.getPage(),
                metadata.getTotalPages()
        );
    }

}
