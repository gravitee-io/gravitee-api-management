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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("EmailUniqueIndexUpgrader")
public class EmailUniqueIndexUpgrader extends MongoUpgrader {

    private static final Logger LOG = LoggerFactory.getLogger(
        EmailUniqueIndexUpgrader.class
    );

    @Override
    public String version() {
        return "v1";
    }

    @Override
    public boolean upgrade() {
        try {
            MongoCollection<Document> usersCollection = this.getCollection("users");

            Map<String, List<Document>> grouped = groupDocumentsByEmail(usersCollection);
            backupDuplicates(grouped);
            removeDuplicates(usersCollection, grouped);
            createEmailIndex(usersCollection);

            return true;
        } catch (Exception e) {
            LOG.error("An error occurred while upgrading EmailUniqueIndex", e);
            return false;
        }
    }

    private Map<String, List<Document>> groupDocumentsByEmail(
        MongoCollection<Document> collection
    ) {
        Map<String, List<Document>> grouped = new HashMap<>();
        FindIterable<Document> allDocs = collection.find();

        for (Document doc : allDocs) {
            String email = doc.getString("email");
            grouped.computeIfAbsent(email, k -> new ArrayList<>()).add(doc);
        }
        return grouped;
    }

    private void backupDuplicates(Map<String, List<Document>> grouped) {
        List<Document> duplicates = grouped
            .values()
            .stream()
            .filter(list -> list.size() > 1)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        if (!duplicates.isEmpty()) {
            MongoCollection<Document> backupCollection =
                this.getCollection(
                        "users_duplicate_backup_" + System.currentTimeMillis()
                    );
            backupCollection.insertMany(duplicates);
        }
    }

    private void removeDuplicates(
        MongoCollection<Document> usersCollection,
        Map<String, List<Document>> grouped
    ) {
        for (List<Document> docs : grouped.values()) {
            List<Document> activeDocs = docs
                .stream()
                .filter(doc -> "ACTIVE".equalsIgnoreCase(doc.getString("status")))
                .toList();

            Document toKeep = activeDocs.isEmpty()
                ? docs.get(0) // fallback to first if no ACTIVE
                : activeDocs.get(0); // keep the first ACTIVE

            for (Document doc : docs) {
                if (!doc.get("_id").equals(toKeep.get("_id"))) {
                    usersCollection.deleteOne(Filters.eq("_id", doc.get("_id")));
                }
            }
        }
    }

    private void createEmailIndex(MongoCollection<Document> usersCollection) {
        IndexOptions indexOptions = new IndexOptions()
            .unique(true)
            .name("oi1e1_unique")
            .partialFilterExpression(Filters.eq("status", "ACTIVE"));

        usersCollection.createIndex(Indexes.ascending("email"), indexOptions);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
