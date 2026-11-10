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
import io.gravitee.apim.core.portal.domain_service.PortalQuotaDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.model.PortalQuota;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalsStatsUseCase {

    private final PortalCrudService portalCrudService;
    private final PortalQuotaDomainService portalQuotaDomainService;
    private final PortalSummaryDomainService portalSummaryDomainService;

    public record Input(String organizationId, String environmentId) {}

    /** @param publishedApisCount the distinct APIs published on at least one portal of the environment */
    public record Output(PortalQuota quota, int publishedApisCount) {}

    public Output execute(Input input) {
        var portals = portalCrudService.findByEnvironmentId(input.environmentId());
        return new Output(
            portalQuotaDomainService.quota(input.organizationId(), input.environmentId()),
            portalSummaryDomainService.countPublishedApis(input.environmentId(), portals)
        );
    }
}
