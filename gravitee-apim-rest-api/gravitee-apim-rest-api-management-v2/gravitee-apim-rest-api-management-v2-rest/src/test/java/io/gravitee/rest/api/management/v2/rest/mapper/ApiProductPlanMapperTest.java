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
import org.junit.jupiter.api.Test;

class ApiProductPlanMapperTest {

    private final ApiProductPlanMapper apiProductPlanMapper = ApiProductPlanMapper.INSTANCE;

    @Test
    void mapToPlanUpdates_should_set_empty_tags_when_source_has_null_tags() {
        // API Products don't send tags in update requests - tags are null after mapping.
        // @AfterMapping sets tags to empty set so PlanUpdates.applyTo() matches DB and deploy banner is correct.
        var updatePlan = new UpdateGenericApiProductPlan();
        updatePlan.setName("Updated Plan");
        updatePlan.setDescription("Updated description");

        PlanUpdates result = apiProductPlanMapper.mapToPlanUpdates(updatePlan);

        assertThat(result).isNotNull();
        assertThat(result.getTags()).isNotNull();
        assertThat(result.getTags()).isEmpty();
        assertThat(result.getName()).isEqualTo("Updated Plan");
        assertThat(result.getDescription()).isEqualTo("Updated description");
    }
}
