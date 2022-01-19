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
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.rest.api.service.impl.upgrade.UpgradeStatus.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class PlansDataFixUpgrader implements Upgrader, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlansDataFixUpgrader.class);
    private static final String PLAN_DESCRIPTION =
        "This plan has been recreated during a data fix process. See documentation : https://docs.gravitee.io/apim/3.x/apim_installguide_migration.html#upgrade_to_3_10_8";
    private static final String PLAN_NAME_SUFFIX = "-Recreated";

    @Autowired
    private InstallationService installationService;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApiService apiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.plans-data-fix-upgrader.enabled:true}")
    private boolean enabled;

    @Value("${services.plans-data-fix-upgrader.dryRun:true}")
    private boolean dryRun;

    @Value("${services.plans-data-fix-upgrader.notifyApiOwner:false}")
    private boolean notifyApiOwner;

    private boolean anomalyFound = false;

    @Override
    public boolean upgrade() {
        if (!enabled) {
            LOGGER.info("Skipping {} execution cause it's not enabled in configuration", this.getClass().getSimpleName());
            return false;
        }
        InstallationEntity installation = installationService.getOrInitialize();
        if (dryRun && isStatus(installation, DRY_SUCCESS)) {
            LOGGER.info(
                "Skipping {} execution cause it has already been successfully executed in dry mode",
                this.getClass().getSimpleName()
            );
            return false;
        }
        if (isStatus(installation, SUCCESS)) {
            LOGGER.info("Skipping {} execution cause it has already been successfully executed", this.getClass().getSimpleName());
            return false;
        }
        if (isStatus(installation, RUNNING)) {
            LOGGER.warn("Skipping {} execution cause it's already running", this.getClass().getSimpleName());
            return false;
        }

        try {
            LOGGER.info("Starting {} execution with dry-run {}", this.getClass().getSimpleName(), dryRun ? "enabled" : "disabled");
            setExecutionStatus(installation, RUNNING);
            fixPlansData();
            setExecutionStatus(installation, dryRun ? DRY_SUCCESS : SUCCESS);
        } catch (Throwable e) {
            LOGGER.error("{} execution failed", this.getClass().getSimpleName(), e);
            setExecutionStatus(installation, FAILURE);
            return false;
        }
        LOGGER.info("Finishing {} execution", this.getClass().getSimpleName());
        return true;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    protected void fixPlansData() throws Exception {
        for (Api api : apiRepository.findAll()) {
            io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(
                api.getDefinition(),
                io.gravitee.definition.model.Api.class
            );

            if (DefinitionVersion.V2 == apiDefinition.getDefinitionVersion()) {
                fixApiPlans(api, apiDefinition);
            }
        }
        if (!anomalyFound) {
            LOGGER.info("No plan data anomaly found");
        }
    }

    protected void fixApiPlans(Api api, io.gravitee.definition.model.Api apiDefinition) throws Exception {
        Set<Plan> apiPlans = planRepository.findByApi(api.getId());
        List<io.gravitee.definition.model.Plan> definitionPlans = apiDefinition.getPlans();

        if (!hasPlansDataAnomaly(apiPlans, definitionPlans)) {
            LOGGER.debug("Skipping API {} : no plans anomaly detected", api.getId());
            return;
        }

        if (!anomalyFound) {
            logWarningHeaderBlock();
            anomalyFound = true;
        }

        LOGGER.info("Plans anomalies found for API \"{}\" ({}) :", api.getName(), api.getId());

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
        if (!dryRun && notifyApiOwner) {
            sendEmailToApiOwner(api, createdPlans, closedPlans);
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
                LOGGER.info(
                    "- Will create plan \"{}\" for API \"{}\" ({}), which is missing in plans table",
                    newApiPlan.getName(),
                    api.getName(),
                    api.getId()
                );
                if (!dryRun) {
                    planRepository.create(newApiPlan);
                }
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
            LOGGER.info("- Will close plan \"{}\" ({}), cause it's absent from api definition", plan.getName(), plan.getId());
            plan.setStatus(Plan.Status.CLOSED);
            if (!dryRun) {
                planRepository.update(plan);
            }
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
        if (!dryRun) {
            apiRepository.update(api);
        }
    }

    private void setExecutionStatus(InstallationEntity installation, UpgradeStatus status) {
        installation.getAdditionalInformation().put(InstallationService.PLANS_DATA_UPGRADER_STATUS, status.toString());
        installationService.setAdditionalInformation(installation.getAdditionalInformation());
    }

    private boolean isStatus(InstallationEntity installation, UpgradeStatus status) {
        return status.toString().equals(installation.getAdditionalInformation().get(InstallationService.PLANS_DATA_UPGRADER_STATUS));
    }

    private boolean hasPlansDataAnomaly(Set<Plan> apiPlans, List<io.gravitee.definition.model.Plan> definitionPlans) {
        List<String> apiPlansIds = apiPlans
            .stream()
            .filter(plan -> plan.getStatus() != Plan.Status.CLOSED)
            .map(Plan::getId)
            .collect(toList());
        List<String> definitionPlansIds = definitionPlans.stream().map(io.gravitee.definition.model.Plan::getId).collect(toList());
        return apiPlansIds.size() != definitionPlansIds.size() || !definitionPlansIds.containsAll(apiPlansIds);
    }

    protected void sendEmailToApiOwner(Api api, List<Plan> createdPlans, List<Plan> closedPlans) {
        getApiOwnerEmail(api)
            .ifPresent(
                apiOwnerEmail -> {
                    LOGGER.debug("Sending report email to api {} owner", api.getId());
                    emailService.sendAsyncEmailNotification(
                        new EmailNotificationBuilder()
                            .params(Map.of("api", api, "closedPlans", closedPlans, "createdPlans", createdPlans))
                            .to(apiOwnerEmail)
                            .template(EmailNotificationBuilder.EmailTemplate.API_PLANS_DATA_FIXED)
                            .build(),
                        GraviteeContext.getCurrentContext()
                    );
                }
            );
    }

    private Optional<String> getApiOwnerEmail(Api api) {
        return Optional.ofNullable(apiService.getPrimaryOwner(api.getId()).getEmail());
    }

    private void logWarningHeaderBlock() {
        LOGGER.warn("");
        LOGGER.warn("##############################################################");
        LOGGER.warn("#                           WARNING                          #");
        LOGGER.warn("##############################################################");
        LOGGER.warn("");
        LOGGER.warn("We detected database anomalies in your plans data.");
        LOGGER.warn("");
        if (dryRun) {
            LOGGER.warn("THIS IS A DRY RUN. DATABASE WON'T BE UPDATED.");
            LOGGER.warn("To fix anomalies, disable the dry run mode.");
            LOGGER.warn("Below, a list of changes that would happen without dry run");
        } else {
            LOGGER.warn("Database anomalies will be fixed.");
        }
        LOGGER.warn("See related documentation : https://docs.gravitee.io/apim/3.x/apim_installguide_migration.html#upgrade_to_3_10_8");
        LOGGER.warn("");
        LOGGER.warn("##############################################################");
        LOGGER.warn("");
    }
}
