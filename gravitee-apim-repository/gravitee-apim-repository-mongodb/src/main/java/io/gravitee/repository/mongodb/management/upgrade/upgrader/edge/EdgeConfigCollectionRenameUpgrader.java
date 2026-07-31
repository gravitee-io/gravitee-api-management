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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.edge;

import com.mongodb.MongoNamespace;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.plan.PlanReferenceTypeUpgrader;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

/**
 * Renames the edge configuration collection from its former hard-coded name {@code aim_module_edge_config}
 * to the prefix-aware name {@code <management.mongodb.prefix>edge_config} used by
 * {@code com.graviteesource.gamma.module.edge.infra.repository.EdgeConfigurationDocument}.
 *
 * <p>The migration is idempotent: it renames only when the legacy collection exists and the target one does not.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class EdgeConfigCollectionRenameUpgrader extends MongoUpgrader {

    public static final int EDGE_CONFIG_COLLECTION_RENAME_UPGRADER_ORDER = PlanReferenceTypeUpgrader.PLAN_REFERENCE_TYPE_UPGRADER_ORDER + 1;

    /** Former collection name, persisted without the management prefix. */
    static final String LEGACY_COLLECTION_NAME = "aim_module_edge_config";

    /** New collection name, to be combined with the management prefix. */
    static final String EDGE_CONFIG_COLLECTION_NAME = "edge_config";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            final String targetCollectionName = buildCollectionName(EDGE_CONFIG_COLLECTION_NAME);

            if (!template.collectionExists(LEGACY_COLLECTION_NAME)) {
                log.debug("Legacy collection '{}' not found, nothing to migrate", LEGACY_COLLECTION_NAME);
                return true;
            }

            if (template.collectionExists(targetCollectionName)) {
                log.warn(
                    "Both legacy collection '{}' and target collection '{}' exist, skipping rename to avoid data loss",
                    LEGACY_COLLECTION_NAME,
                    targetCollectionName
                );
                return true;
            }

            log.info("Renaming edge configuration collection '{}' to '{}'", LEGACY_COLLECTION_NAME, targetCollectionName);
            var legacyCollection = template.getCollection(LEGACY_COLLECTION_NAME);
            var targetNamespace = new MongoNamespace(template.getDb().getName(), targetCollectionName);
            legacyCollection.renameCollection(targetNamespace);
            return true;
        });
    }

    @Override
    public int getOrder() {
        return EDGE_CONFIG_COLLECTION_RENAME_UPGRADER_ORDER;
    }
}
