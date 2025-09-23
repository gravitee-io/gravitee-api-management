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
import io.gravitee.apim.core.portal_page.domain_service.PageExistsSpecification;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.query_service.PortalPageQueryService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePortalPageViewPublicationStatusUseCase {

    private final PortalPageContextCrudService portalPageContextCrudService;
    private final PortalPageQueryService portalPageQueryService;

    public Output execute(Input input) {
        var portalPageView = portalPageContextCrudService.findByPageId(input.pageId());
        var pageExistsSpec = new PageExistsSpecification<PortalPageView>(Objects::nonNull);

        pageExistsSpec.throwIfNotSatisfied(portalPageView);

        PortalPageView toUpdate;
        if (input.published()) {
            toUpdate = portalPageView.publish();
        } else {
            toUpdate = portalPageView.unpublish();
        }

        var updatedDetails = portalPageContextCrudService.update(input.pageId(), toUpdate);

        return new Output(portalPageQueryService.loadContentFor(input.pageId(), updatedDetails));
    }

    public record Input(PageId pageId, boolean published) {}

    public record Output(PortalPageWithViewDetails portalPage) {}
}
