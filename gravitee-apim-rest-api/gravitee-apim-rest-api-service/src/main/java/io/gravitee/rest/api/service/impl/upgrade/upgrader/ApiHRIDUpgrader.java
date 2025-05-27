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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ApiHRIDUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Override
    public boolean upgrade() {
        try {
            for (var environment : environmentRepository.findAll()) {
                setHRIDs(new ExecutionContext(environment));
            }
            return true;
        } catch (TechnicalException e) {
            log.error("Error applying upgrader", e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_HRID_UPGRADER;
    }

    private void setHRIDs(ExecutionContext executionContext) {
        apiRepository
            .search(
                new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build(),
                null,
                new ApiFieldFilter.Builder().excludePicture().build()
            )
            .forEach(api -> {
                api.setHrid(api.getCrossId());
                try {
                    apiRepository.update(api);
                } catch (TechnicalException e) {
                    log.error("Unable to set HRID for API {}", api.getId(), e);
                    throw new RuntimeException(e);
                }
            });
    }
}
