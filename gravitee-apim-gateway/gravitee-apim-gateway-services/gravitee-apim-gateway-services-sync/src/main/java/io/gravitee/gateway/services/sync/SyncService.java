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
package io.gravitee.gateway.services.sync;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.services.sync.healthcheck.SyncProcessProbe;
import io.gravitee.node.api.healthcheck.ProbeManager;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SyncService extends AbstractService {

    @Value("${services.sync.enabled:true}")
    private boolean enabled;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private SharedPolicyGroupManager sharedPolicyGroupManager;

    @Autowired(required = false)
    private List<SyncManager> syncManagers;

    @Autowired
    private ProbeManager probeManager;

    @Autowired
    private SyncProcessProbe syncProcessProbe;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            // Register sync prob
            probeManager.register(syncProcessProbe);

            // Force refresh based on internal state of the api manager and shared policy group manager (useful if apis definitions are maintained across the cluster).
            apiManager.refresh();
            sharedPolicyGroupManager.refresh();

            // Initialize the sync managers.
            if (syncManagers != null) {
                syncManagers.forEach(syncManager -> {
                    try {
                        syncManager.start();
                    } catch (Exception e) {
                        log.error("Unable to start sync manager {}", syncManager.getClass().getSimpleName(), e);
                    }
                });
            }
        } else {
            log.warn("Sync service is disabled");
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (syncManagers != null) {
            syncManagers.forEach(syncManager -> {
                try {
                    syncManager.stop();
                } catch (Exception e) {
                    log.error("Unable to start sync manager {}", syncManager.getClass().getSimpleName(), e);
                    throw new RuntimeException(e);
                }
            });
        }
        super.doStop();
    }

    @Override
    protected String name() {
        return "Gateway Sync Service";
    }
}
