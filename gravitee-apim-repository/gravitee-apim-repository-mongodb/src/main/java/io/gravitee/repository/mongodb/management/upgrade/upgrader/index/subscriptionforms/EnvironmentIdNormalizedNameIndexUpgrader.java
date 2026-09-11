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

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.springframework.stereotype.Component;

/**
 * Enforces, at database level, that two subscription forms of an environment never share a name: a unique index
 * on {@code (environmentId, normalizedName)}, the name trimmed and lowercased by the repository, so a rename or
 * a creation racing with another one cannot slip past the check the use cases make beforehand. Mirrors the JDBC
 * {@code uc_subscription_forms_normalized_name} constraint of changeset
 * {@code 4.13.0_20_add_normalized_name_to_subscription_forms}.
 *
 * @author GraviteeSource Team
 */
@Component("SubscriptionFormsEnvironmentIdNormalizedNameIndexUpgrader")
public class EnvironmentIdNormalizedNameIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        return Index.builder()
            .collection("subscription_forms")
            .name("ei1nn1_unique")
            .key("environmentId", ascending())
            .key("normalizedName", ascending())
            .unique(true)
            .build();
    }
}
