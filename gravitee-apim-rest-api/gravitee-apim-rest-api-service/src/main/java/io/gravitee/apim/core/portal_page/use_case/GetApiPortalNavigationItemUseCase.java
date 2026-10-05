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
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import lombok.RequiredArgsConstructor;

/**
 * Reads one item of an API's documentation for whoever manages that API. Unlike
 * {@link GetPortalNavigationItemUseCase}, no viewer visibility applies: a draft of an API that is not
 * listed anywhere must still be readable.
 */
@UseCase
@RequiredArgsConstructor
public class GetApiPortalNavigationItemUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;

    public Output execute(Input input) {
        var item = apiOwnedNavigationDomainService.requireOwnedItem(input.environmentId(), input.apiId(), input.itemId());
        if (item.getSource() != null) {
            sourceDomainService.removeSensitiveData(item.getSource());
        }
        return new Output(item);
    }

    public record Input(String environmentId, String apiId, PortalNavigationItemId itemId) {}

    public record Output(PortalNavigationItem item) {}
}
