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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.apicategories;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.environment.MissingEnvironmentUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.plan.PlanDefinitionVersionUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.themes.ThemeTypeUpgrader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Creates api_categories collection so that apis can be ordered within a category
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiCategoryUpgrader extends MongoUpgrader {

    public static final int API_CATEGORY_UPGRADER_ORDER = MissingEnvironmentUpgrader.MISSING_ENVIRONMENT_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var categoryIdKeyMap = new HashMap<String, String>();
        var environmentCategoryKeyMap = new HashMap<String, Map<String, String>>();

        template
            .getCollection("categories")
            .find()
            .forEach(category -> {
                var categoryId = category.getString("_id");
                var categoryKey = category.getString("key");
                var environmentId = category.getString("environmentId");

                categoryIdKeyMap.put(categoryId, categoryKey);
                environmentCategoryKeyMap.computeIfPresent(
                    environmentId,
                    (envId, catMap) -> {
                        catMap.put(categoryKey, categoryId);
                        return catMap;
                    }
                );
                environmentCategoryKeyMap.computeIfAbsent(
                    environmentId,
                    key -> {
                        var map = new HashMap<String, String>();
                        map.put(categoryKey, categoryId);
                        return map;
                    }
                );
            });

        var categoryIdApiIdListMap = new HashMap<String, List<String>>();
        var apiHasCategoriesQuery = new Document("categories.0", new Document("$exists", true));
        var projection = Projections.fields(Projections.include("_id", "environmentId", "categories"));

        template
            .getCollection("apis")
            .find(apiHasCategoriesQuery)
            .projection(projection)
            .forEach(api -> {
                var apiId = api.getString("_id");
                var environmentId = api.getString("environmentId");

                api
                    .getList("categories", String.class)
                    .forEach(category -> {
                        var categoryId = categoryIdKeyMap.containsKey(category)
                            ? category
                            : environmentCategoryKeyMap.get(environmentId).get(category);

                        categoryIdApiIdListMap.computeIfPresent(
                            categoryId,
                            (catId, apiIdList) -> {
                                apiIdList.add(apiId);
                                return apiIdList;
                            }
                        );
                        categoryIdApiIdListMap.computeIfAbsent(categoryId, catId -> Arrays.asList(apiId));
                    });
            });

        var bulkActions = new ArrayList<Document>();
        categoryIdApiIdListMap.forEach((categoryId, apiIdList) -> {
            for (int order = 0; order < apiIdList.size(); order++) {
                bulkActions.add(
                    new Document()
                        .append("_id", new Document(Map.of("categoryId", categoryId, "apiId", apiIdList.get(order))))
                        .append("categoryKey", categoryIdKeyMap.get(categoryId))
                        .append("order", order)
                );
            }
        });

        template.getCollection("api_categories").insertMany(bulkActions).wasAcknowledged();

        template.getCollection("apis").updateMany(new BsonDocument(), Updates.unset("categories"));

        return true;
    }

    @Override
    public int getOrder() {
        return API_CATEGORY_UPGRADER_ORDER;
    }
}
