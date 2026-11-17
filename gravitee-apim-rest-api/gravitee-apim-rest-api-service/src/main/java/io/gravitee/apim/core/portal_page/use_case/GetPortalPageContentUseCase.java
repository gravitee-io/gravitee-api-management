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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalPageContentUseCase {

    private final PortalNavigationAccessDomainService accessService;

    public Output execute(Input input) {
        return new Output(
            accessService.requireContent(input.organizationId(), input.environmentId(), input.scope(), input.portalPageContentId())
        );
    }

    public record Output(PortalPageContent<?> content) {}

    public record Input(
        String organizationId,
        String environmentId,
        PortalPageContentId portalPageContentId,
        NavigationItemReference scope
    ) {
        public Input {
            Objects.requireNonNull(scope, "scope");
        }
    }
}
