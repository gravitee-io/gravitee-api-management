/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync;

import io.gravitee.common.http.MediaType;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.services.sync.handler.SyncHandler;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ScheduledSyncService extends AbstractService implements Runnable {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ScheduledSyncService.class);

    private final static String PATH = "/sync";

    @Autowired
    private TaskScheduler scheduler;

    @Value("${services.sync.cron:*/5 * * * * *}")
    private String cronTrigger;

    @Value("${services.sync.enabled:true}")
    private boolean enabled;

    @Value("${services.local.enabled:false}")
    private boolean localRegistryEnabled;

    @Autowired
    private SyncManager syncStateManager;

    @Autowired
    private Router router;

    @Override
    protected void doStart() throws Exception {
        if (! localRegistryEnabled) {
            if (enabled) {
                super.doStart();
                logger.info("Sync service has been initialized with cron [{}]", cronTrigger);

                logger.info("Associate a new HTTP handler on {}", PATH);

                // Create and associate handler
                SyncHandler syncHandler = new SyncHandler();
                applicationContext.getAutowireCapableBeanFactory().autowireBean(syncHandler);
                router.get(PATH).produces(MediaType.APPLICATION_JSON).handler(syncHandler);

                // Sync must start only when doStart() is invoked, that's the reason why we are not
                // using @Scheduled annotation on doSync() method.
                syncStateManager.refresh();

                // Star cron
                scheduler.schedule(this, new CronTrigger(cronTrigger));
            } else {
                logger.warn("Sync service has been disabled");
            }
        } else {
            logger.warn("Sync service is disabled because local registry mode is enabled");
        }
    }

    @Override
    public void run() {
        syncStateManager.refresh();
    }

    @Override
    protected String name() {
        return "Gateway Sync Service";
    }
}
