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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.organization;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateManyModel;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.accessPoints.AccessPointsStatusAndUpdatedUpgrader;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.common.MongoUpgrader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.springframework.stereotype.Component;

/**
 * Initialize `organizations` attribute on Event
 */
@Component
public class MissingOrganizationsOnEventsUpgrader extends MongoUpgrader {

    public static final int MISSING_ORGANIZATION_ON_EVENT_UPGRADER_ORDER =
        AccessPointsStatusAndUpdatedUpgrader.ACCESSPOINTS_STATUS_AND_UPDATED_UPGRADER_ORDER + 1;

    @Override
    public boolean upgrade() {
        var bulkActions = new ArrayList<UpdateManyModel<Document>>();
        final FindIterable<Document> environmentDocs = getCollection("environments")
            .find()
            .projection(Projections.fields(Projections.include("_id", "organizationId")));

        final Map<String, List<String>> environmentsByOrganization = StreamSupport
            .stream(environmentDocs.spliterator(), false)
            .collect(groupingBy(document -> document.getString("organizationId"), mapping(e -> e.getString("organizationId"), toList())));

        environmentsByOrganization.forEach((key, value) ->
            bulkActions.add(
                new UpdateManyModel<>(
                    Filters.and(Filters.exists("organizations", false), Filters.in("environments", value)),
                    new Document("$push", new Document("organizations", key))
                )
            )
        );

        if (!bulkActions.isEmpty()) {
            return (
                getCollection("events").bulkWrite(bulkActions).wasAcknowledged() &&
                getCollection("events_latest").bulkWrite(bulkActions).wasAcknowledged()
            );
        }
        return true;
    }

    @Override
    public int getOrder() {
        return MISSING_ORGANIZATION_ON_EVENT_UPGRADER_ORDER;
    }
}
