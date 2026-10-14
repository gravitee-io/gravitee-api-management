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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.AutomationMetadata;
import io.gravitee.repository.management.model.AutomationTargetReferenceType;
import io.gravitee.repository.management.model.PortalNavigationItem;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemApiOwnedConsoleMigrationUpgraderTest {

    private static final String ORG = "org";
    private static final String ENV = "env";
    private static final String API_ID = "api-1";
    private static final String SECTION = "section";
    private static final String ROW = "row-1";

    private FakePortalNavigationItemRepository repository;
    private ApiRepository apiRepository;
    private PortalNavigationItemApiOwnedConsoleMigrationUpgrader upgrader;

    @BeforeEach
    void setUp() throws TechnicalException {
        repository = new FakePortalNavigationItemRepository();
        apiRepository = mock(ApiRepository.class);
        lenient().when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        givenApis(API_ID, "api-2");
        upgrader = new PortalNavigationItemApiOwnedConsoleMigrationUpgrader(repository, apiRepository);
    }

    @Test
    void should_move_a_page_under_a_standalone_listing_to_the_api() throws UpgraderException {
        repository.initWith(List.of(section(SECTION), row(ROW, API_ID, SECTION), page("page", ROW, 0, "intro")));

        assertThat(upgrader.upgrade()).isTrue();

        var page = repository.get("page");
        assertThat(page.getParentId()).isNull();
        assertThat(page.getRootId()).isEqualTo("page");
        assertThat(page.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
        assertThat(page.getReferenceId()).isEqualTo(API_ID);
        assertThat(page.getSegment()).isEqualTo("intro");
    }

    @Test
    void should_move_a_folder_and_everything_inside_it() throws UpgraderException {
        repository.initWith(
            List.of(
                section(SECTION),
                row(ROW, API_ID, SECTION),
                folder("guides", ROW, 0, "guides"),
                folder("advanced", "guides", 3, "advanced"),
                page("deep", "advanced", 1, "deep")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("guides").getParentId()).isNull();
        assertThat(repository.get("advanced").getParentId()).isEqualTo("guides");
        assertThat(repository.get("deep").getParentId()).isEqualTo("advanced");
        assertThat(List.of("guides", "advanced", "deep")).allSatisfy(id -> {
            var item = repository.get(id);
            assertThat(item.getRootId()).isEqualTo("guides");
            assertThat(item.getReferenceType()).isEqualTo(PortalNavigationReferenceType.API);
            assertThat(item.getReferenceId()).isEqualTo(API_ID);
        });
        assertThat(repository.get("advanced").getOrder()).isEqualTo(3);
        assertThat(repository.get("deep").getOrder()).isEqualTo(1);
    }

    @Test
    void should_move_an_item_whose_owner_was_already_the_api() throws UpgraderException {
        var detached = page("detached", ROW, 0, "detached");
        detached.setReferenceType(PortalNavigationReferenceType.API);
        detached.setReferenceId(API_ID);
        repository.initWith(List.of(section(SECTION), row(ROW, API_ID, SECTION), detached));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("detached").getParentId()).isNull();
        assertThat(repository.get("detached").getRootId()).isEqualTo("detached");
    }

    @Test
    void should_merge_positions_in_the_order_the_server_shows_today() throws UpgraderException {
        repository.initWith(
            List.of(
                section(SECTION),
                row(ROW, API_ID, SECTION),
                page("child-0", ROW, 0, "child-0"),
                page("child-1", ROW, 1, "child-1"),
                apiRoot("root-0", API_ID, 0, "root-0"),
                apiRoot("root-2", API_ID, 2, "root-2")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(orders("child-0", "root-0", "child-1", "root-2")).containsExactly(0, 1, 2, 3);
    }

    @Test
    void should_rename_the_segment_of_a_migrated_root_that_clashes() throws UpgraderException {
        repository.initWith(
            List.of(
                section(SECTION),
                row(ROW, API_ID, SECTION),
                folder("hand-made", ROW, 0, "guides"),
                apiRoot("api-guides", API_ID, 1, "guides")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("api-guides").getSegment()).isEqualTo("guides");
        assertThat(repository.get("hand-made").getSegment()).isEqualTo("guides-2");
    }

    @Test
    void should_not_rename_segments_inside_a_moved_folder() throws UpgraderException {
        repository.initWith(
            List.of(
                section(SECTION),
                row(ROW, API_ID, SECTION),
                folder("hand-made", ROW, 0, "guides"),
                page("inside", "hand-made", 0, "guides"),
                apiRoot("api-guides", API_ID, 1, "guides")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("inside").getSegment()).isEqualTo("guides");
    }

    @Test
    void should_leave_a_listing_inside_an_api_product_alone() throws UpgraderException {
        var product = item("product", PortalNavigationItem.Type.API_PRODUCT, SECTION, 0, "product");
        var productFolder = folder("product-folder", "product", 0, "product-folder");
        var productRow = row("product-row", API_ID, "product-folder");
        var productPage = page("product-page", "product-row", 0, "overview");
        repository.initWith(List.of(section(SECTION), product, productFolder, productRow, productPage));
        var before = snapshot();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void should_leave_an_item_with_portal_automation_metadata_alone() throws UpgraderException {
        var automated = folder("automated", ROW, 0, "automated");
        automated.setAutomationMetadata(
            AutomationMetadata.builder().referenceType(AutomationTargetReferenceType.PORTAL).referenceId("portal").build()
        );
        var inside = page("inside-automated", "automated", 0, "inside");
        repository.initWith(List.of(section(SECTION), row(ROW, API_ID, SECTION), automated, inside, page("hand-made", ROW, 1, "notes")));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("automated").getParentId()).isEqualTo(ROW);
        assertThat(repository.get("automated").getReferenceType()).isEqualTo(PortalNavigationReferenceType.PORTAL);
        assertThat(repository.get("inside-automated").getReferenceType()).isEqualTo(PortalNavigationReferenceType.PORTAL);
        assertThat(repository.get("hand-made").getParentId()).isNull();
    }

    @Test
    void should_leave_a_listing_whose_api_no_longer_exists_alone() throws UpgraderException {
        repository.initWith(List.of(section(SECTION), row(ROW, "deleted-api", SECTION), page("orphan", ROW, 0, "orphan")));
        var before = snapshot();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void should_merge_the_children_of_two_standalone_listings_of_one_api() throws UpgraderException {
        repository.initWith(
            List.of(
                section(SECTION),
                section("section-2"),
                row(ROW, API_ID, SECTION),
                row("row-2", API_ID, "section-2"),
                page("under-row-1", ROW, 0, "notes"),
                page("under-row-2", "row-2", 0, "notes")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("under-row-1").getParentId()).isNull();
        assertThat(repository.get("under-row-2").getParentId()).isNull();
        assertThat(orders("under-row-1", "under-row-2")).containsExactly(0, 1);
        assertThat(repository.get("under-row-1").getSegment()).isEqualTo("notes");
        assertThat(repository.get("under-row-2").getSegment()).isEqualTo("notes-2");
    }

    @Test
    void should_keep_every_starter_page() throws UpgraderException {
        var starter = page("starter", ROW, 0, "overview");
        starter.setTitle("Overview");
        starter.setPublished(false);
        var own = apiRoot("own-overview", API_ID, 0, "overview");
        own.setType(PortalNavigationItem.Type.PAGE);
        own.setTitle("Overview");
        own.setPublished(true);
        repository.initWith(List.of(section(SECTION), row(ROW, API_ID, SECTION), starter, own));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.storage()).hasSize(4);
        assertThat(repository.get("starter").getReferenceId()).isEqualTo(API_ID);
        assertThat(repository.get("starter").isPublished()).isFalse();
        assertThat(repository.get("starter").getSegment()).isEqualTo("overview-2");
        assertThat(repository.get("own-overview").isPublished()).isTrue();
        assertThat(repository.get("own-overview").getSegment()).isEqualTo("overview");
    }

    @Test
    void should_unpublish_a_moved_item_whose_listing_was_unpublished() throws UpgraderException {
        var row = row(ROW, API_ID, SECTION);
        row.setPublished(false);
        repository.initWith(List.of(section(SECTION), row, page("page", ROW, 0, "intro"), page("below", "page", 0, "below")));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("page").isPublished()).isFalse();
        // Hidden through the moved item, as it was through the listing
        assertThat(repository.get("below").isPublished()).isTrue();
    }

    @Test
    void should_unpublish_a_moved_item_when_a_section_above_its_listing_is_unpublished() throws UpgraderException {
        var section = section(SECTION);
        section.setPublished(false);
        repository.initWith(List.of(section, row(ROW, API_ID, SECTION), page("page", ROW, 0, "intro")));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("page").isPublished()).isFalse();
    }

    @Test
    void should_make_a_moved_item_private_when_a_section_above_its_listing_is_private() throws UpgraderException {
        var section = section(SECTION);
        section.setVisibility(PortalNavigationItem.Visibility.PRIVATE);
        repository.initWith(List.of(section, row(ROW, API_ID, SECTION), page("page", ROW, 0, "intro")));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("page").getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PRIVATE);
        assertThat(repository.get("page").isPublished()).isTrue();
    }

    @Test
    void should_keep_the_visibility_of_a_moved_item_whose_listing_was_shown() throws UpgraderException {
        var hidden = page("hidden", ROW, 1, "hidden");
        hidden.setPublished(false);
        hidden.setVisibility(PortalNavigationItem.Visibility.PRIVATE);
        repository.initWith(List.of(section(SECTION), row(ROW, API_ID, SECTION), page("shown", ROW, 0, "shown"), hidden));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.get("shown").isPublished()).isTrue();
        assertThat(repository.get("shown").getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PUBLIC);
        assertThat(repository.get("hidden").isPublished()).isFalse();
        assertThat(repository.get("hidden").getVisibility()).isEqualTo(PortalNavigationItem.Visibility.PRIVATE);
    }

    @Test
    void should_leave_a_listing_whose_api_belongs_to_another_environment_alone() throws TechnicalException, UpgraderException {
        when(apiRepository.findById("elsewhere")).thenReturn(Optional.of(Api.builder().id("elsewhere").environmentId("other-env").build()));
        repository.initWith(List.of(section(SECTION), row(ROW, "elsewhere", SECTION), page("page", ROW, 0, "intro")));
        var before = snapshot();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void should_keep_the_source_and_the_parents_below_a_migrated_sourced_folder() throws UpgraderException {
        var sourced = folder("sourced", ROW, 0, "imported");
        sourced.setConfiguration("{\"source\":{\"type\":\"github-fetcher\",\"configuration\":\"{}\"}}");
        repository.initWith(List.of(section(SECTION), row(ROW, API_ID, SECTION), sourced, page("fetched", "sourced", 0, "readme")));

        assertThat(upgrader.upgrade()).isTrue();

        // SourcedItemReadOnlyRule decides from the item's own source and its stored parents only
        assertThat(repository.get("sourced").getConfiguration()).isEqualTo(sourced.getConfiguration());
        assertThat(repository.get("fetched").getParentId()).isEqualTo("sourced");
    }

    @Test
    void should_not_touch_an_api_with_nothing_under_its_listing() throws UpgraderException {
        repository.initWith(
            List.of(
                section(SECTION),
                row(ROW, API_ID, SECTION),
                apiRoot("root-0", API_ID, 0, "root-0"),
                apiRoot("root-5", API_ID, 5, "root-5")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.updateCount()).isZero();
    }

    @Test
    void should_migrate_without_creating_or_deleting_a_row() throws UpgraderException {
        repository.initWith(fullFixture());
        var idsBefore = ids();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(ids()).isEqualTo(idsBefore);
    }

    @Test
    void a_second_run_changes_nothing() throws UpgraderException {
        repository.initWith(fullFixture());
        upgrader.upgrade();
        var afterFirstRun = snapshot();

        repository.initWith(new ArrayList<>(repository.storage()));
        assertThat(upgrader.upgrade()).isTrue();

        assertThat(repository.updateCount()).isZero();
        assertThat(snapshot()).isEqualTo(afterFirstRun);
    }

    @Test
    void should_reach_the_same_result_when_interrupted_and_run_again() throws UpgraderException {
        repository.initWith(fullFixture());
        upgrader.upgrade();
        var uninterrupted = snapshot();
        var writes = repository.updateCount();

        for (int failingWrite = 1; failingWrite <= writes; failingWrite++) {
            repository.initWith(fullFixture());
            repository.failOnUpdate(failingWrite);
            assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);

            assertThat(upgrader.upgrade()).isTrue();

            // Only the positions step may end differently (positions are written before anything moves),
            // and the API's roots stay one contiguous list either way
            var resumed = snapshot();
            assertThat(withoutOrder(resumed)).as("interrupted at write %d", failingWrite).isEqualTo(withoutOrder(uninterrupted));
            assertThat(apiRootOrders(API_ID))
                .as("interrupted at write %d", failingWrite)
                .isEqualTo(IntStream.range(0, apiRootOrders(API_ID).size()).boxed().toList());
        }
    }

    @Test
    void should_run_after_the_rekey_upgrader_and_before_the_default_apis_folder() {
        assertThat(upgrader.getOrder())
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_ITEM_API_OWNED_REKEY_UPGRADER)
            .isLessThan(UpgraderOrder.ENVIRONMENTS_DEFAULT_APIS_FOLDER_UPGRADER);
    }

    @Test
    void should_fail_when_the_repository_fails() {
        repository.initWith(fullFixture());
        repository.failOnUpdate(1);

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    /** Every shape the migration handles, together: nested folders, a clash, two lists to merge, and an item left alone. */
    private static List<PortalNavigationItem> fullFixture() {
        var sourced = folder("sourced", ROW, 2, "imported");
        sourced.setConfiguration("{\"source\":{\"type\":\"github-fetcher\",\"configuration\":\"{}\"}}");
        return List.of(
            section(SECTION),
            row(ROW, API_ID, SECTION),
            folder("guides", ROW, 0, "guides"),
            folder("advanced", "guides", 0, "advanced"),
            page("deep", "advanced", 0, "deep"),
            page("notes", ROW, 1, "notes"),
            sourced,
            page("fetched", "sourced", 0, "readme"),
            apiRoot("api-guides", API_ID, 0, "guides"),
            apiRoot("api-faq", API_ID, 1, "faq"),
            row("row-other", "deleted-api", SECTION),
            page("orphan", "row-other", 0, "orphan")
        );
    }

    private void givenApis(String... apiIds) throws TechnicalException {
        for (var apiId : apiIds) {
            lenient().when(apiRepository.findById(apiId)).thenReturn(Optional.of(Api.builder().id(apiId).environmentId(ENV).build()));
        }
    }

    private List<Integer> orders(String... ids) {
        return Arrays.stream(ids)
            .map(id -> repository.get(id).getOrder())
            .toList();
    }

    private List<Integer> apiRootOrders(String apiId) {
        return repository
            .storage()
            .stream()
            .filter(item -> item.getParentId() == null && item.getReferenceType() == PortalNavigationReferenceType.API)
            .filter(item -> apiId.equals(item.getReferenceId()))
            .map(PortalNavigationItem::getOrder)
            .sorted()
            .toList();
    }

    private Collection<String> ids() {
        return repository.storage().stream().map(PortalNavigationItem::getId).sorted().toList();
    }

    private Map<String, Row> snapshot() {
        return repository.storage().stream().collect(Collectors.toMap(PortalNavigationItem::getId, Row::of));
    }

    private static Map<String, Row> withoutOrder(Map<String, Row> snapshot) {
        return snapshot.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().withOrder(null)));
    }

    private record Row(
        String parentId,
        String rootId,
        PortalNavigationReferenceType referenceType,
        String referenceId,
        Integer order,
        String segment,
        boolean published,
        PortalNavigationItem.Visibility visibility
    ) {
        static Row of(PortalNavigationItem item) {
            return new Row(
                item.getParentId(),
                item.getRootId(),
                item.getReferenceType(),
                item.getReferenceId(),
                item.getOrder(),
                item.getSegment(),
                item.isPublished(),
                item.getVisibility()
            );
        }

        Row withOrder(Integer newOrder) {
            return new Row(parentId, rootId, referenceType, referenceId, newOrder, segment, published, visibility);
        }
    }

    private static PortalNavigationItem section(String id) {
        var section = item(id, PortalNavigationItem.Type.FOLDER, null, 0, id);
        section.setRootId(id);
        return section;
    }

    private static PortalNavigationItem row(String id, String apiId, String parentId) {
        var row = item(id, PortalNavigationItem.Type.API, parentId, 0, id);
        row.setApiId(apiId);
        return row;
    }

    private static PortalNavigationItem folder(String id, String parentId, int order, String segment) {
        return item(id, PortalNavigationItem.Type.FOLDER, parentId, order, segment);
    }

    private static PortalNavigationItem page(String id, String parentId, int order, String segment) {
        return item(id, PortalNavigationItem.Type.PAGE, parentId, order, segment);
    }

    private static PortalNavigationItem apiRoot(String id, String apiId, int order, String segment) {
        var root = item(id, PortalNavigationItem.Type.FOLDER, null, order, segment);
        root.setRootId(id);
        root.setReferenceType(PortalNavigationReferenceType.API);
        root.setReferenceId(apiId);
        return root;
    }

    /** A portal-owned item in the main navigation; its root is the top section, as for any item stored below it. */
    private static PortalNavigationItem item(String id, PortalNavigationItem.Type type, String parentId, int order, String segment) {
        return PortalNavigationItem.builder()
            .id(id)
            .organizationId(ORG)
            .environmentId(ENV)
            .type(type)
            .area(PortalNavigationItem.Area.TOP_NAVBAR)
            .title(segment)
            .segment(segment)
            .parentId(parentId)
            .rootId(SECTION)
            .order(order)
            .published(true)
            .visibility(PortalNavigationItem.Visibility.PUBLIC)
            .build();
    }
}
