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
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Enforces, at database level, that an application has at most one active subscription to a plan, or to a channel of a
 * PUSH plan: a unique index on the hash of the unique active key, which only active subscriptions carry. It is the only
 * protection against identical subscription requests processed at the same time, by one or several nodes.
 * <p>
 * No subscription has the hash yet when the index is created, before the node writes any: building the index neither
 * reads nor fails on the existing subscriptions. Duplicates created before are left as they are.
 * <p>
 * Like the other index upgraders, it stops the node when the index cannot be created, and is tried again at the next
 * startup: without the index, two identical requests could store the same hash, and the index could not be created
 * anymore.
 *
 * @author GraviteeSource Team
 */
@Component("SubscriptionsActiveKeyHashIndexUpgrader")
public class ActiveKeyHashIndexUpgrader extends IndexUpgrader {

    public static final String NAME = "akh1_unique";
    public static final String FIELD = "activeKeyHash";

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("subscriptions")
            .name(NAME)
            .key(FIELD, ascending())
            .unique(true)
            .partialFilterExpression(new Document(FIELD, new Document("$exists", true)))
            .build();
    }
}
