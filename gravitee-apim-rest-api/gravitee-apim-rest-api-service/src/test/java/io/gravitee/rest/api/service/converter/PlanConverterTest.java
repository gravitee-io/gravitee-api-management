package io.gravitee.rest.api.service.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlanConverterTest {

    @InjectMocks
    private PlanConverter planConverter;

    @Test
    public void toPlanEntity_should_convert_plan_to_plan_entity() {
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

        PlanEntity planEntity = planConverter.toPlanEntity(plan);

        assertEquals(plan.getId(), planEntity.getId());
        assertEquals(plan.getName(), planEntity.getName());
        assertEquals(plan.getDescription(), planEntity.getDescription());
        assertEquals(plan.getValidation().name(), planEntity.getValidation().name());
        assertEquals(plan.getStatus().name(), planEntity.getStatus().name());
        assertEquals(plan.getApi(), planEntity.getApi());
        assertEquals(plan.getGeneralConditions(), planEntity.getGeneralConditions());
        assertEquals(plan.getSecurity().name(), planEntity.getSecurity().name());
    }

    @Test
    public void toPlanEntity_should_get_flows_from_apiEntity_plans() {
        String planId = "my-test-plan";

        Plan plan = new Plan();
        plan.setId(planId);
        plan.setType(Plan.PlanType.API);
        plan.setValidation(Plan.PlanValidationType.AUTO);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());

        Flow flow1 = new Flow();
        Flow flow2 = new Flow();
        Flow flow3 = new Flow();

        io.gravitee.definition.model.Plan plan1 = new io.gravitee.definition.model.Plan();
        plan1.setId(planId);
        plan1.setFlows(List.of(flow1, flow3));

        io.gravitee.definition.model.Plan plan2 = new io.gravitee.definition.model.Plan();
        plan2.setId("another-plan-id");
        plan2.setFlows(List.of(flow2));

        apiEntity.getPlans().add(plan1);
        apiEntity.getPlans().add(plan2);

        PlanEntity planEntity = planConverter.toPlanEntity(plan, apiEntity);

        // should not contains flow 2 cause it belongs to another plan
        assertEquals(2, planEntity.getFlows().size());
        assertTrue(planEntity.getFlows().contains(flow1));
        assertTrue(planEntity.getFlows().contains(flow3));
    }

    @Test
    public void toUpdatePlanEntity_should_convert_to_UpdatePlanEntity() {
        final PlanEntity actual = buildTestPlanEntity();
        final UpdatePlanEntity result = planConverter.toUpdatePlanEntity(actual);

        Assert.assertEquals(result.getId(), actual.getId());
        Assert.assertEquals(result.getName(), actual.getName());
        Assert.assertEquals(result.getDescription(), actual.getDescription());
        Assert.assertEquals(result.getValidation(), actual.getValidation());
        Assert.assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        Assert.assertEquals(result.getPaths(), actual.getPaths());
        Assert.assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        Assert.assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        Assert.assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        Assert.assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        Assert.assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        Assert.assertEquals(result.getTags(), actual.getTags());
        Assert.assertEquals(result.getSelectionRule(), actual.getSelectionRule());
        Assert.assertSame(result.getFlows(), actual.getFlows());
    }

    @Test
    public void toUpdatePlanEntity_should_not_set_fields_with_default_value_to_null() {
        final PlanEntity actual = buildTestPlanEntity();
        actual.setPaths(null);
        final UpdatePlanEntity result = planConverter.toUpdatePlanEntity(actual);

        Assert.assertEquals(result.getId(), actual.getId());
        Assert.assertEquals(result.getName(), actual.getName());
        Assert.assertEquals(result.getDescription(), actual.getDescription());
        Assert.assertEquals(result.getValidation(), actual.getValidation());
        Assert.assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        Assert.assertEquals(result.getPaths(), new HashMap<>());
        Assert.assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        Assert.assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        Assert.assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        Assert.assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        Assert.assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        Assert.assertEquals(result.getTags(), actual.getTags());
        Assert.assertEquals(result.getSelectionRule(), actual.getSelectionRule());
    }

    private PlanEntity buildTestPlanEntity() {
        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId("plan-id");
        planEntity.setApi("api-id");
        planEntity.setName("plan-name");
        planEntity.setDescription("description");
        planEntity.setValidation(PlanValidationType.AUTO);
        planEntity.setSecurity(PlanSecurityType.JWT);
        planEntity.setSecurityDefinition("definition");
        planEntity.setType(PlanType.API);
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

    @Test
    public void toNewPlanEntity_should_convert_to_NewPlanEntity() {
        final PlanEntity actual = buildTestPlanEntity();
        final NewPlanEntity result = planConverter.toNewPlanEntity(actual);

        Assert.assertEquals(result.getId(), actual.getId());
        Assert.assertEquals(result.getApi(), actual.getApi());
        Assert.assertEquals(result.getName(), actual.getName());
        Assert.assertEquals(result.getDescription(), actual.getDescription());
        Assert.assertEquals(result.getValidation(), actual.getValidation());
        Assert.assertEquals(result.getSecurity(), actual.getSecurity());
        Assert.assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        Assert.assertEquals(result.getType(), actual.getType());
        Assert.assertEquals(result.getStatus(), actual.getStatus());
        Assert.assertEquals(result.getApi(), actual.getApi());
        Assert.assertEquals(result.getPaths(), actual.getPaths());
        Assert.assertEquals(result.getFlows(), actual.getFlows());
        Assert.assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        Assert.assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        Assert.assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        Assert.assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        Assert.assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        Assert.assertEquals(result.getTags(), actual.getTags());
        Assert.assertEquals(result.getSelectionRule(), actual.getSelectionRule());
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

        Assert.assertEquals(result.getId(), actual.getId());
        Assert.assertEquals(result.getApi(), actual.getApi());
        Assert.assertEquals(result.getName(), actual.getName());
        Assert.assertEquals(result.getDescription(), actual.getDescription());
        Assert.assertEquals(result.getValidation(), PlanValidationType.MANUAL);
        Assert.assertEquals(result.getSecurity(), PlanSecurityType.API_KEY);
        Assert.assertEquals(result.getSecurityDefinition(), actual.getSecurityDefinition());
        Assert.assertEquals(result.getType(), PlanType.API);
        Assert.assertEquals(result.getStatus(), PlanStatus.STAGING);
        Assert.assertEquals(result.getApi(), actual.getApi());
        Assert.assertEquals(result.getPaths(), new HashMap<>());
        Assert.assertEquals(result.getFlows(), new ArrayList<>());
        Assert.assertEquals(result.getCharacteristics(), actual.getCharacteristics());
        Assert.assertEquals(result.getExcludedGroups(), actual.getExcludedGroups());
        Assert.assertEquals(result.isCommentRequired(), actual.isCommentRequired());
        Assert.assertEquals(result.getCommentMessage(), actual.getCommentMessage());
        Assert.assertEquals(result.getGeneralConditions(), actual.getGeneralConditions());
        Assert.assertEquals(result.getTags(), actual.getTags());
        Assert.assertEquals(result.getSelectionRule(), actual.getSelectionRule());
    }
}
