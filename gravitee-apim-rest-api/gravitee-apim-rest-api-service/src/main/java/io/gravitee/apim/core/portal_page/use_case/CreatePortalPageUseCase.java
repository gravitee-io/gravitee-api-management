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
import io.gravitee.apim.core.portal_page.domain_service.ContentSanitizedSpecification;
import io.gravitee.apim.core.portal_page.domain_service.LocatorUniqueSpecification;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageLocator;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.UUID;

public class CreatePortalPageUseCase {

    private final PortalPageCrudService crudService;

    public CreatePortalPageUseCase(PortalPageCrudService crudService) {
        this.crudService = crudService;
    }

    public Output execute(Input input) {
        PageLocator locator = new PageLocator(input.homepage);
        new LocatorUniqueSpecification(l -> !crudService.locatorExists(l)).throwIfNotSatisfied(locator);
        ContentSanitizedSpecification contentSpec = new ContentSanitizedSpecification();
        PortalPage page = PortalPage.create(locator, input.pageContent);
        contentSpec.throwIfNotSatisfied(page);
        PortalPage created = crudService.create(page);
        return new Output(created);
    }

    public record Input(UUID environmentId, boolean homepage, GraviteeMarkdown pageContent) {}

    public record Output(PortalPage page) {}
}
