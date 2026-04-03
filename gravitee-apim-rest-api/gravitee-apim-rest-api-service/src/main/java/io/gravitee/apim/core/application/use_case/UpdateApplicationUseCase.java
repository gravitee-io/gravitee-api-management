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
package io.gravitee.apim.core.application.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.application.domain_service.ImportApplicationCRDDomainService;
import io.gravitee.apim.core.application_certificate.domain_service.MtlsSubscriptionSyncDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class UpdateApplicationUseCase {

    private final ImportApplicationCRDDomainService importApplicationCRDDomainService;
    private final MtlsSubscriptionSyncDomainService mtlsSubscriptionSyncDomainService;

    public record Input(AuditInfo auditInfo, String applicationId, UpdateApplicationEntity updateApplicationEntity) {}

    public record Output(ApplicationEntity application) {}

    public Output execute(Input input) {
        var updated = (ApplicationEntity) importApplicationCRDDomainService.update(
            input.applicationId(),
            input.updateApplicationEntity(),
            input.auditInfo()
        );
        mtlsSubscriptionSyncDomainService.updateActiveMTLSSubscriptions(input.applicationId());
        return new Output(updated);
    }
}
