package io.gravitee.gamma.module.authz.entityimport.service;

import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls every registered SCIM connector on a fixed cadence and reconciles its
 * users/groups into the entity graph. Cadence is intentionally short for the demo
 * (10s) — production should externalise this to gravitee.yml.
 */
@CustomLog
@Component
public class ScimSyncScheduler {

    private final ScimConnectorService connectorService;

    @Autowired
    public ScimSyncScheduler(ScimConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @Scheduled(fixedDelay = 10_000L, initialDelay = 5_000L)
    public void syncAll() {
        for (ScimConnectorDocument c : connectorService.findAllForScheduler()) {
            try {
                connectorService.runScheduledSync(c);
            } catch (Exception ex) {
                log.warn("SCIM sync failed for connector {} (env {}): {}", c.getName(), c.getEnvironmentId(), ex.getMessage());
            }
        }
    }
}
