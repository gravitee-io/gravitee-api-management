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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails;

import com.mongodb.MongoNamespace;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.subscriptionform.SubscriptionFormGlobalDefaultMongoUpgrader;
import java.util.stream.StreamSupport;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

/**
 * Moves the Guardians, renamed Guardrails, to their new names: the {@code guardians} collection becomes
 * {@code guardrails}, and the {@code guardians} an API runs become its {@code guardrails}.
 *
 * <p>The index upgraders run first, so the {@code guardrails} collection usually exists already, empty, holding
 * only the indexes they created. It is dropped to make room for the renamed one, which carries the same indexes.
 * A {@code guardrails} collection holding documents is left alone, with the {@code guardians} one, rather than lose
 * either.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class GuardrailRenameMongoUpgrader extends MongoUpgrader {

    public static final int GUARDRAIL_RENAME_MONGO_UPGRADER_ORDER =
        SubscriptionFormGlobalDefaultMongoUpgrader.SUBSCRIPTION_FORM_GLOBAL_DEFAULT_MONGO_UPGRADER_ORDER + 1;

    static final String LEGACY_COLLECTION = "guardians";
    static final String COLLECTION = "guardrails";
    static final String LEGACY_API_FIELD = "guardians";
    static final String API_FIELD = "guardrails";
    static final String LEGACY_API_INDEX = "gu1";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            renameCollection();
            renameApiField();
            return true;
        });
    }

    private void renameCollection() {
        String legacy = buildCollectionName(LEGACY_COLLECTION);
        String target = buildCollectionName(COLLECTION);
        if (!template.collectionExists(legacy)) {
            return;
        }
        if (template.collectionExists(target)) {
            if (template.getCollection(target).estimatedDocumentCount() > 0) {
                log.warn("Both '{}' and '{}' hold documents, skipping the rename to avoid data loss", legacy, target);
                return;
            }
            template.getCollection(target).drop();
        }
        log.info("Renaming the collection '{}' to '{}'", legacy, target);
        template.getCollection(legacy).renameCollection(new MongoNamespace(template.getDb().getName(), target));
    }

    private void renameApiField() {
        var apis = getCollection("apis");
        boolean indexed = StreamSupport.stream(apis.listIndexes().spliterator(), false).anyMatch(index ->
            LEGACY_API_INDEX.equals(index.getString("name"))
        );
        if (!indexed) {
            // the field only ever existed alongside its index
            return;
        }
        // through the legacy index: the databases the tests run against forbid collection scans
        apis.updateMany(
            Filters.exists(LEGACY_API_FIELD),
            Updates.rename(LEGACY_API_FIELD, API_FIELD),
            new UpdateOptions().hintString(LEGACY_API_INDEX)
        );
        apis.dropIndex(LEGACY_API_INDEX);
    }

    @Override
    public int getOrder() {
        return GUARDRAIL_RENAME_MONGO_UPGRADER_ORDER;
    }
}
