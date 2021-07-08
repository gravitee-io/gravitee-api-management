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
package io.gravitee.repository.jdbc.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.Pageable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class JdbcAbstractPageableRepository<T> extends JdbcAbstractRepository<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAbstractPageableRepository.class);

    JdbcAbstractPageableRepository(String prefix, String tableName) {
        super(prefix, tableName);
    }

    Page<T> getResultAsPage(final Pageable page, final List<T> items) {
        if (page != null) {
            LOGGER.debug("Getting results as page {} for {}", page, items);
            int start = page.from();
            if ((start == 0) && (page.pageNumber() > 0)) {
                start = page.pageNumber() * page.pageSize();
            }
            int rows = page.pageSize();
            if ((rows == 0) && (page.to() > 0)) {
                rows = page.to() - start;
            }
            if (start + rows > items.size()) {
                rows = items.size() - start;
            }
            return new Page<>(items.subList(start, start + rows), start / page.pageSize(), rows, items.size());
        }
        return new Page<>(items, 0, items.size(), items.size());
    }
}
