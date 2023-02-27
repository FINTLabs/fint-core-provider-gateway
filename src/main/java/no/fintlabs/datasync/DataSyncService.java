package no.fintlabs.datasync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.*;
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
        logEntities(syncPageOfObject.getClass().toString(), syncPageOfObject.getMetadata(), syncPageOfObject.getResources().size());
        validator.validateOrgId(jwt, syncPageOfObject.getMetadata().getOrgId());
        syncPageService.doSync(syncPageOfObject, domain, packageName, entity);
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
