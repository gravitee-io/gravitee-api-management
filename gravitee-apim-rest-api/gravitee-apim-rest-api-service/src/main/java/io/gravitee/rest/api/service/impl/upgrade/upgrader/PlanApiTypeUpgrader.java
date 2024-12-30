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
    public boolean upgrade() {
        try {
            updatePlanApiType();
        } catch (Exception e) {
            log.error("Unable to apply upgrader {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    private void updatePlanApiType() throws TechnicalException {
        log.info("Starting migration of plan api_type...");

        apiRepository
            .search(
                new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V4)).build(),
                null,
                ApiFieldFilter.defaultFields()
            )
            .forEach(this::populatePlansWithApiTypeForApi);

        log.info("Migration of plan api_type completed.");
    }

    private void populatePlansWithApiTypeForApi(Api api) {
        try {
            planRepository
                .findByApi(api.getId())
                .stream()
                .forEach(plan -> {
                    try {
                        populateApiType(plan, api.getType());
                    } catch (TechnicalException e) {
                        log.error("Unable to update api_type for plan {}", plan.getId(), e);
                    }
                });
        } catch (Exception e) {
            log.error("Unable to migrate api_type for API {} and its plans", api.getId(), e);
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
