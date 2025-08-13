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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.groups;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.environment.MissingEnvironmentUpgrader;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonNull;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Remove any remaining deleted groups from APIs.
 */
@Component
@Slf4j
public class RemoveDeletedGroupsFromApisUpgrader extends MongoUpgrader {

    public static final int REMOVE_DELETED_GROUPS_UPGRADER_ORDER = MissingEnvironmentUpgrader.MISSING_ENVIRONMENT_UPGRADER_ORDER + 1;

    private static final String ATTR_GROUPS = "groups";
    private static final String ATTR_ID = "_id";
    private static final String DOCUMENTDB_ENGINE_VERSION_PREFIX = "5.0.0";

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        var buildInfo = template.executeCommand(new Document("buildInfo", 1));

        try {
            if (checkDatabaseCompatibility(buildInfo)) {
                final List<String> deletedGroupsIds = findDeletedGroups();
                if (!deletedGroupsIds.isEmpty()) {
                    var result = removeDeletedGroupsFromApis(deletedGroupsIds);
                    log.info("Removed deleted groups {} from {} APIs", deletedGroupsIds, result.getModifiedCount());
                } else {
                    log.info("No deleted groups found");
                }
            } else {
                log.warn("Skipping this upgrade because database is not compatible: {}", buildInfo.toJson());
            }
        } catch (Exception ex) {
            // Ultimate safeguard, we detect if an error occurred while running the upgrader and we skip it. The original
            // issue is not that popular as it requires a lot of APIs and a group deletion. I prefer to skip the upgrader
            // and run a manual cleanup if needed.
            log.error("An error occurs while running the upgrader, skipping it", ex);
            return true;
        }

        return true;
    }

    private boolean checkDatabaseCompatibility(Document document) {
        var version = document.getString("version");
        var majorVersion = Integer.parseInt(version.split("\\.")[0]);
        var hasStorageEngine = document.containsKey("storageEngine");

        // The absence of storageEngine is a strong indicator of DocumentDB
        return hasStorageEngine && majorVersion >= 5;
    }

    private List<String> findDeletedGroups() {
        var deletedGroupsIdsAggregateResult =
            this.getCollection("apis")
                .aggregate(
                    List.of(
                        // 1. Filter APIs with groups defined
                        Aggregates.match(Filters.and(Filters.exists(ATTR_GROUPS, true), Filters.not(Filters.size(ATTR_GROUPS, 0)))),
                        // 2. Keep only relevant attributes
                        Aggregates.project(Projections.include(ATTR_ID, ATTR_GROUPS)),
                        // 3. Join with Groups collections. "matchedGroups" contains the _id of Groups that exists
                        new Document(
                            "$lookup",
                            new Document("from", ATTR_GROUPS)
                                .append("localField", ATTR_GROUPS)
                                .append("foreignField", ATTR_ID)
                                .append("as", "matchedGroups")
                                .append("pipeline", List.of(new Document("$project", new Document(ATTR_ID, 1L))))
                        ),
                        // 4. Make `existingGroups` a list of strings instead of documents
                        Aggregates.project(
                            Projections.fields(
                                Projections.include(ATTR_ID, ATTR_GROUPS),
                                Projections.computed("existingGroups", "$matchedGroups._id")
                            )
                        ),
                        // 5. Calculate the difference between groups and existingGroups to find the deleted groups
                        Aggregates.project(
                            Projections.fields(
                                Projections.include(ATTR_ID),
                                Projections.computed("deletedGroups", new Document("$setDifference", List.of("$groups", "$existingGroups")))
                            )
                        ),
                        // 6. Keep only APIs with deleted groups
                        Aggregates.match(Filters.not(Filters.size("deletedGroups", 0))),
                        // 7. Deconstruct the arrays
                        Aggregates.unwind("$deletedGroups"),
                        // 8. Group all documents and collect unique IDs
                        new Document(
                            "$group",
                            new Document(ATTR_ID, new BsonNull()).append("allDeletedGroups", new Document("$addToSet", "$deletedGroups"))
                        ),
                        // 9. Clean up the output
                        Aggregates.project(
                            Projections.fields(Projections.excludeId(), Projections.computed("deletedGroups", "$allDeletedGroups"))
                        )
                    )
                );
        return StreamSupport
            .stream(deletedGroupsIdsAggregateResult.spliterator(), false)
            .findFirst()
            .map(document -> document.getList("deletedGroups", String.class))
            .orElse(List.of());
    }

    private UpdateResult removeDeletedGroupsFromApis(List<String> deletedGroupsIds) {
        return this.getCollection("apis")
            .updateMany(
                Filters.in(ATTR_GROUPS, deletedGroupsIds),
                new Document("$pull", new Document(ATTR_GROUPS, new Document("$in", deletedGroupsIds)))
            );
    }

    @Override
    public int getOrder() {
        return REMOVE_DELETED_GROUPS_UPGRADER_ORDER;
    }
}
