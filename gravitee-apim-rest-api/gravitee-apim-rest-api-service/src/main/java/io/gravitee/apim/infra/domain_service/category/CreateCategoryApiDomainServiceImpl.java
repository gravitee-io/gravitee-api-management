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
package io.gravitee.apim.infra.domain_service.category;

import io.gravitee.apim.core.category.domain_service.CreateCategoryApiDomainService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCategoryApiDomainServiceImpl implements CreateCategoryApiDomainService {

    private final ApiCategoryService delegate;

    @Override
    public void addApiToCategories(String apiId, Set<String> categoryIds) {
        this.delegate.addApiToCategories(apiId, categoryIds);
    }
}
