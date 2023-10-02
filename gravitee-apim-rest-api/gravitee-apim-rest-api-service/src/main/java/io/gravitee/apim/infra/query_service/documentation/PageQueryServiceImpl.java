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
package io.gravitee.apim.infra.query_service.documentation;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.PageReferenceType;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PageQueryServiceImpl implements PageQueryService {

    private final PageRepository pageRepository;

    public PageQueryServiceImpl(@Lazy final PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    @Override
    public List<Page> searchByApiId(String apiId) {
        PageCriteria criteria = new PageCriteria.Builder().referenceType(PageReferenceType.API.name()).referenceId(apiId).build();
        try {
            return PageAdapter.INSTANCE.toEntityList(pageRepository.search(criteria));
        } catch (TechnicalException e) {
            throw new RuntimeException(e);
        }
    }
}
