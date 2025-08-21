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

import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PageExistsSpecification;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;

public class GetHomepageUseCase {

    private final PortalPageCrudService crudService;

    public GetHomepageUseCase(PortalPageCrudService crudService) {
        this.crudService = crudService;
    }

    public Output execute() {
        var spec = PageExistsSpecification.byEntrypoint(crudService::portalViewContextExists);
        spec.throwIfNotSatisfied(PortalViewContext.HOMEPAGE);
        PortalPage page = crudService.byPortalViewContext(PortalViewContext.HOMEPAGE);
        return new Output(page);
    }

    public record Output(PortalPage page) {}
}
