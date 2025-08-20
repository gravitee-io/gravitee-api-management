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
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.Portal;
import io.gravitee.apim.core.portal_page.model.PortalPage;

public class SetHomepageUseCase {

    private final PortalService portalService;

    public SetHomepageUseCase(PortalService portalService) {
        this.portalService = portalService;
    }

    public Output execute(Input input) {
        Portal portal = portalService.getPortal();
        PortalPage updatedPage = portal.setEntrypointPage(Entrypoint.HOMEPAGE, input.pageId);
        return new Output(updatedPage);
    }

    public record Input(PageId pageId) {}

    public record Output(PortalPage page) {}
}
