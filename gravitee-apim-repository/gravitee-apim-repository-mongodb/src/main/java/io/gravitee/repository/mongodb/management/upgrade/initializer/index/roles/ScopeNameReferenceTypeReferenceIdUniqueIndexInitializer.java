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
package io.gravitee.repository.mongodb.management.upgrade.initializer.index.roles;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.gravitee.node.api.initializer.Initializer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.CustomLog;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@CustomLog
@Component("RolesScopeNameReferenceTypeReferenceIdUniqueIndexInitializer")
public class ScopeNameReferenceTypeReferenceIdUniqueIndexInitializer implements Initializer {

    public static final String INDEX_NAME = "s1n1rt1ri1_unique";

    private final MongoTemplate template;

    private final String rolesCollectionName;

    public ScopeNameReferenceTypeReferenceIdUniqueIndexInitializer(
        MongoTemplate template,
        @Value("${management.mongodb.prefix:}") String prefix
    ) {
        this.template = template;
        this.rolesCollectionName = prefix + "roles";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public boolean initialize() {
        try {
            final var rolesCollection = template.getCollection(rolesCollectionName);
            if (indexExists(rolesCollection)) {
                log.debug("Index {} already exists on {}", INDEX_NAME, rolesCollectionName);
                return true;
            }

            final var duplicates = searchDuplicateRoles(rolesCollection);
            if (!duplicates.isEmpty()) {
                log.error("{}", duplicatesMessage(duplicates));
                return false;
            }

            rolesCollection.createIndex(
                Indexes.ascending("scope", "name", "referenceType", "referenceId"),
                new IndexOptions().unique(true).name(INDEX_NAME)
            );
            log.info("Index {} has been created successfully on {}", INDEX_NAME, rolesCollectionName);
            return true;
        } catch (MongoException e) {
            log.error("Unable to create the unique index {} on the collection '{}'", INDEX_NAME, rolesCollectionName, e);
            return false;
        }
    }

    private boolean indexExists(MongoCollection<Document> collection) {
        return StreamSupport.stream(collection.listIndexes().spliterator(), false).anyMatch(index ->
            INDEX_NAME.equals(index.getString("name"))
        );
    }

    private List<Document> searchDuplicateRoles(MongoCollection<Document> collection) {
        final var duplicatesAggregated = collection.aggregate(
            Arrays.asList(
                Aggregates.group(
                    new Document("scope", "$scope")
                        .append("name", "$name")
                        .append("referenceType", "$referenceType")
                        .append("referenceId", "$referenceId"),
                    Accumulators.push("roleIds", "$_id"),
                    Accumulators.sum("count", 1)
                ),
                Aggregates.match(Filters.gt("count", 1))
            )
        );
        return StreamSupport.stream(duplicatesAggregated.spliterator(), false).toList();
    }

    private String duplicatesMessage(List<Document> duplicates) {
        final var duplicatedRoles = duplicates
            .stream()
            .map(duplicate -> {
                final var group = duplicate.get("_id", Document.class);
                return "scope [%s], name [%s], reference type [%s], reference id [%s], role ids %s".formatted(
                    group.get("scope"),
                    group.get("name"),
                    group.get("referenceType"),
                    group.get("referenceId"),
                    duplicate.get("roleIds")
                );
            })
            .collect(Collectors.joining("\n  - ", "\n  - ", "\n"));

        return (
            "The unique index %s was not created on the collection '%s' because several roles share the same scope, name, reference type and reference id. " +
            "Role uniqueness stays unenforced until they are resolved. Duplicated roles:%s" +
            "To list them again, run this in mongosh: db.getCollection('%s').aggregate([{$group: {_id: {scope: '$scope', name: '$name', referenceType: '$referenceType', referenceId: '$referenceId'}, roleIds: {$push: '$_id'}, count: {$sum: 1}}}, {$match: {count: {$gt: 1}}}])\n" +
            "For each group keep one role and delete the others. " +
            "If a membership references a role you delete, set its roleId to the kept role id first. " +
            "Then restart the node: the index is created at the next start."
        ).formatted(INDEX_NAME, rolesCollectionName, duplicatedRoles, rolesCollectionName);
    }
}
