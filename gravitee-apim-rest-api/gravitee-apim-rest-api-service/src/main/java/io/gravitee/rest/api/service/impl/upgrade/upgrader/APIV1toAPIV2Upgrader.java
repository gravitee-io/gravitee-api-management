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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class APIV1toAPIV2Upgrader implements Upgrader {

    private final ApiRepository apiRepository;
    private final ApiService apiService;

    @Autowired
    public APIV1toAPIV2Upgrader(@Lazy ApiRepository apiRepository, @Lazy ApiService apiService) {
        this.apiRepository = apiRepository;
        this.apiService = apiService;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_V1_TO_V2_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            migrateApiV1toApiV2();
        } catch (Exception e) {
            log.error("error occurred while upgrading V1 APIs to V2 APIs", e);
            return false;
        }
        return true;
    }

    private void migrateApiV1toApiV2() {
        log.info("Starting migrating API v1 to API v2");

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ArrayList<String> migratedApiIds = new ArrayList<>();
        ArrayList<String> notMigratedApiIds = new ArrayList<>();

        final Date now = new Date();

        apiRepository
            .searchV1ApisId()
            .forEach(apiId -> {
                try {
                    ApiEntity migratedApi = apiService.migrate(executionContext, apiId);
                    if (migratedApi.getUpdatedAt().after(now)) {
                        migratedApiIds.add(apiId);
                    }
                } catch (Exception e) {
                    log.error("Api {} has not been migrated to v2", apiId, e);
                    notMigratedApiIds.add(apiId);
                }
            });

        if (migratedApiIds.isEmpty()) {
            log.info("No V1 API has been migrate");
        } else {
            log.info("{} V1 APIs have been successfully migrated to V2 APIs!", migratedApiIds.size());
        }
        if (!notMigratedApiIds.isEmpty()) {
            log.info("{} V1 APIs haven't been migrated to V2 APIs. Check logs for more details.", notMigratedApiIds.size());
        }
    }
}
