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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class PlanApiTypeUpgrader implements Upgrader {

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::updatePlanApiType);
    }

    private boolean updatePlanApiType() throws TechnicalException {
        log.info("Starting migration of plan api_type...");

        var apis = apiRepository
            .search(
                new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V4)).build(),
                null,
                ApiFieldFilter.defaultFields()
            )
            .toList();

        for (var api : apis) {
            try {
                populatePlansWithApiTypeForApi(api);
            } catch (TechnicalException e) {
                throw new TechnicalException("Error populating plans for API " + api.getId(), e);
            }
        }

        log.info("Migration of plan api_type completed.");
        return true;
    }

    private void populatePlansWithApiTypeForApi(Api api) throws TechnicalException {
        try {
            for (var plan : planRepository.findByApi(api.getId())) {
                populateApiType(plan, api.getType());
            }
        } catch (TechnicalException e) {
            throw new TechnicalException("Unable to migrate api_type for API" + api.getId() + " {} and its plans", e);
        }
    }

    private void populateApiType(Plan plan, ApiType apiType) throws TechnicalException {
        plan.setApiType(apiType);
        planRepository.update(plan);
        log.info("Updated plan {} with api_type {}", plan.getId(), apiType);
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.PLAN_API_TYPE_UPGRADER;
    }
}
