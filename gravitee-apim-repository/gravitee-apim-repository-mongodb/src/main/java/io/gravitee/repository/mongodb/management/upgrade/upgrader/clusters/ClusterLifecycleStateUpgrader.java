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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.clusters;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import org.springframework.stereotype.Component;

/**
 * Backfills the cluster {@code lifecycleState} for documents created before the field existed. The read
 * model coerces a missing/null value to {@code UNDEPLOYED}, but the server-side lifecycleState filter and
 * the {@code /clusters/_stats} counts query the field directly — so a null document would neither match
 * the Undeployed facet nor be counted. Setting it makes the pushdown consistent with the read model.
 */
@Component
public class ClusterLifecycleStateUpgrader extends MongoUpgrader {

    public static final int CLUSTER_LIFECYCLE_STATE_UPGRADER_ORDER = ThemeTypeUpgrader.THEME_TYPE_UPGRADER_ORDER + 1;

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        // Filters.eq(field, null) matches documents where the field is null or absent.
        getCollection("clusters").updateMany(Filters.eq("lifecycleState", null), Updates.set("lifecycleState", "UNDEPLOYED"));
        return true;
    }

    @Override
    public int getOrder() {
        return CLUSTER_LIFECYCLE_STATE_UPGRADER_ORDER;
    }
}
