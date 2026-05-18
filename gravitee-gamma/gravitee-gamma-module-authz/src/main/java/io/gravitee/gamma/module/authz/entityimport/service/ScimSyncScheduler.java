package io.gravitee.gamma.module.authz.entityimport.service;

import io.gravitee.gamma.module.authz.entityimport.repository.ScimConnectorDocument;
import java.time.Clock;
import java.time.Instant;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class ScimSyncScheduler {

    private final ScimConnectorService connectorService;
    private final Clock clock;

    @Autowired
    public ScimSyncScheduler(ScimConnectorService connectorService) {
        this(connectorService, Clock.systemUTC());
    }

    ScimSyncScheduler(ScimConnectorService connectorService, Clock clock) {
        this.connectorService = connectorService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 10_000L, initialDelay = 5_000L)
    public void syncAll() {
        Instant now = Instant.now(clock);
        for (ScimConnectorDocument c : connectorService.findAllForScheduler()) {
            if (!isDue(c, now)) continue;
            try {
                connectorService.runScheduledSync(c);
            } catch (Exception ex) {
                log.warn("SCIM sync failed for connector {} (env {}): {}", c.getName(), c.getEnvironmentId(), ex.getMessage());
            }
        }
    }

    private static boolean isDue(ScimConnectorDocument c, Instant now) {
        if (c.getLastSyncAt() == null) return true;
        return c.getLastSyncAt().plusSeconds(ScimConnectorService.intervalSecondsOf(c)).isBefore(now);
    }
}
