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
package io.gravitee.rest.api.service.v4.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanMapperTest {

    private final PlanMapper planMapper = new PlanMapper();

    @Test
    public void shouldConvertPlanToPlanEntity() {
        Plan plan = new Plan();
        plan.setId("123123-1531-4563456166");
        plan.setName("Plan name");
        plan.setDescription("Description for the new plan");
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setType(Plan.PlanType.API);
        plan.setStatus(Plan.Status.STAGING);
        plan.setApi("api1");
        plan.setGeneralConditions("general_conditions");
        plan.setSecurity(Plan.PlanSecurityType.KEY_LESS);

        List<Flow> flows = new ArrayList<>();

        PlanEntity planEntity = planMapper.toEntity(plan, flows);

        assertEquals(plan.getId(), planEntity.getId());
        assertEquals(plan.getName(), planEntity.getName());
        assertEquals(plan.getDescription(), planEntity.getDescription());
        assertEquals(plan.getValidation().name(), planEntity.getValidation().name());
        assertEquals(plan.getStatus().name(), planEntity.getStatus().name());
        assertEquals(plan.getApi(), planEntity.getApiId());
        assertEquals(plan.getGeneralConditions(), planEntity.getGeneralConditions());
        assertEquals(PlanSecurityType.KEY_LESS.getLabel(), planEntity.getSecurity().getType());
        assertSame(flows, planEntity.getFlows());
    }

    @Test
    public void shouldConvertPlanEntityToUpdatePlanEntity() {
        final PlanEntity actual = buildTestPlanEntity();
        final UpdatePlanEntity result = planMapper.toUpdatePlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), actual.getValidation());
        assertEquals(result.getSecurity(), actual.getSecurity());
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
        actual.setFlows(null);
        final UpdatePlanEntity result = planMapper.toUpdatePlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), actual.getValidation());
        assertEquals(result.getSecurity(), actual.getSecurity());
        assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        assertEquals(result.getTags(), actual.getTags());
        assertEquals(result.getSelectionRule(), actual.getSelectionRule());
        assertEquals(result.getFlows(), actual.getFlows());
    }

    @Test
    public void shouldConvertPlanEntityToNewPlanEntity() {
        final PlanEntity actual = buildTestPlanEntity();
        final NewPlanEntity result = planMapper.toNewPlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getApiId(), actual.getApiId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), actual.getValidation());
        assertEquals(result.getSecurity(), actual.getSecurity());
        assertEquals(result.getSecurity(), actual.getSecurity());
        assertEquals(result.getType(), actual.getType());
        assertEquals(result.getStatus(), actual.getStatus());
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
    public void sshouldNotSetFieldsWithDefaultValueToNull() {
        final PlanEntity actual = buildTestPlanEntity();
        actual.setValidation(null);
        actual.setSecurity(null);
        actual.setType(null);
        actual.setStatus(null);
        actual.setFlows(null);
        final NewPlanEntity result = planMapper.toNewPlanEntity(actual);

        assertEquals(result.getId(), actual.getId());
        assertEquals(result.getApiId(), actual.getApiId());
        assertEquals(result.getName(), actual.getName());
        assertEquals(result.getDescription(), actual.getDescription());
        assertEquals(result.getValidation(), PlanValidationType.MANUAL);
        assertEquals(result.getSecurity(), actual.getSecurity());
        assertEquals(result.getType(), PlanType.API);
        assertEquals(result.getStatus(), PlanStatus.STAGING);
        assertEquals(result.getApiId(), actual.getApiId());
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

        NewPlanEntity newPlanEntity = planMapper.toNewPlanEntity(planEntity);

        assertEquals("test-cross-id", newPlanEntity.getCrossId());
    }

    @Test
    public void toNewPlanEntity_should_reset_crossId_if_param_set_to_true() {
        PlanEntity planEntity = buildTestPlanEntity();
        planEntity.setCrossId("test-cross-id");

        NewPlanEntity newPlanEntity = planMapper.toNewPlanEntity(planEntity, true);

        assertNull(newPlanEntity.getCrossId());
    }

    private PlanEntity buildTestPlanEntity() {
        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId("plan-id");
        planEntity.setApiId("api-id");
        planEntity.setName("plan-name");
        planEntity.setDescription("description");
        planEntity.setValidation(PlanValidationType.AUTO);
        PlanSecurity security = new PlanSecurity();
        security.setType(PlanSecurityType.JWT.getLabel());
        security.setConfiguration("definition");
        planEntity.setSecurity(security);
        planEntity.setType(PlanType.API);
        planEntity.setStatus(PlanStatus.STAGING);
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
