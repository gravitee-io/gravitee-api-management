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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.applications;

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enforces, at database level, that a client_id is used by at most one active application per environment.
 * <p>
 * Applications sharing a client_id cannot be arbitrated here: both may be serving traffic, and the losing one would
 * have its subscriptions closed. They are reported instead, and the index is left out until an operator resolves them.
 *
 * @author GraviteeSource Team
 */
@Component("ApplicationsClientIdUniqueIndexUpgrader")
public class ClientIdUniqueIndexUpgrader extends MongoUpgrader {

    public static final String INDEX_NAME = "ei1mci1_unique";

    private static final String CLIENT_ID_FIELD = "metadata." + METADATA_CLIENT_ID;

    private static final Logger LOG = LoggerFactory.getLogger(
        ClientIdUniqueIndexUpgrader.class
    );

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * Never reports a failure: the node stops when an upgrader fails, and a missing index only means keeping the
     * behaviour of the previous versions. Run it again by removing its record from the `upgrades` collection.
     */
    @Override
    public boolean upgrade() {
        try {
            createIndex();
        } catch (Exception e) {
            LOG.error(
                "Unexpected error while creating index {} on applications, client_id uniqueness stays unenforced",
                INDEX_NAME,
                e
            );
        }
        return true;
    }

    private void createIndex() {
        final var applicationsCollection = this.getCollection("applications");
        final var duplicates = searchDuplicateClientIds(applicationsCollection);

        if (!duplicates.isEmpty()) {
            LOG.warn(
                "Index {} has not been created on applications: {} client_id(s) are shared by several active " +
                    "applications, and client_id uniqueness stays unenforced until they are resolved.",
                INDEX_NAME,
                duplicates.size()
            );
            duplicates.forEach(duplicate -> {
                Document group = duplicate.get("_id", Document.class);
                LOG.warn(
                    "Duplicated client_id [{}] in environment [{}] used by applications {}",
                    group.getString(METADATA_CLIENT_ID),
                    group.getString("environmentId"),
                    duplicate.get("applicationIds")
                );
            });
            return;
        }

        applicationsCollection.createIndex(
            Indexes.ascending("environmentId", CLIENT_ID_FIELD),
            new IndexOptions()
                .name(INDEX_NAME)
                .unique(true)
                .partialFilterExpression(activeApplicationsHavingAClientId())
        );
        LOG.info("Index {} has been created successfully on applications", INDEX_NAME);
    }

    private List<Document> searchDuplicateClientIds(
        MongoCollection<Document> collection
    ) {
        final var duplicatesAggregated = collection.aggregate(
            Arrays.asList(
                Aggregates.match(activeApplicationsHavingAClientId()),
                Aggregates.group(
                    new Document("environmentId", "$environmentId").append(
                        METADATA_CLIENT_ID,
                        "$" + CLIENT_ID_FIELD
                    ),
                    Accumulators.push("applicationIds", "$_id"),
                    Accumulators.sum("count", 1)
                ),
                Aggregates.match(Filters.gt("count", 1))
            )
        );
        return StreamSupport.stream(duplicatesAggregated.spliterator(), false).toList();
    }

    private static Bson activeApplicationsHavingAClientId() {
        return Filters.and(
            Filters.eq("status", ApplicationStatus.ACTIVE.name()),
            Filters.exists(CLIENT_ID_FIELD)
        );
    }
}
