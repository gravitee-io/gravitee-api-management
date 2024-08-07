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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.PlanFixtures;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.PlanValidation;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericPlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanFederated;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import java.util.HashSet;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class PlanMapperTest {

    private final PlanMapper planMapper = Mappers.getMapper(PlanMapper.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    void should_map_PlanEntity_to_PlanV4() {
        final var planEntity = PlanFixtures.aPlanEntityV4();
        final var plan = planMapper.map(planEntity);

        assertEquals(planEntity.getId(), plan.getId());
        assertEquals(planEntity.getApiId(), plan.getApiId());
        assertEquals(planEntity.getName(), plan.getName());
        assertEquals(planEntity.getDescription(), plan.getDescription());
        assertEquals(planEntity.getOrder(), (int) plan.getOrder());
        assertEquals(planEntity.getCharacteristics(), plan.getCharacteristics());
        assertEquals(planEntity.getCreatedAt().getTime(), plan.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getUpdatedAt().getTime(), plan.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getCommentMessage(), plan.getCommentMessage());
        assertEquals(planEntity.getCrossId(), plan.getCrossId());
        assertEquals(planEntity.getGeneralConditions(), plan.getGeneralConditions());
        assertEquals(planEntity.getTags(), new HashSet<>(plan.getTags()));
        assertEquals(planEntity.getStatus().name(), plan.getStatus().name());
        assertEquals(planEntity.getType().name(), plan.getType().name());
        assertEquals(planEntity.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(planEntity.getValidation().name(), plan.getValidation().name());
        assertEquals(planEntity.getSelectionRule(), plan.getSelectionRule());

        assertSecurityV4Equals(planEntity.getSecurity(), plan.getSecurity());
        assertEquals(planEntity.getFlows().size(), plan.getFlows().size()); // Flow mapping is tested in FlowMapperTest
    }

    @Test
    void should_map_PlanEntity_to_PlanV2() {
        final var planEntity = PlanFixtures.aPlanEntityV2();
        final var plan = planMapper.map(planEntity);

        assertEquals(planEntity.getId(), plan.getId());
        assertEquals(planEntity.getApiId(), plan.getApiId());
        assertEquals(planEntity.getName(), plan.getName());
        assertEquals(planEntity.getDescription(), plan.getDescription());
        assertEquals(planEntity.getOrder(), (int) plan.getOrder());
        assertEquals(planEntity.getCharacteristics(), plan.getCharacteristics());
        assertEquals(planEntity.getCreatedAt().getTime(), plan.getCreatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getUpdatedAt().getTime(), plan.getUpdatedAt().toInstant().toEpochMilli());
        assertEquals(planEntity.getCommentMessage(), plan.getCommentMessage());
        assertEquals(planEntity.getCrossId(), plan.getCrossId());
        assertEquals(planEntity.getGeneralConditions(), plan.getGeneralConditions());
        assertEquals(planEntity.getTags(), new HashSet<>(plan.getTags()));
        assertEquals(planEntity.getStatus().name(), plan.getStatus().name());
        assertEquals(planEntity.getType().name(), plan.getType().name());
        assertEquals(planEntity.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(planEntity.getValidation().name(), plan.getValidation().name());
        assertEquals(planEntity.getSelectionRule(), plan.getSelectionRule());
        assertEquals(planEntity.getPlanSecurity().getType(), plan.getSecurity().getType().name());
        assertDefinitionEquals(planEntity.getSecurityDefinition(), plan.getSecurity().getConfiguration());

        assertEquals(planEntity.getFlows().size(), plan.getFlows().size()); // Flow mapping is tested in FlowMapperTest
    }

    @Test
    void should_map_CreatePlanV4_to_core_Plan() {
        final var createPlanV4 = PlanFixtures.aCreatePlanV4();
        final var plan = planMapper.map(createPlanV4);

        Assertions.assertNull(plan.getId());
        assertEquals(createPlanV4.getName(), plan.getName());
        assertEquals(createPlanV4.getDescription(), plan.getDescription());
        assertEquals(createPlanV4.getOrder(), plan.getOrder());
        assertEquals(createPlanV4.getCharacteristics(), plan.getCharacteristics());
        assertEquals(createPlanV4.getCommentMessage(), plan.getCommentMessage());
        assertEquals(createPlanV4.getCrossId(), plan.getCrossId());
        assertEquals(createPlanV4.getGeneralConditions(), plan.getGeneralConditions());
        assertEquals(new HashSet<>(createPlanV4.getTags()), plan.getPlanDefinitionV4().getTags());
        assertEquals(createPlanV4.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(createPlanV4.getValidation().name(), plan.getValidation().name());
        assertEquals(createPlanV4.getSelectionRule(), plan.getPlanDefinitionV4().getSelectionRule());

        assertSecurityV4Equals(plan.getPlanSecurity(), createPlanV4.getSecurity());
    }

    @Test
    void should_map_CreatePlanV4_to_core_Plan_with_default_values() {
        final var createPlanV4 = PlanFixtures.aCreatePlanV4().toBuilder().validation(null).mode(null).flows(null).build();
        final var plan = planMapper.map(createPlanV4);

        Assertions.assertNull(plan.getId());
        assertNotNull(plan.getValidation());
        assertEquals(PlanValidationType.MANUAL.name(), plan.getValidation().name());

        assertNotNull(plan.getPlanMode());
        assertEquals(PlanMode.STANDARD.name(), plan.getPlanMode().name());
    }

    @Test
    void should_map_UpdatePlanV4_to_UpdatePlanEntity() {
        final var updatePlanV4 = PlanFixtures.anUpdatePlanV4();
        final var updatePlanEntity = planMapper.map(updatePlanV4);

        Assertions.assertNull(updatePlanEntity.getId());
        assertEquals(updatePlanV4.getName(), updatePlanEntity.getName());
        assertEquals(updatePlanV4.getDescription(), updatePlanEntity.getDescription());
        assertEquals(updatePlanV4.getOrder(), updatePlanEntity.getOrder());
        assertEquals(updatePlanV4.getCharacteristics(), updatePlanEntity.getCharacteristics());
        assertEquals(updatePlanV4.getCommentMessage(), updatePlanEntity.getCommentMessage());
        assertEquals(updatePlanV4.getCrossId(), updatePlanEntity.getCrossId());
        assertEquals(updatePlanV4.getGeneralConditions(), updatePlanEntity.getGeneralConditions());
        assertEquals(new HashSet<>(updatePlanV4.getTags()), updatePlanEntity.getTags());
        assertEquals(updatePlanV4.getExcludedGroups(), updatePlanEntity.getExcludedGroups());
        assertEquals(updatePlanV4.getValidation().name(), updatePlanEntity.getValidation().name());
        assertEquals(updatePlanV4.getSelectionRule(), updatePlanEntity.getSelectionRule());

        assertEquals(updatePlanEntity.getFlows().size(), updatePlanV4.getFlows().size()); // Flow mapping is tested in FlowMapperTest
    }

    @Test
    void should_map_CreatePlanV2_to_CreatePlanEntity() {
        final var createPlanV2 = PlanFixtures.aCreatePlanV2();
        final var createPlanEntity = planMapper.map(createPlanV2);

        Assertions.assertNull(createPlanEntity.getId());
        assertEquals(createPlanV2.getName(), createPlanEntity.getName());
        assertEquals(createPlanV2.getDescription(), createPlanEntity.getDescription());
        assertEquals(createPlanV2.getOrder(), createPlanEntity.getOrder());
        assertEquals(createPlanV2.getCharacteristics(), createPlanEntity.getCharacteristics());
        assertEquals(createPlanV2.getCommentMessage(), createPlanEntity.getCommentMessage());
        assertEquals(createPlanV2.getCrossId(), createPlanEntity.getCrossId());
        assertEquals(createPlanV2.getGeneralConditions(), createPlanEntity.getGeneralConditions());
        assertEquals(new HashSet<>(createPlanV2.getTags()), createPlanEntity.getTags());
        assertEquals(createPlanV2.getExcludedGroups(), createPlanEntity.getExcludedGroups());
        assertEquals(createPlanV2.getValidation().name(), createPlanEntity.getValidation().name());
        assertEquals(createPlanV2.getSelectionRule(), createPlanEntity.getSelectionRule());

        assertEquals(createPlanV2.getSecurity().getType().name(), createPlanEntity.getSecurity().name());
        assertDefinitionEquals(createPlanV2.getSecurity().getConfiguration(), createPlanEntity.getSecurityDefinition());

        assertEquals(createPlanEntity.getFlows().size(), createPlanV2.getFlows().size()); // Flow mapping is tested in FlowMapperTest
    }

    @Test
    void should_map_CreatePlanV2_to_CreatePlanEntity_ignoring_mtls_plan_security_type() {
        final var createPlanV2 = PlanFixtures.aCreatePlanV2();
        // PlanSecurityType is a common enum containing MTLS plan. Such security type is not supported by V2, so it is ignored during mapping
        createPlanV2.security(
            io.gravitee.rest.api.management.v2.rest.model.PlanSecurity
                .builder()
                .type(io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType.MTLS)
                .build()
        );
        final var createPlanEntity = planMapper.map(createPlanV2);

        Assertions.assertNull(createPlanEntity.getId());
        assertEquals(createPlanV2.getName(), createPlanEntity.getName());
        assertEquals(createPlanV2.getDescription(), createPlanEntity.getDescription());
        assertEquals(createPlanV2.getOrder(), createPlanEntity.getOrder());
        assertEquals(createPlanV2.getCharacteristics(), createPlanEntity.getCharacteristics());
        assertEquals(createPlanV2.getCommentMessage(), createPlanEntity.getCommentMessage());
        assertEquals(createPlanV2.getCrossId(), createPlanEntity.getCrossId());
        assertEquals(createPlanV2.getGeneralConditions(), createPlanEntity.getGeneralConditions());
        assertEquals(new HashSet<>(createPlanV2.getTags()), createPlanEntity.getTags());
        assertEquals(createPlanV2.getExcludedGroups(), createPlanEntity.getExcludedGroups());
        assertEquals(createPlanV2.getValidation().name(), createPlanEntity.getValidation().name());
        assertEquals(createPlanV2.getSelectionRule(), createPlanEntity.getSelectionRule());

        assertNull(createPlanEntity.getSecurity());
        assertDefinitionEquals(createPlanV2.getSecurity().getConfiguration(), createPlanEntity.getSecurityDefinition());

        assertEquals(createPlanEntity.getFlows().size(), createPlanV2.getFlows().size()); // Flow mapping is tested in FlowMapperTest
    }

    @Test
    void should_map_UpdatePlanV2_to_UpdatePlanEntity() {
        final var updatePlanV2 = PlanFixtures.anUpdatePlanV2();
        final var updatePlanEntity = planMapper.map(updatePlanV2);

        Assertions.assertNull(updatePlanEntity.getId());
        assertEquals(updatePlanV2.getName(), updatePlanEntity.getName());
        assertEquals(updatePlanV2.getDescription(), updatePlanEntity.getDescription());
        assertEquals(updatePlanV2.getOrder(), updatePlanEntity.getOrder());
        assertEquals(updatePlanV2.getCharacteristics(), updatePlanEntity.getCharacteristics());
        assertEquals(updatePlanV2.getCommentMessage(), updatePlanEntity.getCommentMessage());
        assertEquals(updatePlanV2.getCrossId(), updatePlanEntity.getCrossId());
        assertEquals(updatePlanV2.getGeneralConditions(), updatePlanEntity.getGeneralConditions());
        assertEquals(new HashSet<>(updatePlanV2.getTags()), updatePlanEntity.getTags());
        assertEquals(updatePlanV2.getExcludedGroups(), updatePlanEntity.getExcludedGroups());
        assertEquals(updatePlanV2.getValidation().name(), updatePlanEntity.getValidation().name());
        assertEquals(updatePlanV2.getSelectionRule(), updatePlanEntity.getSelectionRule());

        assertDefinitionEquals(updatePlanV2.getSecurity().getConfiguration(), updatePlanEntity.getSecurityDefinition());

        assertEquals(updatePlanEntity.getFlows().size(), updatePlanV2.getFlows().size()); // Flow mapping is tested in FlowMapperTest
    }

    @Test
    void should_map_PlanWithFlows_to_PlanV4() {
        final var planWithFlows = PlanFixtures.aPlanWithFlows();
        final var plan = planMapper.map(planWithFlows);

        assertNotNull(plan.getId());
        assertEquals(planWithFlows.getId(), plan.getId());
        assertEquals(planWithFlows.getCrossId(), plan.getCrossId());
        assertEquals(planWithFlows.getName(), plan.getName());
        assertEquals(planWithFlows.getDescription(), plan.getDescription());
        assertEquals(planWithFlows.getValidation().name(), plan.getValidation().name());
        assertEquals(planWithFlows.getType().name(), plan.getType().name());
        assertEquals(planWithFlows.getPlanMode().name(), plan.getMode().name());

        assertNotNull(plan.getSecurity());
        assertSecurityV4Equals(planWithFlows.getPlanSecurity(), plan.getSecurity());

        assertEquals(planWithFlows.getPlanDefinitionV4().getSelectionRule(), plan.getSelectionRule());
        assertEquals(planWithFlows.getPlanDefinitionV4().getTags(), new HashSet<>(plan.getTags()));
        assertEquals(planWithFlows.getPlanStatus().name(), plan.getStatus().name());
        assertEquals(planWithFlows.getApiId(), plan.getApiId());
        assertEquals(planWithFlows.getOrder(), plan.getOrder());
        assertEquals(planWithFlows.getCharacteristics(), plan.getCharacteristics());
        assertEquals(planWithFlows.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(planWithFlows.getCommentMessage(), plan.getCommentMessage());
        assertEquals(planWithFlows.isCommentRequired(), plan.getCommentRequired());
        assertEquals(planWithFlows.getGeneralConditions(), plan.getGeneralConditions());

        assertEquals(planWithFlows.getFlows().size(), plan.getFlows().size());
    }

    @Test
    void should_map_UpdatePlanFederated_to_Plan() {
        final var source = PlanFixtures.anUpdatePlanFederated();
        final var target = planMapper.map(source);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(target.getName()).isEqualTo(source.getName());
            soft.assertThat(target.getDefinitionVersion()).isEqualTo(source.getDefinitionVersion());
            soft.assertThat(target.getDescription()).isEqualTo(source.getDescription());
            soft.assertThat(target.getCharacteristics()).isEqualTo(source.getCharacteristics());
            soft.assertThat(target.getCommentMessage()).isEqualTo(source.getCommentMessage());
            soft.assertThat(target.isCommentRequired()).isEqualTo(source.getCommentRequired());
            soft.assertThat(target.getExcludedGroups()).isEqualTo(source.getExcludedGroups());
            soft.assertThat(target.getGeneralConditions()).isEqualTo(source.getGeneralConditions());
            soft.assertThat(target.getOrder()).isEqualTo(source.getOrder());
            soft.assertThat(target.getValidation()).isEqualTo(Plan.PlanValidationType.AUTO);
            soft
                .assertThat(target.getFederatedPlanDefinition())
                .isEqualTo(
                    FederatedPlan
                        .builder()
                        .status(PlanStatus.PUBLISHED)
                        .mode(PlanMode.STANDARD)
                        .security(PlanSecurity.builder().configuration("{\"nice\": \"config\"}").build())
                        .build()
                );
        });
    }

    private void assertSecurityV4Equals(
        PlanSecurity planEntitySecurity,
        io.gravitee.rest.api.management.v2.rest.model.PlanSecurity planSecurity
    ) {
        assertEquals(PlanSecurityType.valueOfLabel(planEntitySecurity.getType()).name(), planSecurity.getType().getValue());
        assertDefinitionEquals(planEntitySecurity.getConfiguration(), planSecurity.getConfiguration());
    }

    private void assertDefinitionEquals(Object definition, Object other) {
        try {
            JsonNode definitionJsonNode = toJsonNode(definition);
            JsonNode otherJsonNode = toJsonNode(other);

            assertEquals(definitionJsonNode, otherJsonNode);
        } catch (Exception e) {
            AssertionFailureBuilder
                .assertionFailure()
                .message("Definitions are not equals")
                .expected(String.valueOf(definition))
                .actual(String.valueOf(other))
                .buildAndThrow();
        }
    }

    private JsonNode toJsonNode(Object definition) throws Exception {
        if (definition instanceof String) {
            return jsonMapper.readTree((String) definition);
        }

        if (!(definition instanceof JsonNode)) {
            return jsonMapper.valueToTree(definition);
        }

        return (JsonNode) definition;
    }
}
