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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class PlansDataFixUpgrader implements Upgrader {

    private static final String PLAN_DESCRIPTION =
        "This plan has been recreated during a data fix process. See documentation : https://docs.gravitee.io/apim/3.x/apim_installguide_migration.html#upgrade_to_3_10_8";
    private static final String PLAN_NAME_SUFFIX = "-Recreated";
    private final Map<String, ExecutionContext> executionContextByEnvironment = new ConcurrentHashMap<>();

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PrimaryOwnerService primaryOwnerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.plans-data-fix-upgrader.enabled:true}")
    private boolean enabled;

    @Value("${services.plans-data-fix-upgrader.notifyApiOwner:false}")
    private boolean notifyApiOwner;

    private boolean anomalyFound = false;

    @Override
    public int getOrder() {
        return UpgraderOrder.PLANS_DATA_FIX_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        if (enabled) {
            return this.wrapException(this::applyUpgrade);
        }
        log.info("Skipping {} execution cause it's not enabled in configuration", this.getClass().getSimpleName());
        return true;
    }

    private boolean applyUpgrade() throws Exception {
        AtomicBoolean upgradeFailed = new AtomicBoolean(false);

        var apis = apiRepository
            .search(new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V2)).build(), null, ApiFieldFilter.allFields())
            .toList();

        for (var api : apis) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                    api.getDefinition(),
                    io.gravitee.definition.model.Api.class
                );
                ExecutionContext executionContext = getApiExecutionContext(api);
                if (executionContext != null) {
                    fixApiPlans(executionContext, api, apiDefinition);
                }
            } catch (Exception e) {
                upgradeFailed.set(true);
                throw e;
            }
        }

        if (!anomalyFound) {
            log.info("No plan data anomaly found");
        }

        return !upgradeFailed.get();
    }

    protected void fixApiPlans(ExecutionContext executionContext, Api api, io.gravitee.definition.model.Api apiDefinition)
        throws Exception {
        Set<Plan> apiPlans = planRepository.findByApi(api.getId());
        List<io.gravitee.definition.model.Plan> definitionPlans = apiDefinition.getPlans();

        if (!hasPlansDataAnomaly(apiPlans, definitionPlans)) {
            log.debug("Skipping API {} : no plans anomaly detected", api.getId());
            return;
        }

        if (!anomalyFound) {
            logWarningHeaderBlock();
            anomalyFound = true;
        }

        log.info("Plans anomalies found for API \"{}\" ({}) :", api.getName(), api.getId());

        Map<String, Plan> apiPlansMap = apiPlans.stream().collect(toMap(Plan::getId, plan -> plan));
        Map<String, io.gravitee.definition.model.Plan> definitionPlansMap = definitionPlans
            .stream()
            .collect(toMap(io.gravitee.definition.model.Plan::getId, plan -> plan, (plan1, plan2) -> plan1));

        // close plans in plans table, which are absent from api definition
        List<Plan> closedPlans = closeExtraApiPlans(definitionPlansMap, apiPlansMap);

        // add in plans tables, all plans that are present in api definition, but not plans table
        List<Plan> createdPlans = createMissingApiPlans(definitionPlansMap, apiPlansMap, api);

        // update api definition plans (only ids and names changed)
        updateApiDefinitionPlans(api, apiDefinition, definitionPlansMap);

        // notify API owner by email
        if (notifyApiOwner) {
            sendEmailToApiOwner(executionContext, api, createdPlans, closedPlans);
        }
    }

    protected List<Plan> createMissingApiPlans(
        Map<String, io.gravitee.definition.model.Plan> definitionPlansMap,
        Map<String, Plan> apiPlansMap,
        Api api
    ) throws TechnicalException {
        List<Plan> addedPlans = new ArrayList<>();
        for (Map.Entry<String, io.gravitee.definition.model.Plan> definitionPlanEntry : definitionPlansMap.entrySet()) {
            String planId = definitionPlanEntry.getKey();
            if (!apiPlansMap.containsKey(planId)) {
                io.gravitee.definition.model.Plan definitionPlan = definitionPlanEntry.getValue();
                Plan newApiPlan = planFromDefinitionPlan(definitionPlan, api.getId());
                log.info(
                    "- Will create plan \"{}\" for API \"{}\" ({}), which is missing in plans table",
                    newApiPlan.getName(),
                    api.getName(),
                    api.getId()
                );

                planRepository.create(newApiPlan);

                apiPlansMap.put(planId, newApiPlan);
                definitionPlan.setId(newApiPlan.getId());
                definitionPlan.setName(newApiPlan.getName());
                addedPlans.add(newApiPlan);
            }
        }
        return addedPlans;
    }

    protected List<Plan> closeExtraApiPlans(
        Map<String, io.gravitee.definition.model.Plan> definitionPlansMap,
        Map<String, Plan> apiPlansMap
    ) throws TechnicalException {
        List<Plan> extraPlans = apiPlansMap
            .values()
            .stream()
            .filter(apiPlan -> apiPlan.getStatus() != Plan.Status.CLOSED)
            .filter(apiPlan -> !definitionPlansMap.containsKey(apiPlan.getId()))
            .collect(toList());
        for (Plan plan : extraPlans) {
            log.info("- Will close plan \"{}\" ({}), cause it's absent from api definition", plan.getName(), plan.getId());
            plan.setStatus(Plan.Status.CLOSED);

            planRepository.update(plan);
        }
        return extraPlans;
    }

    private Plan planFromDefinitionPlan(io.gravitee.definition.model.Plan definitionPlan, String apiId) {
        Plan plan = new Plan();
        plan.setId(UuidString.generateRandom());
        plan.setType(Plan.PlanType.API);
        plan.setValidation(Plan.PlanValidationType.MANUAL);
        plan.setStatus(Plan.Status.DEPRECATED);
        plan.setName(definitionPlan.getName().concat(PLAN_NAME_SUFFIX));
        plan.setDescription(PLAN_DESCRIPTION);
        plan.setApi(apiId);
        plan.setSecurityDefinition(definitionPlan.getSecurityDefinition());
        plan.setSelectionRule(definitionPlan.getSelectionRule());
        plan.setTags(definitionPlan.getTags());
        plan.setCreatedAt(new Date());
        plan.setUpdatedAt(plan.getCreatedAt());
        plan.setNeedRedeployAt(plan.getCreatedAt());
        if (definitionPlan.getSecurity() != null) {
            plan.setSecurity(Plan.PlanSecurityType.valueOf(definitionPlan.getSecurity()));
        }
        return plan;
    }

    private void updateApiDefinitionPlans(
        Api api,
        io.gravitee.definition.model.Api apiDefinition,
        Map<String, io.gravitee.definition.model.Plan> definitionPlansMap
    ) throws TechnicalException, JsonProcessingException {
        apiDefinition.setPlans(new ArrayList<>(definitionPlansMap.values()));
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        apiRepository.update(api);
    }

    private boolean hasPlansDataAnomaly(Set<Plan> apiPlans, List<io.gravitee.definition.model.Plan> definitionPlans) {
        List<String> apiPlansIds = apiPlans
            .stream()
            .filter(plan -> plan.getStatus() != Plan.Status.CLOSED)
            .map(Plan::getId)
            .toList();
        List<String> definitionPlansIds = definitionPlans.stream().map(io.gravitee.definition.model.Plan::getId).toList();
        return apiPlansIds.size() != definitionPlansIds.size() || !definitionPlansIds.containsAll(apiPlansIds);
    }

    protected void sendEmailToApiOwner(ExecutionContext executionContext, Api api, List<Plan> createdPlans, List<Plan> closedPlans) {
        getApiOwnerEmail(executionContext.getOrganizationId(), api).ifPresent(apiOwnerEmail -> {
            log.debug("Sending report email to api {} owner", api.getId());
            emailService.sendAsyncEmailNotification(
                executionContext,
                new EmailNotificationBuilder()
                    .params(Map.of("api", api, "closedPlans", closedPlans, "createdPlans", createdPlans))
                    .to(apiOwnerEmail)
                    .template(EmailNotificationBuilder.EmailTemplate.API_PLANS_DATA_FIXED)
                    .build()
            );
        });
    }

    private Optional<String> getApiOwnerEmail(String organizationId, Api api) {
        return Optional.ofNullable(primaryOwnerService.getPrimaryOwner(organizationId, api.getId()).getEmail());
    }

    private void logWarningHeaderBlock() {
        log.warn("");
        log.warn("##############################################################");
        log.warn("#                           WARNING                          #");
        log.warn("##############################################################");
        log.warn("");
        log.warn("We detected database anomalies in your plans data.");
        log.warn("");
        log.warn("Database anomalies will be fixed.");
        log.warn("See related documentation : https://docs.gravitee.io/apim/3.x/apim_installguide_migration.html#upgrade_to_3_10_8");
        log.warn("");
        log.warn("##############################################################");
        log.warn("");
    }

    private ExecutionContext getApiExecutionContext(Api api) {
        return executionContextByEnvironment.computeIfAbsent(api.getEnvironmentId(), envId -> {
            try {
                return environmentRepository.findById(api.getEnvironmentId()).map(ExecutionContext::new).orElse(null);
            } catch (TechnicalException e) {
                log.error("failed to find environment {}", api.getEnvironmentId(), e);
                return null;
            }
        });
    }
}
