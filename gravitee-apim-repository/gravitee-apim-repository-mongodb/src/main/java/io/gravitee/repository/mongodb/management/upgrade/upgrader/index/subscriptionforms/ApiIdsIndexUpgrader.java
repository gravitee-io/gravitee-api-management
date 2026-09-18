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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptionforms;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Enforces, at database level, that an API is mapped to at most one subscription form: a unique multikey index
 * on {@code apiIds} restricted to the documents mapping at least one API, so forms without a mapping never
 * collide on it. An API belongs to one environment, so the index does not need the environment. Mirrors the
 * JDBC {@code subscription_form_apis} unique constraint of changeset
 * {@code 4.13.0_20_add_subscription_form_apis_table}.
 *
 * @author GraviteeSource Team
 */
@Component("SubscriptionFormsApiIdsIndexUpgrader")
public class ApiIdsIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("subscription_forms")
            .name("ai1_unique")
            .key("apiIds", ascending())
            .unique(true)
            .partialFilterExpression(Filters.exists("apiIds.0"))
            .build();
    }
}
