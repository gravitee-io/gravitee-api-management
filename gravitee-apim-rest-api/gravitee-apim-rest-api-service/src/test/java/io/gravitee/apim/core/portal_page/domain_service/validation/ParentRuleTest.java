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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.ParentAreaMismatchException;
import io.gravitee.apim.core.portal_page.exception.ParentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.ParentTypeMismatchException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ParentRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";
    private static final PortalNavigationItemId FOLDER_ID = PortalNavigationItemId.of("11111111-1111-1111-1111-111111111a11");
    private static final PortalNavigationItemId ITEM_ID = PortalNavigationItemId.of("22222222-2222-2222-2222-2222222222a2");
    private static final PortalNavigationItemId MISSING_ID = PortalNavigationItemId.of("33333333-3333-3333-3333-3333333333a3");

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private ParentRule rule;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        rule = new ParentRule(navigationItemsQueryService);
    }

    @Test
    void appliesTo_create_returns_true_when_parent_id_set_and_false_when_null() {
        assertThat(rule.appliesTo(pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null))).isTrue();
        assertThat(rule.appliesTo(pageCreate(null, PortalArea.TOP_NAVBAR, null))).isFalse();
    }

    @Test
    void create_passes_when_parent_exists_in_db_and_matches_area() {
        navigationItemsQueryService.storage().add(publicPublishedFolder(FOLDER_ID, PortalArea.TOP_NAVBAR));

        assertThatCode(() ->
            rule.validate(pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null), ENV_ID, CreateValidationContext.empty())
        ).doesNotThrowAnyException();
    }

    @Test
    void create_throws_when_parent_missing_in_db_and_not_automation() {
        assertThatThrownBy(() ->
            rule.validate(pageCreate(MISSING_ID, PortalArea.TOP_NAVBAR, null), ENV_ID, CreateValidationContext.empty())
        ).isInstanceOf(ParentNotFoundException.class);
    }

    @Test
    void create_tolerates_phantom_parent_when_automation() {
        var item = pageCreate(MISSING_ID, PortalArea.TOP_NAVBAR, automationMeta());

        assertThatCode(() -> rule.validate(item, ENV_ID, CreateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void create_throws_when_parent_exists_but_area_mismatches() {
        navigationItemsQueryService.storage().add(publicPublishedFolder(FOLDER_ID, PortalArea.HOMEPAGE));

        assertThatThrownBy(() ->
            rule.validate(pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null), ENV_ID, CreateValidationContext.empty())
        ).isInstanceOf(ParentAreaMismatchException.class);
    }

    @Test
    void create_throws_when_parent_exists_but_is_not_a_container() {
        navigationItemsQueryService
            .storage()
            .add(
                PortalNavigationPage.builder()
                    .id(FOLDER_ID)
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .title("Not a folder")
                    .segment("not-a-folder")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .portalPageContentId(PortalPageContentId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build()
            );

        assertThatThrownBy(() ->
            rule.validate(pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null), ENV_ID, CreateValidationContext.empty())
        ).isInstanceOf(ParentTypeMismatchException.class);
    }

    @Test
    void create_uses_pending_parent_from_same_batch() {
        var pendingParent = folderCreate(FOLDER_ID, PortalArea.TOP_NAVBAR);
        var child = pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null);
        var ctx = new CreateValidationContext(List.of(), Map.of(), Map.of(FOLDER_ID, pendingParent), Map.of(), List.of());

        assertThatCode(() -> rule.validate(child, ENV_ID, ctx)).doesNotThrowAnyException();
    }

    @Test
    void create_pending_parent_area_mismatch_throws() {
        var pendingParent = folderCreate(FOLDER_ID, PortalArea.HOMEPAGE);
        var child = pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null);
        var ctx = new CreateValidationContext(List.of(), Map.of(), Map.of(FOLDER_ID, pendingParent), Map.of(), List.of());

        assertThatThrownBy(() -> rule.validate(child, ENV_ID, ctx)).isInstanceOf(ParentAreaMismatchException.class);
    }

    @Test
    void update_passes_when_parent_exists_in_db() {
        navigationItemsQueryService.storage().add(publicPublishedFolder(FOLDER_ID, PortalArea.TOP_NAVBAR));
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, null);

        assertThatCode(() -> rule.validate(pageUpdate(FOLDER_ID), existing, UpdateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void update_throws_when_parent_missing_in_db_and_existing_is_not_automation_owned() {
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, null);

        assertThatThrownBy(() -> rule.validate(pageUpdate(MISSING_ID), existing, UpdateValidationContext.empty())).isInstanceOf(
            ParentNotFoundException.class
        );
    }

    @Test
    void update_tolerates_phantom_parent_when_existing_is_automation_owned() {
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, automationMeta());

        assertThatCode(() -> rule.validate(pageUpdate(MISSING_ID), existing, UpdateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void update_uses_pending_parent_from_same_batch() {
        var pendingParent = folderCreate(FOLDER_ID, PortalArea.TOP_NAVBAR);
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, null);
        var ctx = new UpdateValidationContext(List.of(), Map.of(), Map.of(FOLDER_ID, pendingParent), Map.of(), List.of());

        assertThatCode(() -> rule.validate(pageUpdate(FOLDER_ID), existing, ctx)).doesNotThrowAnyException();
    }

    @Test
    void update_pending_parent_area_mismatch_throws() {
        var pendingParent = folderCreate(FOLDER_ID, PortalArea.HOMEPAGE);
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, null);
        var ctx = new UpdateValidationContext(List.of(), Map.of(), Map.of(FOLDER_ID, pendingParent), Map.of(), List.of());

        assertThatThrownBy(() -> rule.validate(pageUpdate(FOLDER_ID), existing, ctx)).isInstanceOf(ParentAreaMismatchException.class);
    }

    @Test
    void update_pending_parent_public_under_private_throws() {
        var pendingParent = folderCreate(FOLDER_ID, PortalArea.TOP_NAVBAR).toBuilder().visibility(PortalVisibility.PRIVATE).build();
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, null);
        var ctx = new UpdateValidationContext(List.of(), Map.of(), Map.of(FOLDER_ID, pendingParent), Map.of(), List.of());

        assertThatThrownBy(() -> rule.validate(pageUpdate(FOLDER_ID), existing, ctx))
            .isInstanceOf(io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("must be PUBLIC");
    }

    @Test
    void create_public_child_under_persisted_private_parent_throws() {
        navigationItemsQueryService.storage().add(privatePublishedFolder(FOLDER_ID, PortalArea.TOP_NAVBAR));

        assertThatThrownBy(() -> rule.validate(pageCreate(FOLDER_ID, PortalArea.TOP_NAVBAR, null), ENV_ID, CreateValidationContext.empty()))
            .isInstanceOf(io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("must be PUBLIC");
    }

    @Test
    void update_public_child_under_persisted_private_parent_throws() {
        navigationItemsQueryService.storage().add(privatePublishedFolder(FOLDER_ID, PortalArea.TOP_NAVBAR));
        var existing = pageExisting(ITEM_ID, PortalArea.TOP_NAVBAR, null);

        assertThatThrownBy(() -> rule.validate(pageUpdate(FOLDER_ID), existing, UpdateValidationContext.empty()))
            .isInstanceOf(io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException.class)
            .hasMessageContaining("must be PUBLIC");
    }

    private static CreatePortalNavigationItem pageCreate(PortalNavigationItemId parentId, PortalArea area, AutomationMetadata meta) {
        return CreatePortalNavigationItem.builder()
            .id(ITEM_ID)
            .title("Doc")
            .segment("doc")
            .type(PortalNavigationItemType.PAGE)
            .area(area)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .automationMetadata(meta)
            .build();
    }

    private static CreatePortalNavigationItem folderCreate(PortalNavigationItemId id, PortalArea area) {
        return CreatePortalNavigationItem.builder()
            .id(id)
            .title("Folder")
            .segment("folder")
            .type(PortalNavigationItemType.FOLDER)
            .area(area)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
    }

    private static UpdatePortalNavigationItem pageUpdate(PortalNavigationItemId parentId) {
        return UpdatePortalNavigationItem.builder()
            .title("Doc")
            .segment("doc")
            .type(PortalNavigationItemType.PAGE)
            .order(0)
            .parentId(parentId)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
    }

    private static PortalNavigationFolder publicPublishedFolder(PortalNavigationItemId id, PortalArea area) {
        return folder(id, area, PortalVisibility.PUBLIC);
    }

    private static PortalNavigationFolder privatePublishedFolder(PortalNavigationItemId id, PortalArea area) {
        return folder(id, area, PortalVisibility.PRIVATE);
    }

    private static PortalNavigationFolder folder(PortalNavigationItemId id, PortalArea area, PortalVisibility visibility) {
        return PortalNavigationFolder.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Folder")
            .segment("folder")
            .area(area)
            .order(0)
            .published(true)
            .visibility(visibility)
            .build();
    }

    private static PortalNavigationPage pageExisting(PortalNavigationItemId id, PortalArea area, AutomationMetadata meta) {
        return PortalNavigationPage.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Doc")
            .segment("doc")
            .area(area)
            .order(0)
            .portalPageContentId(PortalPageContentId.of("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .automationMetadata(meta)
            .build();
    }

    private static AutomationMetadata automationMeta() {
        return new AutomationMetadata(
            AutomationMetadata.ReferenceType.PORTAL,
            PortalId.ZERO.toString(),
            "Doc",
            Optional.empty(),
            Optional.empty()
        );
    }
}
