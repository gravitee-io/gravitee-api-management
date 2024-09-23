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

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.plan.exception.PlanInvalidException;
import io.gravitee.apim.core.plan.exception.UnauthorizedPlanSecurityTypeException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class PlanValidatorDomainService {

    private final ParametersQueryService parametersQueryService;
    private final PolicyValidationDomainService policyValidationDomainService;
    private final PageCrudService pageCrudService;

    public PlanValidatorDomainService(
        ParametersQueryService parametersQueryService,
        PolicyValidationDomainService policyValidationDomainService,
        PageCrudService pageCrudService
    ) {
        this.parametersQueryService = parametersQueryService;
        this.policyValidationDomainService = policyValidationDomainService;
        this.pageCrudService = pageCrudService;
    }

    public void validatePlanSecurity(Plan plan, String currentOrganizationId, String currentEnvironmentId) {
        var planMode = plan.getPlanMode();
        var security = plan.getPlanSecurity();
        if (planMode.equals(PlanMode.STANDARD)) {
            if (security == null) {
                throw new PlanInvalidException("Security type is required for plan with 'STANDARD' mode");
            }
            ensurePlanSecurityIsAllowed(security.getType(), currentOrganizationId, currentEnvironmentId);
            policyValidationDomainService.validateAndSanitizeConfiguration(security.getType(), security.getConfiguration());
        }

        if (planMode.equals(PlanMode.PUSH) && security != null) {
            throw new PlanInvalidException("Security type is forbidden for plan with 'Push' mode");
        }
    }

    public void validatePlanTagsAgainstApiTags(Set<String> planTags, Set<String> apiTags) {
        if (!isEmpty(planTags) && (isEmpty(apiTags) || apiTags.stream().noneMatch(planTags::contains))) {
            log.debug("Plan rejected, tags {} mismatch the tags defined by the API ({})", planTags, apiTags);

            throw new ValidationDomainException(
                "Plan tags mismatch the tags defined by the API",
                Map.of("planTags", String.join(",", planTags), "apiTags", String.join(",", apiTags))
            );
        }
    }

    public void validateGeneralConditionsPageStatus(Plan plan) {
        if (plan.getGeneralConditions() != null && !plan.getGeneralConditions().isEmpty() && (plan.isPublished() || plan.isDeprecated())) {
            var page = pageCrudService.findById(plan.getGeneralConditions());
            if (page.isEmpty()) {
                return;
            }
            var isPublished = page.map(Page::isPublished).orElse(false);
            if (!isPublished) {
                throw new ValidationDomainException("Plan references a non published page as general conditions");
            }
        }
    }

    private void ensurePlanSecurityIsAllowed(String securityType, String currentOrganizationId, String currentEnvironmentId) {
        PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(securityType);
        Key securityKey =
            switch (planSecurityType) {
                case API_KEY -> Key.PLAN_SECURITY_APIKEY_ENABLED;
                case KEY_LESS -> Key.PLAN_SECURITY_KEYLESS_ENABLED;
                case JWT -> Key.PLAN_SECURITY_JWT_ENABLED;
                case OAUTH2 -> Key.PLAN_SECURITY_OAUTH2_ENABLED;
                case MTLS -> Key.PLAN_SECURITY_MTLS_ENABLED;
            };
        if (
            !parametersQueryService.findAsBoolean(
                securityKey,
                new ParameterContext(currentEnvironmentId, currentOrganizationId, ParameterReferenceType.ENVIRONMENT)
            )
        ) {
            throw new UnauthorizedPlanSecurityTypeException(planSecurityType.name());
        }
    }

    public void validatePlanSecurityAgainstEntrypoints(PlanSecurity planSecurity, List<ListenerType> listenerTypes) {
        if (
            listenerTypes.contains(ListenerType.TCP) &&
            !(
                PlanSecurityType.KEY_LESS.getLabel().equals(planSecurity.getType()) ||
                PlanSecurityType.MTLS.getLabel().equals(planSecurity.getType())
            )
        ) {
            throw new UnauthorizedPlanSecurityTypeException(planSecurity.getType());
        }
    }
}
