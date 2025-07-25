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
package io.gravitee.apim.core.api.model.mapper;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.flow.Flow;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class V2toV4MigrationOperator {

    private static final ApiMigration apiMigration = new ApiMigration();
    private static final PlanMigration planMigration = new PlanMigration();
    private static final FlowMigration flowMigration = new FlowMigration();

    public MigrationResult<Api> mapApi(Api source) {
        return apiMigration.mapApi(source);
    }

    public MigrationResult<Plan> mapPlan(Plan plan) {
        return planMigration.mapPlan(plan);
    }

    public MigrationResult<List<Flow>> mapFlows(Iterable<io.gravitee.definition.model.flow.Flow> flows) {
        return flowMigration.mapFlows(flows);
    }
}
