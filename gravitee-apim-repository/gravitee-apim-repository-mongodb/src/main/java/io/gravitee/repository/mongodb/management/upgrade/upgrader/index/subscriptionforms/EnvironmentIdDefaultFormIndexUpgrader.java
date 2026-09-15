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
 * Enforces, at database level, that an environment has at most one default subscription form: a unique index
 * on {@code (environmentId, defaultForm)} restricted to the documents where {@code defaultForm} is true, so
 * the other forms of the environment never collide on it. Mirrors the JDBC {@code default_marker} unique
 * index of changeset {@code 4.13.0_19_add_catalog_columns_to_subscription_forms}.
 *
 * @author GraviteeSource Team
 */
@Component("SubscriptionFormsEnvironmentIdDefaultFormIndexUpgrader")
public class EnvironmentIdDefaultFormIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("subscription_forms")
            .name("ei1df1_unique")
            .key("environmentId", ascending())
            .key("defaultForm", ascending())
            .unique(true)
            .partialFilterExpression(Filters.eq("defaultForm", true))
            .build();
    }
}
