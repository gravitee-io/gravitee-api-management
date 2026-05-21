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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptions;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Compound index {@code {status:1, endingAt:1}} on the {@code subscriptions} collection.
 *
 * <p>Serves the cron-driven pre-expiration and close-expired scheduler queries, both of which match
 * subscriptions by status (equality / {@code $in}) and constrain {@code endingAt} to a range
 * (typically a 1-hour cron-tick window).</p>
 *
 * <p>The field ordering follows MongoDB's Equality-Sort-Range (ESR) rule. Status (equality) MUST
 * come first; endingAt (range) second. Reordering — for example inserting another field between
 * status and endingAt — will silently regress to a collection-style scan since the planner can no
 * longer use the index for the range predicate. Name {@code s1ea1} encodes the index shape:
 * status asc, endingAt asc.</p>
 *
 * <p>See APIM-14086 for the original ESR violation this index resolves.</p>
 */
@Component("StatusEndingAtIndexUpgrader")
public class StatusEndingAtIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("subscriptions")
            .name("s1ea1")
            .key("status", ascending())
            .key("endingAt", ascending())
            .build();
    }
}
