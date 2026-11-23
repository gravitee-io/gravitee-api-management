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
package io.gravitee.apim.core.portal.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.domain_service.PortalNameDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalSummary;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RenamePortalUseCase {

    private final PortalCrudService portalCrudService;
    private final PortalNameDomainService portalNameDomainService;
    private final PortalSummaryDomainService portalSummaryDomainService;

    public record Input(String environmentId, PortalId portalId, @Nullable String name) {}

    public PortalSummary execute(Input input) {
        var portal = portalCrudService
            .findByIdAndEnvironmentId(input.portalId(), input.environmentId())
            .orElseThrow(() -> new PortalNotFoundException(input.portalId().toString()));

        var requestedName = input.name();
        if (requestedName != null) {
            var normalizedName = portalNameDomainService.validateAndNormalize(requestedName);
            var currentName = portal.getName();
            // Repeating the stored name is a no-op, even for a legacy name with surrounding whitespace.
            var unchanged = requestedName.equals(currentName) || normalizedName.equals(currentName);
            if (!unchanged) {
                portalNameDomainService.ensureUnique(input.environmentId(), normalizedName, input.portalId());
                portal = portalCrudService
                    .updateName(input.portalId(), input.environmentId(), normalizedName)
                    .orElseThrow(() -> new PortalNotFoundException(input.portalId().toString()));
            }
        }
        return portalSummaryDomainService.summarize(input.environmentId(), List.of(portal)).getFirst();
    }
}
