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
package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlanConverterTest {

    private PlanConverter planConverter;

    @Before
    public void setUp() {
        planConverter = new PlanConverter(new ObjectMapper());
    }

    @Test
    public void toPlanEntity_should_convert_plan_to_plan_entity() {
        Plan plan = new Plan();
        plan.setId("123123-1531-4563456166");
        plan.setName("Plan name");
        plan.setDescription("Description for the new plan");
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setReferenceType(Plan.PlanReferenceType.API);
        plan.setStatus(Plan.Status.STAGING);
        plan.setReferenceId("api1");
        plan.setGeneralConditions("general_conditions");
        plan.setSecurity(Plan.PlanSecurityType.KEY_LESS);

        List<Flow> flows = new ArrayList<>();

        PlanEntity planEntity = planConverter.toPlanEntity(plan, flows);

        assertEquals(plan.getId(), planEntity.getId());
        assertEquals(plan.getName(), planEntity.getName());
        assertEquals(plan.getDescription(), planEntity.getDescription());
        assertEquals(plan.getValidation().name(), planEntity.getValidation().name());
        assertEquals(plan.getStatus().name(), planEntity.getStatus().name());
        assertEquals(plan.getReferenceId(), planEntity.getReferenceId());
        assertEquals(plan.getApi(), planEntity.getApi());
        assertEquals(plan.getGeneralConditions(), planEntity.getGeneralConditions());
        assertEquals(plan.getSecurity().name(), planEntity.getSecurity().name());
        assertSame(flows, planEntity.getFlows());
    }

    @Test
    public void toUpdatePlanEntity_should_convert_to_UpdatePlanEntity() {
        final PlanEntity actual = buildTestPlanEntity();
        final UpdatePlanEntity result = planConverter.toUpdatePlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), actual.getValidation());
        assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        assertEquals(result.getPaths(), actual.getPaths());
        assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        assertEquals(result.getTags(), actual.getTags());
        assertEquals(result.getSelectionRule(), actual.getSelectionRule());
        assertSame(result.getFlows(), actual.getFlows());
    }

    @Test
    public void toUpdatePlanEntity_should_not_set_fields_with_default_value_to_null() {
        final PlanEntity actual = buildTestPlanEntity();
        actual.setPaths(null);
        final UpdatePlanEntity result = planConverter.toUpdatePlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), actual.getValidation());
        assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        assertEquals(result.getPaths(), new HashMap<>());
        assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        assertEquals(result.getTags(), actual.getTags());
        assertEquals(result.getSelectionRule(), actual.getSelectionRule());
    }

    @Test
    public void toNewPlanEntity_should_convert_to_NewPlanEntity() {
        final PlanEntity actual = buildTestPlanEntity();
        final NewPlanEntity result = planConverter.toNewPlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), actual.getValidation());
        assertEquals(result.getSecurity(), actual.getSecurity());
        assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        assertEquals(result.getReferenceType(), actual.getReferenceType());
        assertEquals(result.getStatus(), actual.getStatus());
        assertEquals(result.getReferenceId(), actual.getReferenceId());
        assertEquals(result.getApi(), actual.getApi());
        assertEquals(result.getPaths(), actual.getPaths());
        assertEquals(result.getFlows(), actual.getFlows());
        assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        assertEquals(result.getTags(), actual.getTags());
        assertEquals(result.getSelectionRule(), actual.getSelectionRule());
    }

    @Test
    public void toNewPlanEntity_should_not_set_fields_with_default_value_to_null() {
        final PlanEntity actual = buildTestPlanEntity();
        actual.setValidation(null);
        actual.setSecurity(null);
        actual.setType(null);
        actual.setStatus(null);
        actual.setPaths(null);
        actual.setFlows(null);
        final NewPlanEntity result = planConverter.toNewPlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getReferenceId(), actual.getReferenceId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), PlanValidationType.MANUAL);
        assertEquals(result.getSecurity(), PlanSecurityType.API_KEY);
        assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        assertEquals(result.getReferenceType(), GenericPlanEntity.ReferenceType.API);
        assertEquals(result.getApi(), actual.getApi());
        assertEquals(result.getStatus(), PlanStatus.STAGING);
        assertEquals(result.getReferenceId(), actual.getReferenceId());
        assertEquals(result.getPaths(), new HashMap<>());
        assertEquals(result.getFlows(), new ArrayList<>());
        assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        assertEquals(result.getTags(), actual.getTags());
        assertEquals(result.getSelectionRule(), actual.getSelectionRule());
    }

    @Test
    public void toNewPlanEntity_should_keep_crossId() {
        PlanEntity planEntity = buildTestPlanEntity();
        planEntity.setCrossId("test-cross-id");

        NewPlanEntity newPlanEntity = planConverter.toNewPlanEntity(planEntity);

        assertEquals("test-cross-id", newPlanEntity.getCrossId());
    }

    @Test
    public void toNewPlanEntity_should_reset_crossId_if_param_set_to_true() {
        PlanEntity planEntity = buildTestPlanEntity();
        planEntity.setCrossId("test-cross-id");

        NewPlanEntity newPlanEntity = planConverter.toNewPlanEntity(planEntity, true);

        assertNull(newPlanEntity.getCrossId());
    }

    @Test
    public void toPlan_should_convert_NewPlanEntity_to_Plan_with_all_fields() throws Exception {
        NewPlanEntity newPlan = buildNewPlanEntity();
        newPlan.setStatus(PlanStatus.STAGING);
        newPlan.setSecurity(PlanSecurityType.API_KEY);
        newPlan.setValidation(PlanValidationType.MANUAL);

        Plan plan = planConverter.toPlan(newPlan, DefinitionVersion.V4);

        assertEquals(newPlan.getId(), plan.getId());
        assertEquals(newPlan.getCrossId(), plan.getCrossId());
        assertEquals(newPlan.getHrid(), plan.getHrid());
        assertEquals(newPlan.getReferenceId(), plan.getApi());
        assertEquals(newPlan.getReferenceId(), plan.getReferenceId());
        assertEquals(Plan.PlanReferenceType.API, plan.getReferenceType());
        assertEquals(newPlan.getName(), plan.getName());
        assertEquals(newPlan.getDescription(), plan.getDescription());
        assertNotNull(plan.getCreatedAt());
        assertEquals(plan.getCreatedAt(), plan.getUpdatedAt());
        assertEquals(plan.getCreatedAt(), plan.getNeedRedeployAt());
        assertEquals(Plan.PlanSecurityType.API_KEY, plan.getSecurity());
        assertEquals(newPlan.getSecurityDefinition(), plan.getSecurityDefinition());
        assertEquals(Plan.Status.STAGING, plan.getStatus());
        assertEquals(newPlan.getExcludedGroups(), plan.getExcludedGroups());
        assertEquals(newPlan.isCommentRequired(), plan.isCommentRequired());
        assertEquals(newPlan.getCommentMessage(), plan.getCommentMessage());
        assertEquals(newPlan.getTags(), plan.getTags());
        assertEquals(newPlan.getSelectionRule(), plan.getSelectionRule());
        assertEquals(newPlan.getGeneralConditions(), plan.getGeneralConditions());
        assertEquals(newPlan.getOrder(), plan.getOrder());
        assertEquals(Plan.PlanValidationType.MANUAL, plan.getValidation());
        assertEquals(newPlan.getCharacteristics(), plan.getCharacteristics());
        assertNull(plan.getPublishedAt());
        assertNotNull(plan.getDefinition());
    }

    @Test
    public void toPlan_should_set_publishedAt_when_status_is_PUBLISHED() throws Exception {
        NewPlanEntity newPlan = buildNewPlanEntity();
        newPlan.setStatus(PlanStatus.PUBLISHED);

        Plan plan = planConverter.toPlan(newPlan, DefinitionVersion.V4);

        assertEquals(Plan.Status.PUBLISHED, plan.getStatus());
        assertNotNull(plan.getPublishedAt());
    }

    @Test
    public void toPlan_should_force_validation_AUTO_when_security_is_KEY_LESS() throws Exception {
        NewPlanEntity newPlan = buildNewPlanEntity();
        newPlan.setSecurity(PlanSecurityType.KEY_LESS);
        newPlan.setValidation(PlanValidationType.MANUAL);

        Plan plan = planConverter.toPlan(newPlan, DefinitionVersion.V4);

        assertEquals(Plan.PlanSecurityType.KEY_LESS, plan.getSecurity());
        assertEquals(Plan.PlanValidationType.AUTO, plan.getValidation());
    }

    @Test
    public void toPlan_should_not_set_definition_when_definition_version_is_V2() throws Exception {
        NewPlanEntity newPlan = buildNewPlanEntity();

        Plan plan = planConverter.toPlan(newPlan, DefinitionVersion.V2);

        assertNull(plan.getDefinition());
    }

    private NewPlanEntity buildNewPlanEntity() {
        NewPlanEntity newPlan = new NewPlanEntity();
        newPlan.setId("plan-id-123");
        newPlan.setCrossId("cross-id-456");
        newPlan.setHrid("plan-hrid");
        newPlan.setReferenceId("api-ref-id");
        newPlan.setReferenceType(GenericPlanEntity.ReferenceType.API);
        newPlan.setName("Plan name");
        newPlan.setDescription("Plan description");
        newPlan.setSecurity(PlanSecurityType.API_KEY);
        newPlan.setSecurityDefinition("{\"prop\":\"value\"}");
        newPlan.setStatus(PlanStatus.STAGING);
        newPlan.setValidation(PlanValidationType.MANUAL);
        newPlan.setPaths(new HashMap<>());
        newPlan.setCharacteristics(new ArrayList<>());
        newPlan.setExcludedGroups(new ArrayList<>());
        newPlan.setCommentRequired(true);
        newPlan.setCommentMessage("Comment message");
        newPlan.setTags(new HashSet<>(Collections.singletonList("tag1")));
        newPlan.setSelectionRule("selection-rule");
        newPlan.setGeneralConditions("general-conditions-page-id");
        newPlan.setOrder(1);
        return newPlan;
    }

    private PlanEntity buildTestPlanEntity() {
        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId("plan-id");
        planEntity.setReferenceId("api-id");
        planEntity.setName("plan-name");
        planEntity.setDescription("description");
        planEntity.setValidation(PlanValidationType.AUTO);
        planEntity.setSecurity(PlanSecurityType.JWT);
        planEntity.setSecurityDefinition("definition");
        planEntity.setReferenceType(GenericPlanEntity.ReferenceType.API);
        planEntity.setStatus(PlanStatus.STAGING);
        planEntity.setPaths(new HashMap<>());
        planEntity.setFlows(new ArrayList<>());
        planEntity.setCharacteristics(new ArrayList<>());
        planEntity.setExcludedGroups(new ArrayList<>());
        planEntity.setCommentRequired(true);
        planEntity.setCommentMessage("comment-message");
        planEntity.setGeneralConditions("conditions");
        planEntity.setTags(new HashSet<>());
        planEntity.setSelectionRule("selection-rule");
        return planEntity;
    }
}
