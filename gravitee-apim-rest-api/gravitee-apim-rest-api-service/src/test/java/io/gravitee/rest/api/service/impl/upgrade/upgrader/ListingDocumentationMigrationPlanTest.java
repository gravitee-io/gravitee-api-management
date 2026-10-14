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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.AutomationMetadata;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListingDocumentationMigrationPlanTest {

    private static final String API_ID = "api-1";

    @Test
    void should_summarize_what_it_moves_and_what_it_leaves_alone() {
        var starter = item("starter", PortalNavigationItem.Type.PAGE, "row-1", 0, "overview");
        starter.setTitle("Overview");
        var sourced = item("sourced", PortalNavigationItem.Type.FOLDER, "row-1", 1, "imported");
        sourced.setConfiguration("{\"source\":{\"type\":\"github-fetcher\",\"configuration\":\"{}\"}}");
        var automated = item("automated", PortalNavigationItem.Type.PAGE, "row-1", 2, "automated");
        automated.setAutomationMetadata(
            AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.PORTAL).referenceId("portal").build()
        );
        var apiRoot = item("api-overview", PortalNavigationItem.Type.PAGE, null, 0, "overview");
        apiRoot.setReferenceType(PortalNavigationReferenceType.API);
        apiRoot.setReferenceId(API_ID);
        var hiddenRow = row("row-2", API_ID, "section");
        hiddenRow.setPublished(false);
        var items = List.of(
            item("section", PortalNavigationItem.Type.FOLDER, null, 0, "section"),
            row("row-1", API_ID, "section"),
            hiddenRow,
            starter,
            sourced,
            item("fetched", PortalNavigationItem.Type.PAGE, "sourced", 0, "readme"),
            automated,
            item("under-row-2", PortalNavigationItem.Type.LINK, "row-2", 0, "link"),
            apiRoot,
            item("product", PortalNavigationItem.Type.API_PRODUCT, "section", 1, "product"),
            row("product-row", API_ID, "product"),
            item("product-page", PortalNavigationItem.Type.PAGE, "product-row", 0, "product-page"),
            row("orphan-row", "deleted-api", "section"),
            item("orphan-page", PortalNavigationItem.Type.PAGE, "orphan-row", 0, "orphan-page")
        );

        var summary = new ListingDocumentationMigrationPlan(items, Set.of(API_ID)).summary();

        assertThat(summary).isEqualTo(
            new ListingDocumentationMigrationPlan.Summary(
                /* apisMigrated */ 1,
                /* itemsMoved */ 4,
                /* sourcedItemsMoved */ 2,
                /* starterPagesMoved */ 1,
                /* segmentsRenamed */ 1,
                /* apisWithSeveralListings */ 1,
                /* itemsHidden */ 1,
                /* itemsLeftUnderApiProducts */ 1,
                /* itemsLeftForPortalAutomation */ 1,
                /* itemsLeftForMissingApis */ 1
            )
        );
    }

    @Test
    void should_summarize_nothing_when_there_is_nothing_to_migrate() {
        var apiRoot = item("api-root", PortalNavigationItem.Type.PAGE, null, 0, "intro");
        apiRoot.setReferenceType(PortalNavigationReferenceType.API);
        apiRoot.setReferenceId(API_ID);

        var plan = new ListingDocumentationMigrationPlan(
            List.of(item("section", PortalNavigationItem.Type.FOLDER, null, 0, "section"), row("row-1", API_ID, "section"), apiRoot),
            Set.of(API_ID)
        );

        assertThat(plan.summary()).isEqualTo(new ListingDocumentationMigrationPlan.Summary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(plan.isEmpty()).isTrue();
    }

    private static PortalNavigationItem row(String id, String apiId, String parentId) {
        var row = item(id, PortalNavigationItem.Type.API, parentId, 0, id);
        row.setApiId(apiId);
        return row;
    }

    private static PortalNavigationItem item(String id, PortalNavigationItem.Type type, String parentId, int order, String segment) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId("org")
            .environmentId("env")
            .type(type)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .title(segment)
            .segment(segment)
            .parentId(parentId)
            .rootId("section")
            .order(order)
            .published(true)
            .build();
    }
}
