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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.users;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("OrganizationIdSourceSourceIdUniqueIndexUpgrader")
public class OrganizationIdSourceSourceIdUniqueIndexUpgrader extends MongoUpgrader {

    private static final Logger LOG = LoggerFactory.getLogger(
        OrganizationIdSourceSourceIdUniqueIndexUpgrader.class
    );

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public boolean upgrade() {
        return createOrReplaceIndex();
    }

    private boolean createOrReplaceIndex() {
        final var usersCollection = this.getCollection("users");
        final var duplicates = searchDuplicateValues(usersCollection);

        if (duplicates.isEmpty()) {
            final var nonUniqueIndexName = "oi1s1si1";
            final var nonUniqueIndexExists = StreamSupport.stream(
                usersCollection.listIndexes().spliterator(),
                false
            ).anyMatch(index -> index.getString("name").equals(nonUniqueIndexName));
            if (nonUniqueIndexExists) {
                LOG.info(
                    "Non unique index found on triplet (organizationId, source, sourceId), dropping non unique index and replacing by unique one."
                );
                usersCollection.dropIndex(nonUniqueIndexName);
            }

            final var indexOptions = new IndexOptions()
                .unique(true)
                .name(nonUniqueIndexName + "_unique");
            usersCollection.createIndex(
                Indexes.ascending("organizationId", "source", "sourceId"),
                indexOptions
            );
        } else {
            LOG.warn(
                "Cannot create unique index on triplet (organizationId, source, sourceId): duplicate values found in collection."
            );
            for (final var entry : duplicates.entrySet()) {
                LOG.warn(
                    "Duplicate group: {}. Document IDs: {}",
                    entry.getKey(),
                    entry.getValue()
                );
            }
        }

        return true;
    }

    private Map<Object, Object> searchDuplicateValues(
        MongoCollection<Document> collection
    ) {
        final var duplicatesAggregated = collection.aggregate(
            Arrays.asList(
                Aggregates.group(
                    new Document("organizationId", "$organizationId")
                        .append("source", "$source")
                        .append("sourceId", "$sourceId"),
                    Accumulators.push("docs", "$_id"),
                    Accumulators.sum("count", 1)
                ),
                Aggregates.match(Filters.gt("count", 1))
            )
        );
        return StreamSupport.stream(duplicatesAggregated.spliterator(), false).collect(
            Collectors.toMap(doc -> doc.get("_id"), doc -> doc.get("docs"))
        );
    }
}
