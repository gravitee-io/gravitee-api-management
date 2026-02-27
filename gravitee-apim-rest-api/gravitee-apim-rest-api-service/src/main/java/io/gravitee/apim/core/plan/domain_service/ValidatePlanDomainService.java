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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.factory.PlanModelFactory;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.rest.api.service.common.IdBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
@Slf4j
public class ValidatePlanDomainService implements Validator<ValidatePlanDomainService.Input> {

    private final PlanValidatorDomainService planValidator;

    public record Input(AuditInfo auditInfo, ApiCRDSpec apiCRDSpec, Map<String, PlanCRD> plans, List<Page> pages) implements
        Validator.Input {
        ValidatePlanDomainService.Input sanitized(Map<String, PlanCRD> sanitizedPlans) {
            return new ValidatePlanDomainService.Input(auditInfo, apiCRDSpec, sanitizedPlans, pages);
        }
    }

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (input.plans() == null || input.plans().isEmpty()) {
            log.debug("no plans to validate and sanitize");
            return Result.ofValue(input);
        }
        log.debug("validating plans");

        List<Error> errors = new ArrayList<>();
        Map<String, PlanCRD> sanitizedPlans = new HashMap<>();

        input.plans.forEach((k, planCRD) -> {
            try {
                Plan plan = PlanModelFactory.fromCRDSpec(planCRD, input.apiCRDSpec);
                if (planCRD.getId() == null) {
                    planCRD.setId(IdBuilder.builder(input.auditInfo, input.apiCRDSpec.getHrid()).withExtraId(k).buildId());
                }
                if (
                    (planCRD.getGeneralConditions() == null || planCRD.getGeneralConditions().isEmpty()) &&
                    (planCRD.getGeneralConditionsHrid() != null && !planCRD.getGeneralConditionsHrid().isEmpty())
                ) {
                    planCRD.setGeneralConditions(
                        IdBuilder.builder(input.auditInfo, input.apiCRDSpec.getHrid())
                            .withExtraId(plan.getGeneralConditionsHrid())
                            .buildId()
                    );
                    plan.setGeneralConditionsHrid(planCRD.getGeneralConditionsHrid());
                    plan.setGeneralConditions(planCRD.getGeneralConditions());
                }

                planValidator.validatePlanSecurity(
                    plan,
                    input.auditInfo.organizationId(),
                    input.auditInfo.environmentId(),
                    ApiType.valueOf(input.apiCRDSpec.getType())
                );
                planValidator.validatePlanTagsAgainstApiTags(plan.getPlanTags(), input.apiCRDSpec.getTags());
                planValidator.validateGeneralConditionsPage(plan, input.pages);
                planValidator.validatePlanSecurityAgainstEntrypoints(
                    plan.getPlanSecurity(),
                    input.apiCRDSpec.getListeners().stream().map(AbstractListener::getType).toList()
                );

                sanitizedPlans.put(k, planCRD);
            } catch (Exception e) {
                errors.add(Error.severe("invalid plan [%s]. Error: %s", k, e.getMessage()));
            }
        });

        return Result.ofBoth(input.sanitized(sanitizedPlans), errors);
    }
}
