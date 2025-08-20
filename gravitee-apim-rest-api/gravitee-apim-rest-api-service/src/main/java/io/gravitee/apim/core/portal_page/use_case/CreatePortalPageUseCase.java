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

import io.gravitee.apim.core.portal_page.domain_service.PortalService;
import io.gravitee.apim.core.portal_page.model.Entrypoint;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.Portal;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.UUID;

public class CreatePortalPageUseCase {

    private final PortalService portalService;

    public CreatePortalPageUseCase(PortalService portalService) {
        this.portalService = portalService;
    }

    public Output execute(Input input) {
        Portal portal = portalService.getPortal();
        PortalPage page = portal.create(input.pageContent);
        if (input.homepage) {
            page = portal.setEntrypointPage(Entrypoint.HOMEPAGE, page.id());
        }
        return new Output(page);
    }

    public record Input(UUID environmentId, boolean homepage, GraviteeMarkdown pageContent) {}

    public record Output(PortalPage page) {}
}
