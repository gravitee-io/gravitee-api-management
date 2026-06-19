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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.pagerevisions;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Compound index on {@code _id.pageId} (ascending) and {@code _id.revision} (descending) to
 * cover the {@code findLastByPageId} query: filter on {@code _id.pageId} + sort by
 * {@code _id.revision DESC} + limit 1.
 *
 * <p>Without this index MongoDB falls back to a COLLSCAN across the entire {@code page_revisions}
 * collection for every call, which becomes a severe bottleneck when the endpoint that triggers
 * these look-ups is called frequently (e.g. {@code GET /applications}).
 *
 * @author GraviteeSource Team
 */
@Component("PageRevisionsPageRevisionIndexUpgrader")
public class PageRevisionIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("page_revisions")
            .name("pi1r-1")
            .key("_id.pageId", ascending())
            .key("_id.revision", descending())
            .build();
    }
}
