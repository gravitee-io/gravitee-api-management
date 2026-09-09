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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Before the catalog, the single form of an environment applied to all its APIs; it is now the
 * {@value #GLOBAL_DEFAULT_FORM_NAME} ({@link SubscriptionFormCatalogMongoUpgrader}), and a form only applies to the APIs
 * it is dedicated to. So that the APIs of an environment keep the form they were shown, an enabled Global Default Form
 * is dedicated to every API of its environment not already mapped to another form; a disabled one stays mapped to no
 * API. Mirrors the JDBC changeset {@code 4.13.0_23_dedicate_the_global_default_subscription_form_to_its_apis}.
 *
 * @author GraviteeSource Team
 */
@Component
public class SubscriptionFormGlobalDefaultMongoUpgrader extends MongoUpgrader {

    /** Runs once the catalog upgrader has named the legacy forms, and once definitions are back inline. */
    public static final int SUBSCRIPTION_FORM_GLOBAL_DEFAULT_MONGO_UPGRADER_ORDER =
        Math.max(
            SubscriptionFormCatalogMongoUpgrader.SUBSCRIPTION_FORM_CATALOG_MONGO_UPGRADER_ORDER,
            SubscriptionFormInlineContentMongoUpgrader.SUBSCRIPTION_FORM_INLINE_CONTENT_MONGO_UPGRADER_ORDER
        ) +
        1;

    static final String GLOBAL_DEFAULT_FORM_NAME = "Global Default Form";

    private static final String ENVIRONMENT_ID = "environmentId";
    private static final String NORMALIZED_NAME = "normalizedName";
    private static final String API_IDS = "apiIds";
    private static final Document ID_INDEX = new Document("_id", 1);

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var forms = this.getCollection("subscription_forms");
        var apis = this.getCollection("apis");

        // The name carries no index usable without the environment, and the databases these tests run against
        // forbid collection scans: walking the _id index visits every form without asking for one.
        var enabledGlobalDefaultForms = forms
            .find(Filters.and(Filters.eq(NORMALIZED_NAME, GLOBAL_DEFAULT_FORM_NAME.toLowerCase()), Filters.eq("enabled", true)))
            .hint(ID_INDEX);
        for (Document form : enabledGlobalDefaultForms) {
            var formId = form.get("_id");
            var environmentId = form.getString(ENVIRONMENT_ID);
            // Nothing serializes the upgraders across nodes: a form another node has already mapped is left alone.
            var notMappedYet = Filters.and(Filters.eq("_id", formId), Filters.or(Filters.exists(API_IDS, false), Filters.size(API_IDS, 0)));
            forms.updateOne(notMappedYet, Updates.set(API_IDS, apisNotMappedElsewhere(forms, apis, environmentId, formId)));
        }
        return true;
    }

    private static List<String> apisNotMappedElsewhere(
        MongoCollection<Document> forms,
        MongoCollection<Document> apis,
        String environmentId,
        Object formId
    ) {
        var mappedElsewhere = new HashSet<String>();
        forms
            .find(Filters.and(Filters.eq(ENVIRONMENT_ID, environmentId), Filters.ne("_id", formId)))
            .projection(Projections.include(API_IDS))
            .forEach(other -> mappedElsewhere.addAll(other.getList(API_IDS, String.class, List.of())));

        return StreamSupport.stream(
            apis.find(Filters.eq(ENVIRONMENT_ID, environmentId)).projection(Projections.include("_id")).spliterator(),
            false
        )
            .map(api -> api.getString("_id"))
            .filter(apiId -> !mappedElsewhere.contains(apiId))
            .toList();
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_GLOBAL_DEFAULT_MONGO_UPGRADER_ORDER;
    }
}
