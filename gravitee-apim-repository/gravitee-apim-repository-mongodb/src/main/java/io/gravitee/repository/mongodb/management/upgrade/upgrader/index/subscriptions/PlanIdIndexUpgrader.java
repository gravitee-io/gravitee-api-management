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
 * Supports the gateway-sync warmup appender, which loads subscriptions filtered by a {@code plan IN (…)}
 * list and keyset-paginated by {@code (plan, _id)}. This compound index lets Mongo scan each plan's
 * range already ordered by {@code _id} — avoiding both the N-way merge of a {@code {plan,_id}} sort
 * over a wide {@code IN} and the {@code _id}-ordered collection walk that examines unrelated rows.
 */
@Component("SubscriptionsPlanIdIndexUpgrader")
public class PlanIdIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("subscriptions")
            .name("p1i1")
            .key("plan", ascending())
            .key("_id", ascending())
            .build();
    }
}
