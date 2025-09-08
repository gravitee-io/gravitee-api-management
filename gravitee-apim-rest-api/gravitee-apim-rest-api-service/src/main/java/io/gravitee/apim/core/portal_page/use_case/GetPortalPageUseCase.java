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
import io.gravitee.apim.core.portal_page.domain_service.CheckContextExistsDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PageExistsSpecification;
import io.gravitee.apim.core.portal_page.model.ExpandsViewContext;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.query_service.PortalPageQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalPageUseCase {

    private final CheckContextExistsDomainService checkContextExistsDomainService;
    private final PortalPageQueryService portalPageQueryService;

    public Output execute(Input input) {
        var spec = PageExistsSpecification.byPortalViewContext(ctx ->
            checkContextExistsDomainService.portalViewContextExists(input.environmentId(), ctx)
        );
        var context = input.pageType;
        spec.throwIfNotSatisfied(input.pageType);

        var pages = portalPageQueryService.findByEnvironmentIdAndContext(input.environmentId(), context, input.expands);
        return new Output(pages);
    }

    public record Output(List<PortalPageWithViewDetails> pages) {}

    public record Input(String environmentId, PortalViewContext pageType, List<ExpandsViewContext> expands) {}
}
