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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Plan;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/plan-tests/";
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<Plan> plan = planRepository.findById("my-plan");

        assertNotNull(plan);
        assertTrue(plan.isPresent());
        assertEquals("my-plan", plan.get().getId());
        assertEquals("GCU-my-plan", plan.get().getGeneralConditions());
        assertEquals("Free plan", plan.get().getName());
        assertEquals("Description of the free plan", plan.get().getDescription());
        assertEquals("api1", plan.get().getApi());
        assertEquals(Plan.PlanSecurityType.API_KEY, plan.get().getSecurity());
        assertEquals(Plan.PlanValidationType.AUTO, plan.get().getValidation());
        assertEquals(Plan.PlanType.API, plan.get().getType());
        assertEquals(Plan.PlanMode.STANDARD, plan.get().getMode());
        assertEquals(ApiType.NATIVE, plan.get().getApiType());
        assertEquals(Plan.Status.PUBLISHED, plan.get().getStatus());
        assertEquals(2, plan.get().getOrder());
        assertTrue(compareDate(new Date(1506964899000L), plan.get().getCreatedAt()));
        assertTrue(compareDate(new Date(1507032062000L), plan.get().getUpdatedAt()));
        assertTrue(compareDate(new Date(1506878460000L), plan.get().getPublishedAt()));
        assertTrue(compareDate(new Date(1507611600000L), plan.get().getClosedAt()));
        assertTrue(compareDate(new Date(1507611670000L), plan.get().getNeedRedeployAt()));
        assertEquals(Arrays.asList("charac 1", "charac 2"), plan.get().getCharacteristics());
        assertEquals("grp1", plan.get().getExcludedGroups().get(0));
        assertEquals("tag1", plan.get().getTags().iterator().next());
        assertTrue(plan.get().isCommentRequired());
        assertEquals("What is your project code?", plan.get().getCommentMessage());
        assertNull(plan.get().getSelectionRule());
    }

    @Test
    public void shouldFindById_v4() throws Exception {
        final Optional<Plan> plan = planRepository.findById("plan-v4");

        assertThat(plan)
            .isPresent()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(
                Plan
                    .builder()
                    .id("plan-v4")
                    .definitionVersion(DefinitionVersion.V4)
                    .name("Free plan")
                    .description("Description of the free plan")
                    .api("api2")
                    .environmentId("DEFAULT")
                    .security(Plan.PlanSecurityType.API_KEY)
                    .validation(Plan.PlanValidationType.AUTO)
                    .type(Plan.PlanType.API)
                    .mode(Plan.PlanMode.STANDARD)
                    .apiType(ApiType.PROXY)
                    .status(Plan.Status.PUBLISHED)
                    .order(0)
                    .createdAt(new Date(1506964899000L))
                    .characteristics(List.of("charac_v4"))
                    .excludedGroups(List.of(("grp_v4")))
                    .tags(Set.of("tag_v4"))
                    .build()
            );
    }

    @Test
    public void shouldFindByIdIn() throws TechnicalException {
        Set<Plan> plans = planRepository.findByIdIn(List.of("my-plan", "unknown-id"));
        assertNotNull(plans);
        assertEquals(1, plans.size());

        Plan plan = new ArrayList<>(plans).get(0);

        assertEquals("my-plan", plan.getId());
        assertEquals("GCU-my-plan", plan.getGeneralConditions());
        assertEquals("Free plan", plan.getName());
        assertEquals("Description of the free plan", plan.getDescription());
        assertEquals("api1", plan.getApi());
        assertEquals(Plan.PlanSecurityType.API_KEY, plan.getSecurity());
        assertEquals(Plan.PlanValidationType.AUTO, plan.getValidation());
        assertEquals(Plan.PlanType.API, plan.getType());
        assertEquals(ApiType.NATIVE, plan.getApiType());
        assertEquals(Plan.Status.PUBLISHED, plan.getStatus());
        assertEquals(2, plan.getOrder());
        assertTrue(compareDate(new Date(1506964899000L), plan.getCreatedAt()));
        assertTrue(compareDate(new Date(1507032062000L), plan.getUpdatedAt()));
        assertTrue(compareDate(new Date(1506878460000L), plan.getPublishedAt()));
        assertTrue(compareDate(new Date(1507611600000L), plan.getClosedAt()));
        assertTrue(compareDate(new Date(1507611670000L), plan.getNeedRedeployAt()));
    }

    @Test
    public void shouldFindByIdIn_v4() throws TechnicalException {
        Set<Plan> plans = planRepository.findByIdIn(List.of("my-plan", "plan-v4"));
        assertThat(plans)
            .hasSize(2)
            .extracting(
                Plan::getId,
                Plan::getName,
                Plan::getDefinitionVersion,
                Plan::getApiType,
                Plan::getCharacteristics,
                Plan::getExcludedGroups,
                Plan::getTags
            )
            .contains(
                tuple(
                    "plan-v4",
                    "Free plan",
                    DefinitionVersion.V4,
                    ApiType.PROXY,
                    List.of("charac_v4"),
                    List.of("grp_v4"),
                    Set.of("tag_v4")
                ),
                tuple(
                    "my-plan",
                    "Free plan",
                    null,
                    ApiType.NATIVE,
                    List.of("charac 1", "charac 2"),
                    List.of("grp1"),
                    Set.of("tag1", "tag2")
                )
            );
    }

    @Test
    public void shouldFindByIdIn_andReturnEmptyListIfInputIsEmpty() throws TechnicalException {
        Set<Plan> plans = planRepository.findByIdIn(List.of());
        assertNotNull(plans);
        assertEquals(0, plans.size());
    }

    @Test
    public void shouldFindOAuth2PlanById() throws TechnicalException {
        final Optional<Plan> planOAuth2 = planRepository.findById("plan-oauth2");

        assertNotNull(planOAuth2);
        assertTrue(planOAuth2.isPresent());
        assertEquals("plan-oauth2", planOAuth2.get().getId());
        assertEquals("oauth2", planOAuth2.get().getName());
        assertEquals("Description of oauth2", planOAuth2.get().getDescription());
        assertEquals("4e0db366-f772-4489-8db3-66f772b48989", planOAuth2.get().getApi());
        assertEquals(Plan.PlanSecurityType.OAUTH2, planOAuth2.get().getSecurity());
        assertEquals(Plan.PlanValidationType.MANUAL, planOAuth2.get().getValidation());
        assertEquals(Plan.PlanType.API, planOAuth2.get().getType());
        assertEquals(Plan.PlanMode.PUSH, planOAuth2.get().getMode());
        assertEquals(ApiType.PROXY, planOAuth2.get().getApiType());
        assertEquals(Plan.Status.STAGING, planOAuth2.get().getStatus());
        assertEquals(0, planOAuth2.get().getOrder());
        assertTrue(compareDate("11/02/2016", planOAuth2.get().getCreatedAt()));
        assertTrue(compareDate("12/02/2016", planOAuth2.get().getUpdatedAt()));
        assertNull(planOAuth2.get().getPublishedAt());
        assertNull(planOAuth2.get().getClosedAt());
        assertEquals(emptyList(), planOAuth2.get().getCharacteristics());
        assertEquals("7c546c6b-2f2f-4487-946c-6b2f2f648784", planOAuth2.get().getExcludedGroups().get(0));
        assertEquals(
            "{\"extractPayload\":false,\"checkRequiredScopes\":false,\"requiredScopes\":[],\"oauthResource\":\"OAuth\"}",
            planOAuth2.get().getSecurityDefinition()
        );
        assertEquals(
            "{  \"/\" : [ {    \"methods\" : [ \"GET\", \"POST\", \"PUT\", \"DELETE\", \"HEAD\", \"PATCH\", \"OPTIONS\", \"TRACE\", \"CONNECT\" ],    \"resource-filtering\" : {\"whitelist\":[{\"pattern\":\"/**\",\"methods\":[\"GET\"]}]},    \"enabled\" : true  } ]}",
            planOAuth2.get().getDefinition()
        );
        assertEquals("#context.attributes['jwt'].claims['iss'] == 'toto'", planOAuth2.get().getSelectionRule());
    }

    @Test
    public void shouldNotFindById() throws TechnicalException {
        Optional<Plan> unknown = planRepository.findById("unknown");
        assertNotNull(unknown);
        assertFalse(unknown.isPresent());
    }

    @Test
    public void shouldFindByApi() throws Exception {
        final Set<Plan> plans = planRepository.findByApi("api1");

        assertNotNull(plans);
        assertEquals(2, plans.size());
        assertEquals(2, plans.size());
    }

    @Test
    public void shouldFindByApi_v4() throws Exception {
        final Set<Plan> plans = planRepository.findByApi("api2");

        assertThat(plans)
            .hasSize(1)
            .extracting(
                Plan::getId,
                Plan::getName,
                Plan::getDefinitionVersion,
                Plan::getApiType,
                Plan::getCharacteristics,
                Plan::getExcludedGroups,
                Plan::getTags
            )
            .contains(
                tuple(
                    "plan-v4",
                    "Free plan",
                    DefinitionVersion.V4,
                    ApiType.PROXY,
                    List.of("charac_v4"),
                    List.of("grp_v4"),
                    Set.of("tag_v4")
                )
            );
    }

    @Test
    public void findByApisAndEnvironments() throws Exception {
        final List<Plan> plans = planRepository.findByApisAndEnvironments(Arrays.asList("api1", "api2"), Set.of("DEFAULT"));

        assertThat(plans)
            .hasSize(3)
            .extracting(
                Plan::getId,
                Plan::getName,
                Plan::getDefinitionVersion,
                Plan::getApiType,
                Plan::getCharacteristics,
                Plan::getExcludedGroups,
                Plan::getTags
            )
            .contains(
                tuple(
                    "my-plan",
                    "Free plan",
                    null,
                    ApiType.NATIVE,
                    List.of("charac 1", "charac 2"),
                    List.of("grp1"),
                    Set.of("tag1", "tag2")
                ),
                tuple("products", "Products", null, ApiType.MESSAGE, emptyList(), emptyList(), emptySet()),
                tuple(
                    "plan-v4",
                    "Free plan",
                    DefinitionVersion.V4,
                    ApiType.PROXY,
                    List.of("charac_v4"),
                    List.of("grp_v4"),
                    Set.of("tag_v4")
                )
            );
    }

    @Test
    public void shouldFindByApis_andReturnEmptyListIfInputIsEmpty() throws Exception {
        final List<Plan> plans = planRepository.findByApisAndEnvironments(List.of(), Set.of("DEFAULT"));
        assertNotNull(plans);
        assertEquals(0, plans.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        String planName = "new-plan";

        final Plan plan = new Plan();
        plan.setId(planName);
        plan.setName("Plan name");
        plan.setDescription("Description for the new plan");
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setType(Plan.PlanType.API);
        plan.setStatus(Plan.Status.STAGING);
        plan.setApi("api1");
        plan.setEnvironmentId("DEFAULT");
        plan.setGeneralConditions("general_conditions");
        plan.setCreatedAt(parse("11/02/2016"));
        plan.setUpdatedAt(parse("12/02/2016"));
        plan.setPublishedAt(parse("13/02/2016"));
        plan.setClosedAt(parse("14/02/2016"));
        plan.setSecurity(Plan.PlanSecurityType.KEY_LESS);
        plan.setApiType(ApiType.PROXY);

        planRepository.create(plan);

        Optional<Plan> optional = planRepository.findById(planName);
        Assert.assertTrue("New plan not found", optional.isPresent());

        final Plan createdPlan = optional.get();
        Assert.assertEquals("Invalid plan name.", plan.getName(), createdPlan.getName());
        Assert.assertEquals("Invalid GeneralCondition.", plan.getGeneralConditions(), createdPlan.getGeneralConditions());
        Assert.assertEquals("Invalid plan description.", plan.getDescription(), createdPlan.getDescription());
        Assert.assertEquals("Invalid plan validation.", plan.getValidation(), createdPlan.getValidation());
        Assert.assertEquals("Invalid plan type.", plan.getType(), createdPlan.getType());
        Assert.assertEquals("Invalid plan API.", plan.getApi(), createdPlan.getApi());
        Assert.assertTrue("Invalid plan created date.", compareDate(plan.getCreatedAt(), createdPlan.getCreatedAt()));
        Assert.assertTrue("Invalid plan updated date.", compareDate(plan.getUpdatedAt(), createdPlan.getUpdatedAt()));
        Assert.assertEquals("Invalid plan status.", plan.getStatus(), createdPlan.getStatus());
        Assert.assertTrue("Invalid plan published date.", compareDate(plan.getPublishedAt(), createdPlan.getPublishedAt()));
        Assert.assertTrue("Invalid plan closed date.", compareDate(plan.getClosedAt(), createdPlan.getClosedAt()));
        Assert.assertEquals("Invalid plan security.", plan.getSecurity(), createdPlan.getSecurity());
        Assert.assertEquals("Invalid plan apiType.", plan.getApiType(), createdPlan.getApiType());
    }

    @Test
    public void should_create_v4() throws Exception {
        var toCreate = Plan
            .builder()
            .id("a-plan-v4")
            .definitionVersion(DefinitionVersion.V4)
            .name("Plan V4")
            .description("Description of the V4 plan")
            .api("api2")
            .environmentId("DEFAULT")
            .security(Plan.PlanSecurityType.API_KEY)
            .validation(Plan.PlanValidationType.AUTO)
            .type(Plan.PlanType.API)
            .mode(Plan.PlanMode.STANDARD)
            .apiType(ApiType.NATIVE)
            .status(Plan.Status.PUBLISHED)
            .order(0)
            .createdAt(new Date(1506964899000L))
            .characteristics(emptyList())
            .excludedGroups(emptyList())
            .build();

        var created = planRepository.create(toCreate);

        assertThat(created).usingRecursiveComparison().isEqualTo(toCreate);
    }

    @Test
    public void shouldCreateOAuth2Plan() throws Exception {
        String planName = "new-oauth2-plan";

        final Plan plan = new Plan();
        plan.setId(planName);
        plan.setName("Plan oauth2 name");
        plan.setDescription("Description for the new oauth2 plan");
        plan.setValidation(Plan.PlanValidationType.AUTO);
        plan.setType(Plan.PlanType.API);
        plan.setStatus(Plan.Status.STAGING);
        plan.setApi("my-api");
        plan.setEnvironmentId("DEFAULT");
        plan.setCreatedAt(parse("11/02/2016"));
        plan.setUpdatedAt(parse("12/02/2016"));
        plan.setPublishedAt(parse("13/02/2016"));
        plan.setClosedAt(parse("14/02/2016"));
        plan.setSecurity(Plan.PlanSecurityType.OAUTH2);
        plan.setSecurityDefinition(
            "{\"extractPayload\":false,\"checkRequiredScopes\":false,\"requiredScopes\":[],\"oauthResource\":\"OAuth\"}"
        );
        plan.setCommentRequired(true);
        plan.setApiType(ApiType.NATIVE);

        planRepository.create(plan);

        Optional<Plan> optional = planRepository.findById(planName);
        Assert.assertTrue("New oauth2 plan not found", optional != null && optional.isPresent());

        final Plan createdPlan = optional.get();
        Assert.assertEquals("Invalid oauth2 plan name.", plan.getName(), createdPlan.getName());
        Assert.assertEquals("Invalid oauth2 plan description.", plan.getDescription(), createdPlan.getDescription());
        Assert.assertEquals("Invalid oauth2 plan validation.", plan.getValidation(), createdPlan.getValidation());
        Assert.assertEquals("Invalid oauth2 plan type.", plan.getType(), createdPlan.getType());
        Assert.assertEquals("Invalid oauth2 plan API.", plan.getApi(), createdPlan.getApi());
        Assert.assertTrue("Invalid oauth2 plan created date.", compareDate(plan.getCreatedAt(), createdPlan.getCreatedAt()));
        Assert.assertTrue("Invalid oauth2 plan updated date.", compareDate(plan.getUpdatedAt(), createdPlan.getUpdatedAt()));
        Assert.assertEquals("Invalid oauth2 plan status.", plan.getStatus(), createdPlan.getStatus());
        Assert.assertTrue("Invalid oauth2 plan published date.", compareDate(plan.getPublishedAt(), createdPlan.getPublishedAt()));
        Assert.assertTrue("Invalid oauth2 plan closed date.", compareDate(plan.getClosedAt(), createdPlan.getClosedAt()));
        Assert.assertEquals("Invalid oauth2 plan security.", plan.getSecurity(), createdPlan.getSecurity());
        Assert.assertEquals("Invalid oauth2 plan security definition.", plan.getSecurityDefinition(), createdPlan.getSecurityDefinition());
        Assert.assertEquals("Invalid oauth2 plan comment required.", plan.isCommentRequired(), createdPlan.isCommentRequired());
        Assert.assertEquals("Invalid oauth2 plan apiType.", plan.getApiType(), createdPlan.getApiType());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Plan> optional = planRepository.findById("updated-plan");
        Assert.assertTrue("Plan to update not found", optional != null && optional.isPresent());

        final Plan plan = optional.get();
        plan.setName("New plan");
        plan.setDescription("New description");
        plan.setGeneralConditions("New GCU");
        plan.setStatus(Plan.Status.CLOSED);
        plan.setTags(Collections.singleton("tag1"));
        plan.setApiType(ApiType.MESSAGE);

        planRepository.update(plan);

        Optional<Plan> optionalUpdated = planRepository.findById("updated-plan");
        Assert.assertTrue("Plan to update not found", optionalUpdated != null && optionalUpdated.isPresent());

        final Plan planUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved plan name.", plan.getName(), planUpdated.getName());
        Assert.assertEquals("Invalid saved general conditions.", plan.getGeneralConditions(), planUpdated.getGeneralConditions());
        Assert.assertEquals("Invalid plan description.", plan.getDescription(), planUpdated.getDescription());
        Assert.assertEquals("Invalid plan status.", plan.getStatus(), planUpdated.getStatus());
        Assert.assertEquals("Invalid plan tags.", plan.getTags().size(), planUpdated.getTags().size());
        Assert.assertEquals("Invalid plan apiType.", plan.getApiType(), planUpdated.getApiType());
    }

    @Test
    public void shouldUpdateOAuth2Plan() throws Exception {
        Optional<Plan> optional = planRepository.findById("plan-oauth2");
        Assert.assertTrue("Plan to update not found", optional != null && optional.isPresent());

        final Plan plan = optional.get();
        plan.setName("New oauth2 plan");
        plan.setDescription("New oauth2 description");
        plan.setStatus(Plan.Status.CLOSED);
        plan.setSecurityDefinition("{}");
        plan.setApiType(ApiType.PROXY);

        planRepository.update(plan);

        Optional<Plan> optionalUpdated = planRepository.findById("plan-oauth2");
        Assert.assertTrue("Plan to update not found", optionalUpdated != null && optionalUpdated.isPresent());

        final Plan planUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved plan name.", plan.getName(), planUpdated.getName());
        Assert.assertEquals("Invalid plan description.", plan.getDescription(), planUpdated.getDescription());
        Assert.assertEquals("Invalid plan status.", plan.getStatus(), planUpdated.getStatus());
        Assert.assertEquals("Invalid plan security definition.", plan.getSecurityDefinition(), planUpdated.getSecurityDefinition());
        Assert.assertEquals("Invalid plan apiType.", plan.getApiType(), planUpdated.getApiType());
    }

    @Test
    public void shouldDelete() throws Exception {
        planRepository.delete("stores");

        Optional<Plan> optional = planRepository.findById("stores");

        Assert.assertFalse("Plan must not be found", optional.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownPlan() throws Exception {
        Plan unknownPlan = new Plan();
        unknownPlan.setId("unknown");
        planRepository.update(unknownPlan);
        fail("An unknown plan should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        planRepository.update(null);
        fail("A null plan should not be updated");
    }

    @Test
    public void should_delete_by_environment_id() throws Exception {
        List<String> deleted = planRepository.deleteByEnvironmentId("ToBeDeleted");

        assertThat(deleted).containsOnly("plan-deleted-1", "plan-deleted-2");
        assertEquals(0, planRepository.findAll().stream().filter(plan -> "ToBeDeleted".equals(plan.getEnvironmentId())).count());
    }
}
