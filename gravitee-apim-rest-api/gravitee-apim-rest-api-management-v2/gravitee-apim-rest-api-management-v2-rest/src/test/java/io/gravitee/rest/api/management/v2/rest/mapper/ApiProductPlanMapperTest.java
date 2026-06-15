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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericApiProductPlan;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiProductPlanMapperTest {

    private final ApiProductPlanMapper apiProductPlanMapper = ApiProductPlanMapper.INSTANCE;

    @Test
    void mapToPlanUpdates_should_leave_tags_null_when_source_omits_tags() {
        var updatePlan = new UpdateGenericApiProductPlan();
        updatePlan.setName("Updated Plan");
        updatePlan.setDescription("Updated description");

        PlanUpdates result = apiProductPlanMapper.mapToPlanUpdates(updatePlan);

        assertThat(result).isNotNull();
        assertThat(result.getTags()).isNull();
        assertThat(result.getName()).isEqualTo("Updated Plan");
        assertThat(result.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void mapToPlanUpdates_should_map_explicit_plan_tags() {
        var updatePlan = new UpdateGenericApiProductPlan();
        updatePlan.setName("Updated Plan");
        updatePlan.setTags(List.of("internal"));

        PlanUpdates result = apiProductPlanMapper.mapToPlanUpdates(updatePlan);

        assertThat(result.getTags()).containsExactly("internal");
    }
}
