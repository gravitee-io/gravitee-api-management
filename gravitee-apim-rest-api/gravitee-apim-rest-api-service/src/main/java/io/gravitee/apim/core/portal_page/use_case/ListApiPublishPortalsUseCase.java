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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Lists the portals an API can be published to. Callers hold a permission on one API, not on the environment,
 * so the output is deliberately limited to what is needed to pick a portal.
 */
@UseCase
@RequiredArgsConstructor
public class ListApiPublishPortalsUseCase {

    private final PortalCrudService portalCrudService;

    public Output execute(Input input) {
        var portals = portalCrudService
            .findByEnvironmentId(input.environmentId())
            .stream()
            .sorted(Portal.DEFAULT_FIRST_THEN_BY_NAME)
            .map(portal -> new PublishPortal(portal.getId(), portal.getName(), portal.isDefault()))
            .toList();
        return new Output(portals);
    }

    public record Input(String environmentId) {}

    public record Output(List<PublishPortal> portals) {}

    public record PublishPortal(PortalId id, String name, boolean isDefault) {}
}
