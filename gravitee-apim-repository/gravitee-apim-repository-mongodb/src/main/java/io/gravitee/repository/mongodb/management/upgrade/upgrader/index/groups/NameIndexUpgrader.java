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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.groups;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("GroupsNameIndexUpgrader")
public class NameIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("groups")
            .name("name_1")
            .key("name", ascending())
            .build();
    }

    /**
     * Run after the collation index name_collation_1 (GroupsEnvironmentIdCollationIndexUpgrader) on the same key pattern. On Amazon DocumentDB
     * only one index per key pattern can exist: the collation one must be created first because it is the one used by
     * the case-insensitive queries, this one is then skipped (see IndexUpgrader).
     */
    @Override
    public int getOrder() {
        return 1;
    }
}
