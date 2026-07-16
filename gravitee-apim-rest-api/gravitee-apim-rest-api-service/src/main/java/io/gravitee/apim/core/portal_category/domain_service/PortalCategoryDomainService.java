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
package io.gravitee.apim.core.portal_category.domain_service;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;

/**
 * Holds the validation and lookup rules shared by the create/update/delete use cases:
 * non-blank title and environment-scoped not-found semantics.
 *
 * @author GraviteeSource Team
 */
public interface PortalCategoryDomainService {
    void validateTitle(String title);

    PortalCategory findByIdAndEnvironmentId(String environmentId, PortalCategoryId id);
}
