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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan.ValidationEnum;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductPlanMapperTest {

    @Test
    void should_map_only_api_product_supported_fields() throws JsonProcessingException {
        var plan = Plan.builder()
            .id("plan-id")
            .name("API Key plan")
            .description("API Product plan")
            .characteristics(List.of("featured"))
            .order(2)
            .commentRequired(true)
            .commentMessage("Why do you need access?")
            .generalConditions("terms-page-id")
            .definitionVersion(DefinitionVersion.V4)
            .validation(Plan.PlanValidationType.MANUAL)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .mode(PlanMode.STANDARD)
                    .status(PlanStatus.PUBLISHED)
                    .security(PlanSecurity.builder().type("api-key").build())
                    .build()
            )
            .build();

        var result = ApiProductPlanMapper.INSTANCE.map(plan);

        assertThat(result.getId()).isEqualTo("plan-id");
        assertThat(result.getName()).isEqualTo("API Key plan");
        assertThat(result.getDescription()).isEqualTo("API Product plan");
        assertThat(result.getCharacteristics()).containsExactly("featured");
        assertThat(result.getOrder()).isEqualTo(2);
        assertThat(result.getSecurity()).isEqualTo(SecurityEnum.API_KEY);
        assertThat(result.getValidation()).isEqualTo(ValidationEnum.MANUAL);
        assertThat(result.getMode()).isEqualTo(io.gravitee.rest.api.portal.rest.model.PlanMode.STANDARD);
        assertThat(result.getCommentRequired()).isTrue();
        assertThat(result.getCommentQuestion()).isEqualTo("Why do you need access?");

        var json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("general_conditions", "generalConditions", "usage_configuration", "usageConfiguration");
    }
}
