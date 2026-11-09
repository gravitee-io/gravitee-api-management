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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.guardians;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Guardians are always looked up inside an environment, and listed or searched by name, so the
 * environment is the prefix every query shares.
 *
 * <p>Unique, because two Guardians of one environment may not share a name. It is a backstop, not the
 * enforcement: the rule users meet is the domain check, which refuses a duplicate with a 409 and
 * ignores case, while this index is exact and case-sensitive. It also only reaches a store that does
 * not already carry the non-unique version of it — {@link IndexUpgrader} skips a creation that
 * conflicts with an existing index of the same key pattern (error 85) rather than replacing it, so a
 * store created before this change keeps the index it has.
 *
 * @author GraviteeSource Team
 */
@Component("GuardianEnvironmentIdNameIndexUpgrader")
public class EnvironmentIdNameIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("guardians")
            .name("e1n1")
            .key("environmentId", ascending())
            .key("name", ascending())
            .unique(true)
            .build();
    }
}
