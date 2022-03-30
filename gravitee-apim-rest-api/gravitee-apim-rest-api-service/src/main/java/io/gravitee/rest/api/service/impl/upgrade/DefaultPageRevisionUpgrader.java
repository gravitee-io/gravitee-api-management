/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultPageRevisionUpgrader implements Upgrader, Ordered {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(DefaultPageRevisionUpgrader.class);

    @Autowired
    private PageService pageService;

    @Autowired
    private PageRevisionService pageRevisionService;

    @Override
    public boolean upgrade(ExecutionContext executionContext) {
        if (hasNoRevisions()) {
            logger.info("No page revisions found. Create a default revision based on pages.");
            final int pageSize = 100;
            int pageNumber = 0;
            Page<PageEntity> pagesSubSet = null;
            do {
                Pageable pageable = new PageableImpl(pageNumber, pageSize);
                pagesSubSet = pageService.findAll(pageable);
                if (!pagesSubSet.getContent().isEmpty()) {
                    pagesSubSet
                        .getContent()
                        .stream()
                        .filter(entity -> pageService.shouldHaveRevision(entity.getType()))
                        .forEach(entity -> pageRevisionService.create(convert(entity)));
                    ++pageNumber;
                }
            } while (!pagesSubSet.getContent().isEmpty());
        }
        return true;
    }

    private boolean hasNoRevisions() {
        final io.gravitee.repository.management.api.search.Pageable pageable = new PageableBuilder().pageNumber(0).pageSize(1).build();
        return pageRevisionService.findAll(pageable).getContent().isEmpty();
    }

    private static io.gravitee.repository.management.model.Page convert(PageEntity pageEntity) {
        io.gravitee.repository.management.model.Page page = new io.gravitee.repository.management.model.Page();

        page.setId(pageEntity.getId());
        page.setName(pageEntity.getName());
        page.setType(pageEntity.getType());
        page.setContent(pageEntity.getContent());
        page.setLastContributor(pageEntity.getLastContributor());
        page.setCreatedAt(pageEntity.getLastModificationDate());
        page.setUpdatedAt(pageEntity.getLastModificationDate());

        return page;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
