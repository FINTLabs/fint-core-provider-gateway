package no.fintlabs.datasync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.SyncPage;
import no.fintlabs.adapter.models.SyncPageMetadata;
import no.fintlabs.adapter.models.SyncType;
import no.fintlabs.utils.AdapterRequestValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataSyncService {

    private final AdapterRequestValidator validator;
    private final SyncPageService syncPageService;

    public <T extends SyncPage<Object>> void registerSync(Jwt jwt, T syncPageOfObject, final String domain, final String packageName, final String entity) {
        logEntities(syncPageOfObject.getSyncType(), syncPageOfObject.getMetadata(), syncPageOfObject.getResources().size());
        validator.validateRole(jwt, domain, packageName);
        validator.validateOrgId(jwt, syncPageOfObject.getMetadata().getOrgId());
        syncPageService.doSync(syncPageOfObject, domain, packageName, entity);
    }

    private static void logEntities(SyncType syncType, SyncPageMetadata metadata, int resourceSize) {
        log.info("Start {} sync: {}({}), {}, total size: {}, page size: {}, page: {}, total pages: {}",
                syncType.toString().toLowerCase(),
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
