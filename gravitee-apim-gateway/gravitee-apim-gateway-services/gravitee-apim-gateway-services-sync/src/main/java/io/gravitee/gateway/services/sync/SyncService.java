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
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.gateway.services.sync.handler.SyncHandler;
import io.gravitee.gateway.services.sync.healthcheck.ApiSyncProbe;
import io.gravitee.gateway.services.sync.kubernetes.KubernetesSyncService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.vertx.ext.web.Router;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncService extends AbstractService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SyncService.class);

    private static final String PATH = "/sync";

    @Value("${services.sync.delay:5000}")
    private int delay;

    @Value("${services.sync.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${services.sync.enabled:true}")
    private boolean enabled;

    @Value("${services.sync.kubernetes.enabled:true}")
    private boolean kubernetesEnabled;

    @Value("${services.local.enabled:false}")
    private boolean localRegistryEnabled;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private SyncManager syncManager;

    @Autowired
    @Qualifier("managementRouter")
    private Router router;

    @Autowired
    private ApiKeysCacheService apiKeysCacheService;

    @Autowired
    private SubscriptionsCacheService subscriptionsCacheService;

    @Autowired
    private KubernetesSyncService kubernetesSyncService;

    @Autowired
    private ProbeManager probeManager;

    @Autowired
    private ApiSyncProbe apiSyncProbe;

    @Autowired
    private Node node;

    @Override
    protected void doStart() throws Exception {
        if (!localRegistryEnabled) {
            if (enabled) {
                super.doStart();

                logger.info("Sync service has been initialized with delay [{}{}]", delay, unit.name());

                probeManager.register(this.apiSyncProbe);

                logger.info("Associate a new HTTP handler on {}", PATH);

                // Create and associate handler
                SyncHandler syncHandler = new SyncHandler();
                applicationContext.getAutowireCapableBeanFactory().autowireBean(syncHandler);
                router.get(PATH).produces(MediaType.APPLICATION_JSON).handler(syncHandler);

                // Start tasks
                apiKeysCacheService.start();
                subscriptionsCacheService.start();

                // Force refresh based on internal state of the api manager (useful if apis definitions are maintained across the cluster).
                apiManager.refresh();

                // Initialize the sync manager.
                syncManager.start();

                // Set environments list to syncManager
                syncManager.setEnvironments(new ArrayList<>((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS)));

                // Run a first refresh immediately.
                syncManager.refresh();

                // Initial sync has been made, start schedulers.
                syncManager.startScheduler(delay, unit);
                apiKeysCacheService.startScheduler(delay, unit);
                subscriptionsCacheService.startScheduler(delay, unit);

                if (kubernetesEnabled) {
                    kubernetesSyncService.start();
                }
            } else {
                logger.warn("Sync service is disabled");
            }
        } else {
            logger.warn("Sync service is disabled because local registry mode is enabled");
        }
    }

    @Override
    protected void doStop() throws Exception {
        syncManager.stop();
        apiKeysCacheService.stop();
        subscriptionsCacheService.stop();

        if (kubernetesEnabled) {
            kubernetesSyncService.stop();
        }

        super.doStop();
    }

    public boolean isAllApisSync() {
        return syncManager.isAllApisSync();
    }

    @Override
    protected String name() {
        return "Gateway Sync Service";
    }
}
