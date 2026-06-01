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

import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateDefaultPortalUseCase {

    static final String DEFAULT_PORTAL_HRID = "default-portal";
    static final String DEFAULT_PORTAL_NAME = "Default Portal";

    private final PortalCrudService portalCrudService;

    public void execute(String organizationId, String environmentId) {
        var portalId = PortalId.of(
            HRIDToUUID.portal().context(new ExecutionContext(organizationId, environmentId)).hrid(DEFAULT_PORTAL_HRID).id()
        );
        if (portalCrudService.findByIdAndEnvironmentId(portalId, environmentId).isPresent()) {
            return;
        }
        portalCrudService.create(Portal.of(portalId, environmentId, organizationId, DEFAULT_PORTAL_NAME));
    }
}
