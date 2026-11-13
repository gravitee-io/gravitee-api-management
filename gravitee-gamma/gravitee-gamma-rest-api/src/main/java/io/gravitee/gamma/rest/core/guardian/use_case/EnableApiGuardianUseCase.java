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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardian.domain_service.DescribeApiGuardiansDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.ApiCannotRunGuardiansException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianAlreadyEnabledException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianIncompatibleApiTypeException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotDeployedException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Enables a deployed Guardian on an API, after the Guardians already enabled on its phase. The API runs it once
 * redeployed.
 *
 * <p>The phase and the API types come from the revision the gateways run, not from the draft: the gateway checks the
 * Guardian it loads, so a draft a publisher has not deployed yet must not decide where the step goes.
 *
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class EnableApiGuardianUseCase {

    private final GuardedApiPort apis;
    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;
    private final DescribeApiGuardiansDomainService describeApiGuardians;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        var environmentId = input.auditInfo().environmentId();
        var api = apis.findById(environmentId, input.apiId()).orElseThrow(() -> new ApiNotFoundException(input.apiId()));
        if (!api.supportsGuardians()) {
            throw new ApiCannotRunGuardiansException(api.id());
        }
        var guardian = guardianRepository
            .findByEnvironmentId(environmentId, input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
        if (!(deployments.current(guardian) instanceof GuardianDeployment.Published published)) {
            throw new GuardianNotDeployedException(guardian.getId());
        }
        var deployed = published.definition();
        if (!deployed.isCompatibleWith(api.type())) {
            throw new GuardianIncompatibleApiTypeException(guardian.getId(), api.type());
        }
        var guardians = api.guardians();
        if (guardians.isEnabled(guardian.getId())) {
            throw new GuardianAlreadyEnabledException(guardian.getId(), api.id());
        }

        var enabled = guardians.enable(guardian.getId(), deployed.getName(), deployed.getPhase());
        var updated = apis.updateInternalFlow(api.id(), enabled.internalFlow());
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(api.id())
                .organizationId(input.auditInfo().organizationId())
                .environmentId(environmentId)
                .actor(input.auditInfo().actor())
                .event(ApiAuditEvent.API_UPDATED)
                .properties(Map.of(AuditProperties.GUARDIAN, guardian.getId()))
                .oldValue(api.internalFlow())
                .newValue(updated.internalFlow())
                .createdAt(TimeProvider.now())
                .build()
        );
        return new Output(describeApiGuardians.describe(environmentId, updated.guardians()));
    }

    public record Input(AuditInfo auditInfo, String apiId, String guardianId) {}

    public record Output(List<ApiGuardian> guardians) {}
}
