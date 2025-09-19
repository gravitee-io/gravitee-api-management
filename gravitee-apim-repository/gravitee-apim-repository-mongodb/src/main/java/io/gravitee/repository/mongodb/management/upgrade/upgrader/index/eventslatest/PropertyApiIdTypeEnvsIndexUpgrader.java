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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.eventslatest;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("EventsLatestPropertyApiIdTypeEnvsIndexUpgrader")
public class PropertyApiIdTypeEnvsIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        // Warn: Mongodb doesn't support multikey index if more than one to-be-indexed field of a document is an array.
        // Thus, it is not possible to index both 'environments' and 'organizations' fields as they are both arrays.
        // We prefer index the 'environments' field as it may have more cardinalities than the 'organizations' one.
        return Index.builder()
            .collection("events_latest")
            .name("pai1t1e1")
            .key("properties.api_id", ascending())
            .key("type", ascending())
            .key("environments", ascending())
            .build();
    }
}
