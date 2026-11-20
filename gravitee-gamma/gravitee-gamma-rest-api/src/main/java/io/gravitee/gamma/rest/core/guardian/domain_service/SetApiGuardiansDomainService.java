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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.exception.ApiCannotRunGuardiansException;
import io.gravitee.gamma.rest.core.guardian.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Sets the Guardians an API runs, per phase and in execution order, and applies that change to the gateways by the
 * same call: a Guardian turned on starts judging traffic, one turned off stops, without a deployment of the API. Only
 * the Guardians travel — a plan or a flow saved on the API without being deployed stays undeployed.
 *
 * <p>An API the gateways do not run — never deployed, stopped or unpublished — is written and nothing is sent: it
 * stays out of sync, and its publisher's next deployment is what carries the Guardians over.
 *
 * <p>The whole list of each phase travels, so a Guardian the API ran and the list leaves out is turned off by the
 * same call. Nothing is written unless every listed Guardian passes
 * {@link ValidateApiGuardiansDomainService}: a list holding one Guardian the API cannot run leaves the API as it was.
 *
 * <p>The sequence replaces the stored one whole, and only while the API still holds the internal flow it was computed
 * from: two publishers setting the Guardians of the same API at the same time get a conflict rather than one of them
 * silently losing their call.
 *
 * <p>A domain service rather than a use case because the callers are the modules owning the API surfaces a Guardian
 * can be turned on from — the LLM proxies of the AIM module today — and a use case may not call another one.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class SetApiGuardiansDomainService {

    private final GuardedApiPort apis;
    private final ValidateApiGuardiansDomainService validateApiGuardians;
    private final DescribeApiGuardiansDomainService describeApiGuardians;
    private final AuditDomainService auditDomainService;

    /**
     * @param requestGuardianIds the Guardians of the REQUEST phase, in execution order; an empty or {@code null} list
     *     turns off those the API ran on that phase.
     * @param responseGuardianIds the same, for the RESPONSE phase.
     * @param auditInfo who is calling and from where; the environment of the call is read from it, so an API of
     *     another environment answers as an unknown one.
     * @return the Guardians the API now runs, named as the revision the gateways run names them.
     * @throws ApiNotFoundException when the environment holds no such API.
     * @throws ApiCannotRunGuardiansException when the API holds no internal flow to put the steps in.
     * @throws ApiInternalFlowChangedException when the API's internal flow moved while the call was computing one.
     * @throws io.gravitee.apim.core.exception.TechnicalDomainException when the change cannot be sent to the gateways
     *     because what they run cannot be read. Nothing is written then: the API keeps the Guardians it ran.
     */
    public List<ApiGuardian> set(String apiId, List<String> requestGuardianIds, List<String> responseGuardianIds, AuditInfo auditInfo) {
        var environmentId = auditInfo.environmentId();
        var api = apis.findById(environmentId, apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
        // Read first: an API holding no internal flow is refused as such, before its Guardians are judged.
        var guardians = api.guardians();
        // Both phases in one call: the request is one sequence, so a fault on either phase is part of one answer.
        var requested = Map.of(Phase.REQUEST, orEmpty(requestGuardianIds), Phase.RESPONSE, orEmpty(responseGuardianIds));
        var validated = validateApiGuardians.validate(api, requested);

        var internalFlow = guardians
            .replace(Phase.REQUEST, validated.get(Phase.REQUEST))
            .replace(Phase.RESPONSE, validated.get(Phase.RESPONSE))
            .internalFlow();
        var updated = apis.updateInternalFlow(auditInfo, api.id(), api.internalFlow(), internalFlow);
        ensureStillValid(auditInfo, api, updated, requested);
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(api.id())
                .organizationId(auditInfo.organizationId())
                .environmentId(environmentId)
                .actor(auditInfo.actor())
                .event(ApiAuditEvent.API_UPDATED)
                .properties(Map.of())
                .oldValue(api.internalFlow())
                .newValue(updated.internalFlow())
                .createdAt(TimeProvider.now())
                .build()
        );
        return describeApiGuardians.describe(environmentId, updated.guardians());
    }

    /**
     * Validates the Guardians again once written, putting back the internal flow the API ran when one is refused.
     *
     * <p>The validation made before the write cannot see a Guardian undeployed, deleted or published elsewhere
     * between that validation and the write. Those changes check the APIs again after their own write, so whichever of
     * the two writes lands second, its check sees the other one.
     */
    private void ensureStillValid(AuditInfo auditInfo, GuardedApi before, GuardedApi updated, Map<Phase, List<String>> requested) {
        try {
            validateApiGuardians.validate(before, requested);
        } catch (ValidationDomainException | NotFoundDomainException refused) {
            apis.updateInternalFlow(auditInfo, before.id(), updated.internalFlow(), before.internalFlow());
            throw refused;
        }
    }

    private static List<String> orEmpty(List<String> guardianIds) {
        return guardianIds == null ? List.of() : List.copyOf(guardianIds);
    }
}
