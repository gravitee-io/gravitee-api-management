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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.el.TemplateEngine;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiTemplateVariables;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.portal.rest.model.PeriodTimeUnit;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.Plan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.Plan.ValidationEnum;
import io.gravitee.rest.api.portal.rest.model.PlanMode;
import io.gravitee.rest.api.portal.rest.model.PlanUsageConfiguration;
import io.gravitee.rest.api.portal.rest.model.TimePeriodConfiguration;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class PlanMapper {

    private final GraviteeMapper mapper = new GraviteeMapper();
    private final String POLICY_CONFIGURATION_LIMIT = "limit";
    private final String POLICY_CONFIGURATION_DYNAMIC_LIMIT = "dynamicLimit";
    private final String POLICY_CONFIGURATION_PERIOD_TIME = "periodTime";
    private final String POLICY_CONFIGURATION_PERIOD_TIME_UNIT = "periodTimeUnit";
    private final String RATE_LIMIT_POLICY_ID = "rate-limit";
    private final String QUOTA_POLICY_ID = "quota";
    private final Map<String, String> POLICY_CONFIGURATION_ATTRIBUTES = Map.of(QUOTA_POLICY_ID, "quota", RATE_LIMIT_POLICY_ID, "rate");

    public Plan convert(GenericPlanEntity plan, GenericApiEntity api) {
        final Plan planItem = new Plan();

        planItem.setCharacteristics(plan.getCharacteristics());
        planItem.setCommentQuestion(plan.getCommentMessage());
        planItem.setCommentRequired(plan.isCommentRequired());
        planItem.setDescription(plan.getDescription());
        planItem.setId(plan.getId());
        planItem.setName(plan.getName());
        planItem.setOrder(plan.getOrder());
        if (plan.getPlanSecurity() != null && plan.getPlanSecurity().getType() != null) {
            PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(plan.getPlanSecurity().getType());
            planItem.setSecurity(SecurityEnum.fromValue(planSecurityType.name()));
        }
        planItem.setValidation(ValidationEnum.fromValue(plan.getPlanValidation().name()));
        planItem.setGeneralConditions(plan.getGeneralConditions());
        planItem.setMode(PlanMode.valueOf(plan.getPlanMode().name()));

        planItem.setUsageConfiguration(this.toUsageConfiguration(plan, api));

        return planItem;
    }

    private PlanUsageConfiguration toUsageConfiguration(GenericPlanEntity plan, GenericApiEntity api) {
        var configuration = new PlanUsageConfiguration();
        configuration.setRateLimit(this.getMostRestrictivePolicyConfiguration(RATE_LIMIT_POLICY_ID, plan, api));
        configuration.setQuota(this.getMostRestrictivePolicyConfiguration(QUOTA_POLICY_ID, plan, api));
        return configuration;
    }

    /**
     * Get the most restrictive configuration pertaining to the given policy
     *
     * @param policyId -- Filter by policyId, ex. "rate-limit"
     * @param plan -- GenericPlanEntity
     * @return A TimePeriodConfiguration with the longest duration between calls. Null if no valid configuration exists.
     */
    private TimePeriodConfiguration getMostRestrictivePolicyConfiguration(String policyId, GenericPlanEntity plan, GenericApiEntity api) {
        List<String> policyConfigurations = this.getEnabledConfigurationsByPolicyId(policyId, plan);

        // Configuration with the longest minimum duration between calls
        var longestMinimumDuration = new AtomicReference<TimePeriodConfiguration>();
        policyConfigurations
            .stream()
            .map(stepConfiguration -> {
                try {
                    return mapper.readTree(stepConfiguration);
                } catch (JsonProcessingException e) {
                    return JsonNodeFactory.instance.objectNode();
                }
            })
            .map(configuration -> {
                var policyAttribute = POLICY_CONFIGURATION_ATTRIBUTES.get(policyId);
                return configuration.has(policyAttribute) ? configuration.get(policyAttribute) : JsonNodeFactory.instance.objectNode();
            })
            .forEach(configuration -> {
                if (!configuration.has(POLICY_CONFIGURATION_LIMIT) && !configuration.has(POLICY_CONFIGURATION_DYNAMIC_LIMIT)) {
                    return;
                }

                var limitToUse = this.getLimitToUse(configuration, api);
                if (limitToUse < 0) {
                    return;
                }

                var configAsTimePeriodConfiguration = new TimePeriodConfiguration();
                configAsTimePeriodConfiguration.setLimit(limitToUse);
                configAsTimePeriodConfiguration.setPeriodTime(configuration.get(POLICY_CONFIGURATION_PERIOD_TIME).intValue());
                configAsTimePeriodConfiguration.setPeriodTimeUnit(
                    PeriodTimeUnit.valueOf(configuration.get(POLICY_CONFIGURATION_PERIOD_TIME_UNIT).asText())
                );

                var configDuration = this.calculateDuration(configAsTimePeriodConfiguration);
                var currentLongestMinimumDuration = this.calculateDuration(longestMinimumDuration.get());
                if (
                    Objects.isNull(currentLongestMinimumDuration) ||
                    (Objects.nonNull(configDuration) && currentLongestMinimumDuration.compareTo(configDuration) < 0)
                ) {
                    longestMinimumDuration.set(configAsTimePeriodConfiguration);
                }
            });

        return longestMinimumDuration.get();
    }

    private Duration calculateDuration(TimePeriodConfiguration configuration) {
        if (
            Objects.isNull(configuration) ||
            Objects.isNull(configuration.getLimit()) ||
            Objects.isNull(configuration.getPeriodTime()) ||
            Objects.isNull(configuration.getPeriodTimeUnit())
        ) {
            return null;
        }
        if (configuration.getLimit().intValue() == 0) {
            return Duration.ZERO;
        }
        return ChronoUnit
            .valueOf(configuration.getPeriodTimeUnit().getValue())
            .getDuration()
            .multipliedBy(configuration.getPeriodTime().intValue())
            .dividedBy(configuration.getLimit().intValue());
    }

    private long getLimitToUse(JsonNode configuration, GenericApiEntity api) {
        // Dynamic limit exists and limit is either missing or equal to 0
        if (
            configuration.has(POLICY_CONFIGURATION_DYNAMIC_LIMIT) &&
            (!configuration.has(POLICY_CONFIGURATION_LIMIT) || configuration.get(POLICY_CONFIGURATION_LIMIT).intValue() == 0)
        ) {
            return parseLimit(configuration.get(POLICY_CONFIGURATION_DYNAMIC_LIMIT), api);
        }
        return parseLimit(configuration.get(POLICY_CONFIGURATION_LIMIT), api);
    }

    private long parseLimit(JsonNode limitNode, GenericApiEntity api) {
        var evaluatedLimit = evaluateLimit(limitNode, api);
        if (evaluatedLimit != null) {
            try {
                return Long.parseLong(evaluatedLimit);
            } catch (NumberFormatException ignored) {
                log.debug("Limit could not be parsed: {}", evaluatedLimit);
            }
        }
        return -1L;
    }

    private String evaluateLimit(JsonNode limitNode, GenericApiEntity api) {
        var apiParams = new ApiTemplateVariables(api);

        TemplateEngine templateEngine = TemplateEngine.templateEngine();
        templateEngine.getTemplateContext().setVariable("api", apiParams);
        templateEngine.getTemplateContext().setVariable("properties", apiParams.getProperties());

        return templateEngine.eval(limitNode.asText(), String.class).onErrorReturnItem(limitNode.asText()).blockingGet();
    }

    /**
     * Extract the Configuration strings from the policies that are enabled
     *
     * @param policyId -- Filter by policyId, ex. "rate-limit"
     * @param plan -- GenericPlanEntity
     * @return List of configurations for the given policy
     */
    private List<String> getEnabledConfigurationsByPolicyId(String policyId, GenericPlanEntity plan) {
        switch (plan.getDefinitionVersion()) {
            case V2 -> {
                var planV2 = (PlanEntity) plan;
                return planV2
                    .getFlows()
                    .stream()
                    .filter(Flow::isEnabled)
                    .flatMap(flow -> flow.getPre().stream())
                    .filter(step -> step.isEnabled() && Objects.equals(policyId, step.getPolicy()))
                    .map(io.gravitee.definition.model.flow.Step::getConfiguration)
                    .toList();
            }
            case V4 -> {
                var planV4 = (io.gravitee.rest.api.model.v4.plan.PlanEntity) plan;
                return planV4
                    .getFlows()
                    .stream()
                    .filter(io.gravitee.definition.model.v4.flow.Flow::isEnabled)
                    .flatMap(flow -> flow.getRequest().stream())
                    .filter(step -> step.isEnabled() && Objects.equals(policyId, step.getPolicy()))
                    .map(Step::getConfiguration)
                    .toList();
            }
            default -> {
                return List.of();
            }
        }
    }
}
