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
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Turns a Guardian off on an API. The API stops running it once redeployed; a call already in its sequence finishes.
 *
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class DisableApiGuardianUseCase {

    private final GuardedApiPort apis;
    private final DescribeApiGuardiansDomainService describeApiGuardians;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        var environmentId = input.auditInfo().environmentId();
        var api = apis.findById(environmentId, input.apiId()).orElseThrow(() -> new ApiNotFoundException(input.apiId()));
        var disabled = api.guardians().disable(input.guardianId());

        var updated = apis.updateInternalFlow(api.id(), disabled.internalFlow());
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(api.id())
                .organizationId(input.auditInfo().organizationId())
                .environmentId(environmentId)
                .actor(input.auditInfo().actor())
                .event(ApiAuditEvent.API_UPDATED)
                .properties(Map.of(AuditProperties.GUARDIAN, input.guardianId()))
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
