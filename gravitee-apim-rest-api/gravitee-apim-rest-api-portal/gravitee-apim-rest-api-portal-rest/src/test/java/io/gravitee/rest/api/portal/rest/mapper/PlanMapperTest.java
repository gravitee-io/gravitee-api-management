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

import static org.junit.Assert.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.PeriodTimeUnit;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.Plan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.Plan.ValidationEnum;
import io.gravitee.rest.api.portal.rest.model.PlanMode;
import java.time.Instant;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanMapperTest {

    private static final String PLAN_API = "my-plan-api";
    private static final String PLAN_ID = "my-plan-id";
    private static final String PLAN_CHARACTERISTIC = "my-plan-characteristic";
    private static final String PLAN_COMMENT_MESSAGE = "my-plan-comment-message";
    private static final String PLAN_DESCRIPTION = "my-plan-description";
    private static final String PLAN_GROUP = "my-plan-group";
    private static final String PLAN_NAME = "my-plan-name";
    private static final String PLAN_RULE_POLICY_CONFIGURATION = "my-plan-rule-policy-configuration";
    private static final String PLAN_RULE_POLICY_NAME = "my-plan-rule-policy-name";
    private static final String PLAN_RULE_DESCRIPTION = "my-plan-rule-description";
    private static final String PLAN_PATH = "my-plan-path";
    private static final String PLAN_SECURITY_DEFINITINON = "my-plan-security-definition";
    private static final String PLAN_SELECTION_RULE = "my-plan-selection-rule";
    private static final String PLAN_TAG = "my-plan-tag";
    private static final String RATE_LIMIT = "rate-limit";
    private static final String QUOTA = "quota";

    private PlanEntity planEntityV2;
    private io.gravitee.rest.api.model.v4.plan.PlanEntity planEntityV4;

    private final ApiEntity aV4Api = ApiEntity
        .builder()
        .definitionVersion(DefinitionVersion.V4)
        .properties(List.of(new Property("ratelimit", "25")))
        .build();
    private final io.gravitee.rest.api.model.api.ApiEntity aV2Api = io.gravitee.rest.api.model.api.ApiEntity
        .builder()
        .properties(Properties.builder().propertiesList(List.of(new io.gravitee.definition.model.Property("ratelimit", "25"))).build())
        .build();
    private final Instant now = Instant.now();
    private final Date nowDate = Date.from(now);

    private final PlanMapper planMapper = new PlanMapper();

    @Before
    public void init() {
        this.preparePlanEntityV2();
        this.preparePlanEntityV4();
    }

    @Test
    public void testConvertWithSubscriptionsV2() {
        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);

        List<String> characteristics = responsePlan.getCharacteristics();
        assertNotNull(characteristics);
        assertEquals(1, characteristics.size());
        assertEquals(PLAN_CHARACTERISTIC, characteristics.get(0));
        assertEquals(PLAN_COMMENT_MESSAGE, responsePlan.getCommentQuestion());
        assertTrue(responsePlan.getCommentRequired());
        assertEquals(PLAN_DESCRIPTION, responsePlan.getDescription());
        assertEquals(PLAN_ID, responsePlan.getId());
        assertEquals(PLAN_NAME, responsePlan.getName());
        assertEquals(1, responsePlan.getOrder().intValue());
        assertEquals(SecurityEnum.API_KEY, responsePlan.getSecurity());
        assertEquals(ValidationEnum.AUTO, responsePlan.getValidation());
        assertEquals(PlanMode.STANDARD, responsePlan.getMode());
    }

    @Test
    public void testConvertWithSubscriptionsV4() {
        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);

        List<String> characteristics = responsePlan.getCharacteristics();
        assertNotNull(characteristics);
        assertEquals(1, characteristics.size());
        assertEquals(PLAN_CHARACTERISTIC, characteristics.get(0));
        assertEquals(PLAN_COMMENT_MESSAGE, responsePlan.getCommentQuestion());
        assertTrue(responsePlan.getCommentRequired());
        assertEquals(PLAN_DESCRIPTION, responsePlan.getDescription());
        assertEquals(PLAN_ID, responsePlan.getId());
        assertEquals(PLAN_NAME, responsePlan.getName());
        assertEquals(1, responsePlan.getOrder().intValue());
        assertEquals(SecurityEnum.API_KEY, responsePlan.getSecurity());
        assertEquals(ValidationEnum.AUTO, responsePlan.getValidation());
        assertEquals(PlanMode.STANDARD, responsePlan.getMode());
    }

    @Test
    public void testConvertPushPlan() {
        planEntityV2.setSecurity(null);
        planEntityV2.setSecurityDefinition(null);
        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);

        assertNull(responsePlan.getSecurity());
    }

    @Test
    public void shouldMapNoRateLimitOrQuotaV2() {
        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        assertNull(responsePlan.getUsageConfiguration().getRateLimit());
        assertNull(responsePlan.getUsageConfiguration().getQuota());
    }

    @Test
    public void shouldMapNoRateLimitOrQuotaV4() {
        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        assertNull(responsePlan.getUsageConfiguration().getRateLimit());
        assertNull(responsePlan.getUsageConfiguration().getQuota());
    }

    @Test
    public void shouldMapELDynamicRateLimitV2() {
        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        assertNull(responsePlan.getUsageConfiguration().getRateLimit());
        assertNull(responsePlan.getUsageConfiguration().getQuota());
    }

    @Test
    public void shouldMapELDynamicRateLimitV4() {
        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        assertNull(responsePlan.getUsageConfiguration().getRateLimit());
        assertNull(responsePlan.getUsageConfiguration().getQuota());
    }

    @Test
    public void shouldMapMultipleRateLimitsToOneV2() {
        var stepNoLimit = new Step();
        stepNoLimit.setPolicy(RATE_LIMIT);
        stepNoLimit.setConfiguration("{ \"rate\": { \"periodTime\": 2, \"periodTimeUnit\": \"MONTHS\" } }");
        stepNoLimit.setEnabled(true);

        var stepDynamicLimitMostRestrictive = new Step();
        stepDynamicLimitMostRestrictive.setPolicy(RATE_LIMIT);
        stepDynamicLimitMostRestrictive.setConfiguration(
            "{ \"rate\": { \"dynamicLimit\": 25, \"periodTime\": 4, \"periodTimeUnit\": \"HOURS\" } }"
        );
        stepDynamicLimitMostRestrictive.setEnabled(true);

        var stepLowLimit = new Step();
        stepLowLimit.setPolicy(RATE_LIMIT);
        stepLowLimit.setConfiguration(
            "{ \"rate\": { \"limit\": 2, \"dynamicLimit\": 25, \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" }}"
        );
        stepLowLimit.setEnabled(true);

        var stepDisabledLowestLimit = new Step();
        stepDisabledLowestLimit.setPolicy(RATE_LIMIT);
        stepDisabledLowestLimit.setConfiguration("{ \"rate\": { \"limit\": 1, \"periodTime\": 8, \"periodTimeUnit\": \"DAYS\" } }");
        stepDisabledLowestLimit.setEnabled(false);

        var flow1 = new Flow();
        flow1.setPre(List.of(stepNoLimit, stepDynamicLimitMostRestrictive));

        var flow2 = new Flow();
        flow2.setPre(List.of(stepLowLimit, stepDisabledLowestLimit));

        planEntityV2.setFlows(List.of(flow1, flow2));
        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimitConfig = responsePlan.getUsageConfiguration().getRateLimit();
        assertNotNull(rateLimitConfig);
        assertEquals(25L, rateLimitConfig.getLimit());
        assertEquals(4, rateLimitConfig.getPeriodTime());
        assertEquals(PeriodTimeUnit.HOURS, rateLimitConfig.getPeriodTimeUnit());
    }

    @Test
    public void shouldMapMultipleRateLimitsToOneV4() {
        var stepNoLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepNoLimit.setPolicy(RATE_LIMIT);
        stepNoLimit.setConfiguration("{ \"rate\": { \"periodTime\": 2, \"periodTimeUnit\": \"MONTHS\" } }");
        stepNoLimit.setEnabled(true);

        var stepEnabledLongestDuration = new io.gravitee.definition.model.v4.flow.step.Step();
        stepEnabledLongestDuration.setPolicy(RATE_LIMIT);
        stepEnabledLongestDuration.setConfiguration(
            "{ \"rate\": { \"dynamicLimit\": 25, \"periodTime\": 4, \"periodTimeUnit\": \"HOURS\" } }"
        );
        stepEnabledLongestDuration.setEnabled(true);

        var stepLowLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepLowLimit.setPolicy(RATE_LIMIT);
        stepLowLimit.setConfiguration(
            "{ \"rate\": { \"limit\": 2, \"dynamicLimit\": 25, \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" } }"
        );
        stepLowLimit.setEnabled(true);

        var stepDisabledLowestLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepDisabledLowestLimit.setPolicy(RATE_LIMIT);
        stepDisabledLowestLimit.setConfiguration("{ \"rate\": { \"limit\": 1, \"periodTime\": 8, \"periodTimeUnit\": \"DAYS\" } }");
        stepDisabledLowestLimit.setEnabled(false);

        var flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setRequest(List.of(stepNoLimit, stepEnabledLongestDuration));
        flow1.setEnabled(true);

        var disabledFlow = new io.gravitee.definition.model.v4.flow.Flow();
        disabledFlow.setRequest(List.of(stepLowLimit, stepDisabledLowestLimit));
        disabledFlow.setEnabled(false);

        planEntityV4.setFlows(List.of(flow1, disabledFlow));

        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimitConfig = responsePlan.getUsageConfiguration().getRateLimit();
        assertNotNull(rateLimitConfig);
        assertEquals(25L, rateLimitConfig.getLimit());
        assertEquals(4, rateLimitConfig.getPeriodTime());
        assertEquals(PeriodTimeUnit.HOURS, rateLimitConfig.getPeriodTimeUnit());
    }

    @Test
    public void shouldMapMultipleQuotaToOneV2() {
        var stepNoLimit = new Step();
        stepNoLimit.setPolicy(QUOTA);
        stepNoLimit.setConfiguration("{ \"quota\": { \"periodTime\": 2, \"periodTimeUnit\": \"MONTHS\" } }");
        stepNoLimit.setEnabled(true);

        var stepDynamicLimitMostRestrictive = new Step();
        stepDynamicLimitMostRestrictive.setPolicy(QUOTA);
        stepDynamicLimitMostRestrictive.setConfiguration(
            "{ \"quota\": { \"dynamicLimit\": 25, \"periodTime\": 4, \"periodTimeUnit\": \"HOURS\" } }"
        );
        stepDynamicLimitMostRestrictive.setEnabled(true);

        var stepLowLimit = new Step();
        stepLowLimit.setPolicy(QUOTA);
        stepLowLimit.setConfiguration(
            "{ \"quota\": { \"limit\": 2, \"dynamicLimit\": 25, \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" } }"
        );
        stepLowLimit.setEnabled(true);

        var stepDisabledLowestLimit = new Step();
        stepDisabledLowestLimit.setPolicy(QUOTA);
        stepDisabledLowestLimit.setConfiguration("{ \"quota\": { \"limit\": 1, \"periodTime\": 8, \"periodTimeUnit\": \"DAYS\" } }");
        stepDisabledLowestLimit.setEnabled(false);

        var flow1 = new Flow();
        flow1.setPre(List.of(stepNoLimit, stepDynamicLimitMostRestrictive));

        var flow2 = new Flow();
        flow2.setPre(List.of(stepLowLimit, stepDisabledLowestLimit));

        planEntityV2.setFlows(List.of(flow1, flow2));

        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var quotaConfig = responsePlan.getUsageConfiguration().getQuota();
        assertNotNull(quotaConfig);
        assertEquals(25L, quotaConfig.getLimit());
        assertEquals(4, quotaConfig.getPeriodTime());
        assertEquals(PeriodTimeUnit.HOURS, quotaConfig.getPeriodTimeUnit());
    }

    @Test
    public void shouldMapMultipleQuotaToOneV4() {
        var stepNoLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepNoLimit.setPolicy(QUOTA);
        stepNoLimit.setConfiguration("{ \"quota\": { \"periodTime\": 2, \"periodTimeUnit\": \"MONTHS\" } }");
        stepNoLimit.setEnabled(true);

        var stepDynamicLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepDynamicLimit.setPolicy(QUOTA);
        stepDynamicLimit.setConfiguration("{ \"quota\": { \"dynamicLimit\": \"25\", \"periodTime\": 4, \"periodTimeUnit\": \"HOURS\" } }");
        stepDynamicLimit.setEnabled(true);

        var stepLowLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepLowLimit.setPolicy(QUOTA);
        stepLowLimit.setConfiguration(
            "{ \"quota\": { \"limit\": 2, \"dynamicLimit\": 25, \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" } }"
        );
        stepLowLimit.setEnabled(true);

        var stepDisabledLowestLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepDisabledLowestLimit.setPolicy(QUOTA);
        stepDisabledLowestLimit.setConfiguration("{ \"quota\": { \"limit\": 1, \"periodTime\": 8, \"periodTimeUnit\": \"DAYS\" } }");
        stepDisabledLowestLimit.setEnabled(false);

        var flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setRequest(List.of(stepNoLimit, stepDynamicLimit));
        flow1.setEnabled(true);

        var disabledFlow = new io.gravitee.definition.model.v4.flow.Flow();
        disabledFlow.setRequest(List.of(stepLowLimit, stepDisabledLowestLimit));
        disabledFlow.setEnabled(false);

        planEntityV4.setFlows(List.of(flow1, disabledFlow));

        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var quotaConfig = responsePlan.getUsageConfiguration().getQuota();
        assertNotNull(quotaConfig);
        assertEquals(25L, quotaConfig.getLimit());
        assertEquals(4, quotaConfig.getPeriodTime());
        assertEquals(PeriodTimeUnit.HOURS, quotaConfig.getPeriodTimeUnit());
    }

    @Test
    public void shouldHandleStringDynamicLimit() {
        var stepNoLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepNoLimit.setPolicy(QUOTA);
        stepNoLimit.setConfiguration("{ \"quota\": { \"periodTime\": 2, \"periodTimeUnit\": \"MONTHS\" } }");
        stepNoLimit.setEnabled(true);

        var stepDynamicLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepDynamicLimit.setPolicy(QUOTA);
        stepDynamicLimit.setConfiguration("{ \"quota\": { \"dynamicLimit\": \"25\", \"periodTime\": 4, \"periodTimeUnit\": \"HOURS\" } }");
        stepDynamicLimit.setEnabled(true);

        var stepLowLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepLowLimit.setPolicy(QUOTA);
        stepLowLimit.setConfiguration(
            "{ \"quota\": { \"limit\": 2, \"dynamicLimit\": \"25\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" } }"
        );
        stepLowLimit.setEnabled(true);

        var stepDisabledLowestLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepDisabledLowestLimit.setPolicy(QUOTA);
        stepDisabledLowestLimit.setConfiguration("{ \"quota\": { \"limit\": 1, \"periodTime\": 8, \"periodTimeUnit\": \"DAYS\" } }");
        stepDisabledLowestLimit.setEnabled(false);

        var flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setRequest(List.of(stepNoLimit, stepDynamicLimit));
        flow1.setEnabled(true);

        var disabledFlow = new io.gravitee.definition.model.v4.flow.Flow();
        disabledFlow.setRequest(List.of(stepLowLimit, stepDisabledLowestLimit));
        disabledFlow.setEnabled(false);

        planEntityV4.setFlows(List.of(flow1, disabledFlow));

        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var quotaConfig = responsePlan.getUsageConfiguration().getQuota();
        assertNotNull(quotaConfig);
        assertEquals(25L, quotaConfig.getLimit());
        assertEquals(4, quotaConfig.getPeriodTime());
        assertEquals(PeriodTimeUnit.HOURS, quotaConfig.getPeriodTimeUnit());
    }

    @Test
    public void shouldHandleELDynamicLimitForApiPropertiesV4() {
        var stepNoLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepNoLimit.setPolicy(RATE_LIMIT);
        stepNoLimit.setConfiguration(
            "{ \"rate\": {  \"dynamicLimit\": \"{#api.properties['ratelimit']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" } }"
        );
        stepNoLimit.setEnabled(true);

        var flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setRequest(List.of(stepNoLimit));
        flow1.setEnabled(true);

        planEntityV4.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNotNull(rateLimit);
        assertEquals(25L, rateLimit.getLimit());
        assertEquals(6, rateLimit.getPeriodTime());
        assertEquals(PeriodTimeUnit.MINUTES, rateLimit.getPeriodTimeUnit());
    }

    @Test
    public void shouldHandleELDynamicLimitForLegacyPropertiesV4() {
        var stepNoLimit = new io.gravitee.definition.model.v4.flow.step.Step();
        stepNoLimit.setPolicy(RATE_LIMIT);
        stepNoLimit.setConfiguration(
            "{ \"rate\": {  \"dynamicLimit\": \"{#properties['ratelimit']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" } }"
        );
        stepNoLimit.setEnabled(true);

        var flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setRequest(List.of(stepNoLimit));
        flow1.setEnabled(true);

        planEntityV4.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV4, aV4Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNotNull(rateLimit);
        assertEquals(25L, rateLimit.getLimit());
        assertEquals(6, rateLimit.getPeriodTime());
        assertEquals(PeriodTimeUnit.MINUTES, rateLimit.getPeriodTimeUnit());
    }

    @Test
    public void shouldHandleELDynamicLimitForApiPropertiesV2() {
        var step = new Step();
        step.setPolicy(RATE_LIMIT);
        step.setConfiguration(
            "{ \"rate\": { \"limit\": 0, \"dynamicLimit\": \"{#api.properties['ratelimit']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" }}"
        );
        step.setEnabled(true);

        var flow1 = new Flow();
        flow1.setPre(List.of(step));
        flow1.setEnabled(true);

        planEntityV2.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNotNull(rateLimit);
        assertEquals(25L, rateLimit.getLimit());
        assertEquals(6, rateLimit.getPeriodTime());
        assertEquals(PeriodTimeUnit.MINUTES, rateLimit.getPeriodTimeUnit());
    }

    @Test
    public void shouldHandleELDynamicLimitForLegacyPropertiesV2() {
        var step = new Step();
        step.setPolicy(RATE_LIMIT);
        step.setConfiguration(
            "{ \"rate\": { \"limit\": 0, \"dynamicLimit\": \"{#properties['ratelimit']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" }}"
        );
        step.setEnabled(true);

        var flow1 = new Flow();
        flow1.setPre(List.of(step));
        flow1.setEnabled(true);

        planEntityV2.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNotNull(rateLimit);
        assertEquals(25L, rateLimit.getLimit());
        assertEquals(6, rateLimit.getPeriodTime());
        assertEquals(PeriodTimeUnit.MINUTES, rateLimit.getPeriodTimeUnit());
    }

    @Test
    public void shouldNotMapDynamicLimitIfPropertyNotFound() {
        var step = new Step();
        step.setPolicy(RATE_LIMIT);
        step.setConfiguration(
            "{ \"rate\": { \"limit\": 0, \"dynamicLimit\": \"{#api.properties['not-found']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" }}"
        );
        step.setEnabled(true);

        var flow1 = new Flow();
        flow1.setPre(List.of(step));
        flow1.setEnabled(true);

        planEntityV2.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNull(rateLimit);
    }

    @Test
    public void shouldNotReturnDynamicLimitIfELInvalid() {
        var step = new Step();
        step.setPolicy(RATE_LIMIT);
        step.setConfiguration(
            "{ \"rate\": { \"limit\": 0, \"dynamicLimit\": \"{##api.properties['ratelimit']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" }}"
        );
        step.setEnabled(true);

        var flow1 = new Flow();
        flow1.setPre(List.of(step));
        flow1.setEnabled(true);

        planEntityV2.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV2, aV2Api);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNull(rateLimit);
    }

    @Test
    public void shouldNotReturnDynamicLimitIfApiNull() {
        var step = new Step();
        step.setPolicy(RATE_LIMIT);
        step.setConfiguration(
            "{ \"rate\": { \"limit\": 0, \"dynamicLimit\": \"{##api.properties['ratelimit']}\", \"periodTime\": 6, \"periodTimeUnit\": \"MINUTES\" }}"
        );
        step.setEnabled(true);

        var flow1 = new Flow();
        flow1.setPre(List.of(step));
        flow1.setEnabled(true);

        planEntityV2.setFlows(List.of(flow1));

        Plan responsePlan = planMapper.convert(planEntityV2, null);
        assertNotNull(responsePlan);
        assertNotNull(responsePlan.getUsageConfiguration());
        var rateLimit = responsePlan.getUsageConfiguration().getRateLimit();
        assertNull(rateLimit);
    }

    private void preparePlanEntityV2() {
        planEntityV2 = new PlanEntity();

        planEntityV2.setApi(PLAN_API);
        planEntityV2.setCharacteristics(Arrays.asList(PLAN_CHARACTERISTIC));
        planEntityV2.setClosedAt(nowDate);
        planEntityV2.setCommentMessage(PLAN_COMMENT_MESSAGE);
        planEntityV2.setCommentRequired(true);
        planEntityV2.setCreatedAt(nowDate);
        planEntityV2.setDescription(PLAN_DESCRIPTION);
        planEntityV2.setExcludedGroups(Arrays.asList(PLAN_GROUP));
        planEntityV2.setId(PLAN_ID);
        planEntityV2.setName(PLAN_NAME);
        planEntityV2.setNeedRedeployAt(nowDate);
        planEntityV2.setOrder(1);

        Policy policy = new Policy();
        policy.setConfiguration(PLAN_RULE_POLICY_CONFIGURATION);
        policy.setName(PLAN_RULE_POLICY_NAME);
        Rule rule = new Rule();
        rule.setDescription(PLAN_RULE_DESCRIPTION);
        rule.setEnabled(true);
        rule.setMethods(new HashSet<HttpMethod>(Arrays.asList(HttpMethod.GET)));
        rule.setPolicy(policy);
        Map<String, List<Rule>> paths = new HashMap<>();
        paths.put(PLAN_PATH, Arrays.asList(rule));
        planEntityV2.setPaths(paths);

        planEntityV2.setPublishedAt(nowDate);
        planEntityV2.setSecurity(PlanSecurityType.API_KEY);
        planEntityV2.setSecurityDefinition(PLAN_SECURITY_DEFINITINON);
        planEntityV2.setSelectionRule(PLAN_SELECTION_RULE);
        planEntityV2.setStatus(PlanStatus.PUBLISHED);
        planEntityV2.setTags(new HashSet<String>(Arrays.asList(PLAN_TAG)));
        planEntityV2.setType(PlanType.API);
        planEntityV2.setUpdatedAt(nowDate);
        planEntityV2.setValidation(PlanValidationType.AUTO);
    }

    private void preparePlanEntityV4() {
        planEntityV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();

        planEntityV4.setApiId(PLAN_API);
        planEntityV4.setCharacteristics(Arrays.asList(PLAN_CHARACTERISTIC));
        planEntityV4.setClosedAt(nowDate);
        planEntityV4.setCommentMessage(PLAN_COMMENT_MESSAGE);
        planEntityV4.setCommentRequired(true);
        planEntityV4.setCreatedAt(nowDate);
        planEntityV4.setDescription(PLAN_DESCRIPTION);
        planEntityV4.setExcludedGroups(Arrays.asList(PLAN_GROUP));
        planEntityV4.setId(PLAN_ID);
        planEntityV4.setName(PLAN_NAME);
        planEntityV4.setNeedRedeployAt(nowDate);
        planEntityV4.setOrder(1);
        planEntityV4.setMode(io.gravitee.definition.model.v4.plan.PlanMode.STANDARD);

        planEntityV4.setPublishedAt(nowDate);
        var planSecurity = new PlanSecurity();
        planSecurity.setConfiguration(PLAN_SECURITY_DEFINITINON);
        planSecurity.setType(PlanSecurityType.API_KEY.name());
        planEntityV4.setSecurity(planSecurity);
        planEntityV4.setStatus(io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED);
        planEntityV4.setTags(new HashSet<String>(Arrays.asList(PLAN_TAG)));
        planEntityV4.setType(io.gravitee.rest.api.model.v4.plan.PlanType.API);
        planEntityV4.setUpdatedAt(nowDate);
        planEntityV4.setValidation(io.gravitee.rest.api.model.v4.plan.PlanValidationType.AUTO);
    }
}
