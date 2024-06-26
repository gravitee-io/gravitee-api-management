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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.repository.exceptions.TechnicalException;
import java.util.Collection;
import java.util.Set;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface CategoryDomainService {
    Set<String> toCategoryId(Api api, String environmentId);
    Set<String> toCategoryKey(Api api, String environmentId);

    void updateOrderCategoriesOfApi(String apiId, Collection<String> categoryIds);
}
