/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.PlanFixtures;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.FlowV2;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import io.gravitee.rest.api.management.v2.rest.model.StepV2;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
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
        final var plan = planMapper.convert(planEntity);

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
        assertFlowsV4Equals(planEntity.getFlows(), plan.getFlows());
    }

    @Test
    void should_map_PlanEntity_to_PlanV2() {
        final var planEntity = PlanFixtures.aPlanEntityV2();
        final var plan = planMapper.convert(planEntity);

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

        assertFlowsV2Equals(planEntity.getFlows(), plan.getFlows());
    }

    @Test
    void should_map_CreatePlanV4_to_NewPlanEntity() {
        final var createPlanV4 = PlanFixtures.aCreatePlanV4();
        final var newPlanEntity = planMapper.convert(createPlanV4);

        Assertions.assertNull(newPlanEntity.getId());
        assertEquals(createPlanV4.getName(), newPlanEntity.getName());
        assertEquals(createPlanV4.getDescription(), newPlanEntity.getDescription());
        assertEquals(createPlanV4.getOrder(), newPlanEntity.getOrder());
        assertEquals(createPlanV4.getCharacteristics(), newPlanEntity.getCharacteristics());
        assertEquals(createPlanV4.getCommentMessage(), newPlanEntity.getCommentMessage());
        assertEquals(createPlanV4.getCrossId(), newPlanEntity.getCrossId());
        assertEquals(createPlanV4.getGeneralConditions(), newPlanEntity.getGeneralConditions());
        assertEquals(new HashSet<>(createPlanV4.getTags()), newPlanEntity.getTags());
        assertEquals(createPlanV4.getExcludedGroups(), newPlanEntity.getExcludedGroups());
        assertEquals(createPlanV4.getValidation().name(), newPlanEntity.getValidation().name());
        assertEquals(createPlanV4.getSelectionRule(), newPlanEntity.getSelectionRule());

        assertSecurityV4Equals(newPlanEntity.getSecurity(), createPlanV4.getSecurity());
        assertFlowsV4Equals(newPlanEntity.getFlows(), createPlanV4.getFlows());
    }

    @Test
    void should_map_UpdatePlanV4_to_UpdatePlanEntity() {
        final var updatePlanV4 = PlanFixtures.anUpdatePlanV4();
        final var updatePlanEntity = planMapper.convert(updatePlanV4);

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

        assertFlowsV4Equals(updatePlanEntity.getFlows(), updatePlanV4.getFlows());
    }

    @Test
    void should_map_CreatePlanV2_to_CreatePlanEntity() {
        final var createPlanV2 = PlanFixtures.aCreatePlanV2();
        final var createPlanEntity = planMapper.convert(createPlanV2);

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

        assertFlowsV2Equals(createPlanEntity.getFlows(), createPlanV2.getFlows());
    }

    @Test
    void should_map_UpdatePlanV2_to_UpdatePlanEntity() {
        final var updatePlanV2 = PlanFixtures.anUpdatePlanV2();
        final var updatePlanEntity = planMapper.convert(updatePlanV2);

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

        assertFlowsV2Equals(updatePlanEntity.getFlows(), updatePlanV2.getFlows());
    }

    private void assertFlowsV4Equals(List<Flow> planEntityFlows, List<FlowV4> planFlows) {
        assertEquals(planEntityFlows.size(), planFlows.size());

        for (int i = 0; i < planEntityFlows.size(); i++) {
            final var flow = planEntityFlows.get(i);
            final var flowV4 = planFlows.get(i);
            assertEquals(flow.getName(), flowV4.getName());

            final var flowSelectors = flow.getSelectors();
            final var flowV4Selectors = flowV4.getSelectors();

            assertFalse(flowSelectors.isEmpty());
            assertEquals(flowSelectors.size(), flowV4Selectors.size());

            for (int j = 0; j < flowSelectors.size(); j++) {
                final ChannelSelector selector = (ChannelSelector) flowSelectors.get(0);
                final io.gravitee.rest.api.management.v2.rest.model.ChannelSelector selectorV4 = flowV4Selectors
                    .get(0)
                    .getChannelSelector();

                assertEquals(selector.getChannel(), selectorV4.getChannel());
                assertEquals(selector.getChannelOperator().name(), selectorV4.getChannelOperator().name());
                assertEquals(selector.getEntrypoints(), selectorV4.getEntrypoints());
                assertEquals(selector.getType().name(), selectorV4.getType().name());

                assertEquals(
                    selector.getOperations().stream().map(Enum::name).collect(Collectors.toSet()),
                    selectorV4.getOperations().stream().map(Enum::name).collect(Collectors.toSet())
                );
            }

            assertStepsV4Equals(flow.getRequest(), flowV4.getRequest());
            assertStepsV4Equals(flow.getPublish(), flowV4.getPublish());
            assertStepsV4Equals(flow.getResponse(), flowV4.getResponse());
            assertStepsV4Equals(flow.getSubscribe(), flowV4.getSubscribe());
        }
    }

    private void assertFlowsV2Equals(List<io.gravitee.definition.model.flow.Flow> planEntityFlows, List<FlowV2> planFlows) {
        assertEquals(planEntityFlows.size(), planFlows.size());

        for (int i = 0; i < planEntityFlows.size(); i++) {
            final var flow = planEntityFlows.get(i);
            final var flowV2 = planFlows.get(i);
            assertEquals(flow.getName(), flowV2.getName());
            assertEquals(flow.getPath(), flowV2.getPathOperator().getPath());
            assertEquals(flow.getOperator().name(), flowV2.getPathOperator().getOperator().name());
            assertEquals(flow.getCondition(), flowV2.getCondition());
            assertStepsV2Equals(flow.getPre(), flowV2.getPre());
            assertStepsV2Equals(flow.getPost(), flowV2.getPost());
        }
    }

    private void assertStepsV4Equals(List<Step> steps, List<StepV4> stepsV4) {
        assertEquals(steps.size(), steps.size());

        for (int i = 0; i < steps.size(); i++) {
            final var step = steps.get(i);
            final var stepV4 = stepsV4.get(i);
            assertEquals(step.getName(), stepV4.getName());
            assertEquals(step.getDescription(), stepV4.getDescription());
            assertEquals(step.getPolicy(), stepV4.getPolicy());
            assertEquals(step.getCondition(), stepV4.getCondition());
            assertEquals(step.getMessageCondition(), stepV4.getMessageCondition());
            assertEquals(step.getConfiguration(), stepV4.getConfiguration());
        }
    }

    private void assertStepsV2Equals(List<io.gravitee.definition.model.flow.Step> steps, List<StepV2> stepsV2) {
        assertEquals(steps.size(), steps.size());

        for (int i = 0; i < steps.size(); i++) {
            final var step = steps.get(i);
            final var stepV2 = stepsV2.get(i);
            assertEquals(step.getName(), stepV2.getName());
            assertEquals(step.getDescription(), stepV2.getDescription());
            assertEquals(step.getPolicy(), stepV2.getPolicy());
            assertEquals(step.getCondition(), stepV2.getCondition());
            assertEquals(step.getConfiguration(), stepV2.getConfiguration());
        }
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
