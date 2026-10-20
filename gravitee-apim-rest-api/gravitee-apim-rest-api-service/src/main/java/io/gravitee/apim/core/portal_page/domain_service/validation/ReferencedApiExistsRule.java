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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference.ApiReference;
import lombok.RequiredArgsConstructor;

/**
 * An item can only be stored against an API that exists in the environment it is created in. An API of
 * another environment is reported as missing, like one that was never created.
 *
 * Automation is left out: it validates the documentation of an API before that API is created, and resolves
 * the API itself when it applies it.
 */
@RequiredArgsConstructor
public class ReferencedApiExistsRule implements CreatePortalNavigationItemValidationRule {

    private final ApiCrudService apiCrudService;

    @Override
    public boolean appliesTo(CreatePortalNavigationItem item) {
        return item.getReference() instanceof ApiReference && item.getAutomationMetadata() == null;
    }

    @Override
    public void validate(CreatePortalNavigationItem item, String environmentId, CreateValidationContext ctx) {
        var apiId = ((ApiReference) item.getReference()).apiId();
        apiCrudService
            .findById(apiId)
            .filter(api -> api.belongsToEnvironment(environmentId))
            .orElseThrow(() -> new ApiNotFoundException(apiId));
    }
}
