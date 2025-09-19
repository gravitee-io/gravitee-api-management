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
package fixtures;

import io.gravitee.rest.api.model.AccessControlEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageMediaEntity;
import io.gravitee.rest.api.model.Visibility;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageModelFixtures {

    private PageModelFixtures() {}

    private static final PageEntity.PageEntityBuilder BASE_MODEL_PAGE = PageEntity.builder()
        .id("page-id")
        .homepage(true)
        .crossId("page-cross-id")
        .accessControls(Set.of(AccessControlEntity.builder().referenceId("ref-id").referenceType("ref-type").build()))
        .attachedMedia(
            List.of(
                PageMediaEntity.builder()
                    .mediaName("media-name")
                    .mediaHash("media-hash")
                    .attachedAt(Date.from(Instant.parse("2020-01-01T00:00:00.00Z")))
                    .build()
            )
        )
        .excludedGroups(List.of("group1"))
        .configuration(Map.of("key", "value"))
        .content("#content")
        .generalConditions(true)
        .lastContributor("last-contributor")
        .type("MARKDOWN")
        .visibility(Visibility.PUBLIC)
        .order(1);

    public static PageEntity aModelPage() {
        return BASE_MODEL_PAGE.build();
    }
}
