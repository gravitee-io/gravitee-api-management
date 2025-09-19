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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.domain_service.ContentSanitizedSpecification;
import io.gravitee.apim.core.portal_page.domain_service.PageExistsSpecification;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePortalPageUseCase {

    private final PortalPageCrudService portalPageCrudService;
    private final PortalPageContextCrudService portalPageContextCrudService;

    public record Input(String environmentId, String pageId, String content) {}

    public record Output(PortalPageWithViewDetails portalPage) {}

    public Output execute(Input input) {
        PageId pageId = PageId.of(input.pageId);
        var portalPageOpt = portalPageCrudService.findById(pageId);
        var pageExistsSpec = PageExistsSpecification.ofOptional();
        var contentSanitizedSpec = new ContentSanitizedSpecification();

        GraviteeMarkdown pageContent = new GraviteeMarkdown(input.content);

        pageExistsSpec.throwIfNotSatisfied(portalPageOpt);
        contentSanitizedSpec.throwIfNotSatisfied(pageContent);

        var portalPage = portalPageOpt.get();
        portalPage.setContent(pageContent);

        var updated = portalPageCrudService.update(portalPage);

        var withDetails = new PortalPageWithViewDetails(updated, portalPageContextCrudService.findByPageId(pageId));

        return new Output(withDetails);
    }
}
