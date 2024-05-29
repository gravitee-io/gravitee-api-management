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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.apis;

import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Component;

/*
 * Updates the 'categories' attribute in the 'apis' collection from category key to category id values found in the 'categories' collection.
 * If category key is not found in the category table - it's left as is.
 * This is needed because some of the catogories might already have ids in the 'apis' table.
 */

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ApisCategoriesUpgrader extends MongoUpgrader {

    public static final int APIS_CATEGORY_VERSION_UPGRADER_ORDER = ThemeTypeUpgrader.THEME_TYPE_UPGRADER_ORDER + 1;
    private static final int BATCH_SIZE = 100;

    @Override
    public boolean upgrade() {
        var categoriesMap = getCategoriesMap();
        var bulkActions = new ArrayList<UpdateOneModel<Document>>();
        var projection = Projections.fields(Projections.include("_id", "categories"));

        var apisCollection = template.getCollection("apis");

        try {
            for (Document api : apisCollection.find().projection(projection).batchSize(BATCH_SIZE)) {
                List<String> categoryKeys = api.getList("categories", String.class);

                if (categoryKeys != null && !categoryKeys.isEmpty()) {
                    List<String> updatedCategories = categoryKeys
                        .stream()
                        .map(key -> categoriesMap.getOrDefault(key, key))
                        .collect(Collectors.toList());

                    bulkActions.add(
                        new UpdateOneModel<>(
                            new Document("_id", api.getString("_id")),
                            new Document("$set", new Document("categories", updatedCategories))
                        )
                    );
                }

                if (bulkActions.size() >= BATCH_SIZE) {
                    try {
                        template.getCollection("apis").bulkWrite(bulkActions);
                        bulkActions.clear();
                    } catch (Exception e) {
                        log.error("Error during bulk write operation", e);
                        return false;
                    }
                }
            }

            if (!bulkActions.isEmpty()) {
                try {
                    template.getCollection("apis").bulkWrite(bulkActions);
                } catch (Exception e) {
                    log.error("Error during bulk write operation", e);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error during API categories upgrade", e);
            return false;
        }

        return true;
    }

    private Map<String, String> getCategoriesMap() {
        Map<String, String> categoriesMap = new HashMap<>();
        template
            .getCollection("categories")
            .find()
            .forEach(category -> {
                String key = category.getString("key");
                String id = category.getString("_id");
                categoriesMap.put(key, id);
            });
        return categoriesMap;
    }

    @Override
    public int getOrder() {
        return APIS_CATEGORY_VERSION_UPGRADER_ORDER;
    }
}
