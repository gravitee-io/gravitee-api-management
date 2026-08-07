/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.services.portal.autofetch;

import io.gravitee.apim.core.portal_page.use_case.AutoFetchPortalNavigationItemsUseCase;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cluster.ClusterManager;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Re-synchronizes the portal navigation pages backed by an external source. Runs independently from the
 * v1 {@code ScheduledAutoFetchService}, which fetches the v1 documentation pages: both have their own
 * enablement flag, their own cron and their own scheduler thread.
 */
@CustomLog
public class ScheduledPortalPageAutoFetchService extends AbstractService implements Runnable {

    private final TaskScheduler scheduler;
    private final String cronTrigger;
    private final boolean enabled;
    private final AutoFetchPortalNavigationItemsUseCase autoFetchPortalNavigationItemsUseCase;
    private final ClusterManager clusterManager;
    private final AtomicLong counter = new AtomicLong(0);

    public ScheduledPortalPageAutoFetchService(
        @Qualifier("portalAutoFetchTaskScheduler") TaskScheduler scheduler,
        @Value("${services.portal_navigation_auto_fetch.cron:0 */5 * * * *}") String cronTrigger,
        @Value("${services.portal_navigation_auto_fetch.enabled:true}") boolean enabled,
        AutoFetchPortalNavigationItemsUseCase autoFetchPortalNavigationItemsUseCase,
        ClusterManager clusterManager
    ) {
        this.scheduler = scheduler;
        this.cronTrigger = cronTrigger;
        this.enabled = enabled;
        this.autoFetchPortalNavigationItemsUseCase = autoFetchPortalNavigationItemsUseCase;
        this.clusterManager = clusterManager;
    }

    @Override
    protected String name() {
        return "Portal Page Auto Fetch Service";
    }

    @Override
    protected void doStart() throws Exception {
        if (!enabled) {
            log.warn("Portal Page Auto Fetch service has been disabled");
            return;
        }
        super.doStart();
        try {
            scheduler.schedule(this, new CronTrigger(cronTrigger));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid cron expression [%s] in property 'services.portal_navigation_auto_fetch.cron'".formatted(cronTrigger),
                e
            );
        }
        log.info("Portal Page Auto Fetch service has been initialized with cron [{}]", cronTrigger);
    }

    /**
     * Only the primary node runs the job. Without this guard every node of a cluster fetches the same
     * pages on the same tick: N times the outbound calls to the remote sources, and concurrent writes
     * on the same items where the whole item is persisted, so the last writer wins.
     */
    @Override
    public void run() {
        if (!clusterManager.self().primary()) {
            log.debug("Portal page auto fetch is not the primary node, skipping execution");
            return;
        }
        var run = counter.incrementAndGet();
        log.debug("Portal page auto fetch #{} started at {}", run, Instant.now());
        try {
            var output = autoFetchPortalNavigationItemsUseCase.execute();
            // Kept at debug when there was nothing to do: the job ticks every 5 minutes on every
            // installation, including those that use no external source at all.
            if (output.succeeded() + output.failed() > 0) {
                log.info("Portal page auto fetch #{}: {} navigation page(s) fetched, {} failed", run, output.succeeded(), output.failed());
            } else {
                log.debug("Portal page auto fetch #{}: no navigation page was due", run);
            }
        } catch (Exception e) {
            log.error("Portal page auto fetch #{} failed: no portal navigation page was fetched during this run", run, e);
        }
        log.debug("Portal page auto fetch #{} ended at {}", run, Instant.now());
    }
}
