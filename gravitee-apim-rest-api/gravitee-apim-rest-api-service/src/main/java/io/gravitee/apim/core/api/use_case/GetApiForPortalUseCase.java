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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiForPortalUseCase {

    private final PortalNavigationAccessDomainService accessService;
    private final PortalNavigationApiVisibilityDomainService visibilityDomainService;

    public Output execute(Input input) {
        boolean visible = input
            .portalId()
            .map(portalId -> {
                var portal = accessService.resolveScope(input.organizationId(), input.environmentId(), Optional.of(portalId));
                return visibilityDomainService.isApiVisibleToUser(input.environmentId(), portal, input.apiId(), input.userId());
            })
            .orElseGet(() -> visibilityDomainService.isApiVisibleToUser(input.environmentId(), input.apiId(), input.userId()));
        return new Output(visible);
    }

    /**
     * @param portalId the portal whose listings decide; empty on the unscoped paths, where a listing on any portal of the environment does
     */
    public record Input(String environmentId, String organizationId, String apiId, @Nullable String userId, Optional<PortalId> portalId) {}

    public record Output(boolean visible) {}
}
