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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.themes;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.plan.PlanDefinitionVersionUpgrader;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Adds default value PORTAL to Themes if a type has not been denoted.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ThemeTypeUpgrader extends MongoUpgrader {

    public static final int THEME_TYPE_UPGRADER_ORDER = PlanDefinitionVersionUpgrader.PLAN_DEFINITION_VERSION_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var themeTypeNullQuery = new Document("type", null);

        template
            .getCollection("themes")
            .find(themeTypeNullQuery)
            .forEach(theme -> {
                theme.append("type", "PORTAL");
                template.getCollection("themes").replaceOne(Filters.eq("_id", theme.getString("_id")), theme);
            });
        return true;
    }

    @Override
    public int getOrder() {
        return THEME_TYPE_UPGRADER_ORDER;
    }
}
