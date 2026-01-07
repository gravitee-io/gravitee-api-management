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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class PageHRIDUpgrader implements Upgrader {

    private static final int PAGE_SIZE = 100;

    @Lazy
    @Autowired
    private PageRepository pageRepository;

    @Override
    public boolean upgrade() {
        int pageNumber = 0;
        long updatedCount = 0;

        try {
            while (true) {
                Pageable pageable = new PageableBuilder().pageNumber(pageNumber).pageSize(PAGE_SIZE).build();

                var pagedResult = pageRepository.findAll(pageable);
                var pages = pagedResult.getContent();

                if (pages.isEmpty()) {
                    break;
                }

                for (var page : pages) {
                    page.setHrid(page.getId());
                    pageRepository.update(page);
                    updatedCount++;
                    log.debug("Updated HRID for page {}", page.getId());
                }

                if (pages.size() < PAGE_SIZE) {
                    break;
                }

                pageNumber++;
            }
            log.info("Page HRID upgrade completed. Total pages updated: {}", updatedCount);
            return true;
        } catch (TechnicalException e) {
            log.error("Error during Page HRID upgrade", e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.PAGE_HRID_UPGRADER;
    }
}
