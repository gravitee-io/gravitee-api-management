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
import io.gravitee.apim.core.portal.exception.PathConflictException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SegmentConflictRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";
    private static final PortalNavigationItemId PARENT_ID = PortalNavigationItemId.of("11111111-1111-1111-1111-111111111a11");
    private static final PortalNavigationItemId ITEM_ID = PortalNavigationItemId.of("22222222-2222-2222-2222-2222222222a2");
    private static final PortalNavigationItemId OTHER_ID = PortalNavigationItemId.of("33333333-3333-3333-3333-3333333333a3");

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private SegmentConflictRule rule;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        rule = new SegmentConflictRule(navigationItemsQueryService);
    }

    @Test
    void appliesTo_returns_false_when_segment_or_id_is_null() {
        assertThat(rule.appliesTo(item(null, "docs", PortalNavigationItemType.FOLDER, null))).isFalse();
        assertThat(rule.appliesTo(item(ITEM_ID, null, PortalNavigationItemType.FOLDER, null))).isFalse();
    }

    @Test
    void appliesTo_returns_false_for_page_type() {
        assertThat(rule.appliesTo(item(ITEM_ID, "docs", PortalNavigationItemType.PAGE, null))).isFalse();
    }

    @Test
    void appliesTo_returns_true_for_folder_link_api_and_api_product() {
        assertThat(rule.appliesTo(item(ITEM_ID, "docs", PortalNavigationItemType.FOLDER, null))).isTrue();
        assertThat(rule.appliesTo(item(ITEM_ID, "docs", PortalNavigationItemType.LINK, null))).isTrue();
        assertThat(rule.appliesTo(item(ITEM_ID, "docs", PortalNavigationItemType.API, null))).isTrue();
        assertThat(rule.appliesTo(item(ITEM_ID, "docs", PortalNavigationItemType.API_PRODUCT, null))).isTrue();
    }

    @Test
    void validate_passes_when_no_sibling_holds_the_segment() {
        assertThatCode(() ->
            rule.validate(item(ITEM_ID, "docs", PortalNavigationItemType.FOLDER, null), ENV_ID, CreateValidationContext.empty())
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_folder_throws_folder_path_message_when_persisted_sibling_collides() {
        navigationItemsQueryService.storage().add(existingFolder(OTHER_ID, PARENT_ID, "docs"));
        var item = item(ITEM_ID, "docs", PortalNavigationItemType.FOLDER, folderLocation("/docs"));

        assertThatThrownBy(() -> rule.validate(item, ENV_ID, CreateValidationContext.empty()))
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("/docs")
            .hasMessageContaining("not managed by the Automation API");
    }

    @Test
    void validate_link_throws_link_kind_when_persisted_sibling_collides() {
        navigationItemsQueryService.storage().add(existingFolder(OTHER_ID, PARENT_ID, "help"));
        var item = item(ITEM_ID, "help", PortalNavigationItemType.LINK, folderLocation("/help"));

        assertThatThrownBy(() -> rule.validate(item, ENV_ID, CreateValidationContext.empty()))
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("Link entry at [/help]");
    }

    @Test
    void validate_api_throws_listing_kind_when_persisted_sibling_collides() {
        navigationItemsQueryService.storage().add(existingFolder(OTHER_ID, PARENT_ID, "pets"));
        var item = item(ITEM_ID, "pets", PortalNavigationItemType.API, folderLocation("/pets"));

        assertThatThrownBy(() -> rule.validate(item, ENV_ID, CreateValidationContext.empty()))
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("Listing entry at [/pets]");
    }

    @Test
    void validate_passes_when_sibling_at_same_segment_is_the_same_item() {
        navigationItemsQueryService.storage().add(existingFolder(ITEM_ID, PARENT_ID, "docs"));

        assertThatCode(() ->
            rule.validate(item(ITEM_ID, "docs", PortalNavigationItemType.FOLDER, null), ENV_ID, CreateValidationContext.empty())
        ).doesNotThrowAnyException();
    }

    @Test
    void validate_throws_when_another_item_in_the_pending_batch_claims_same_parent_and_segment() {
        var pendingClaim = new PendingSegmentClaim(OTHER_ID, PARENT_ID, "docs");
        var ctx = new CreateValidationContext(List.of(), Map.of(), Map.of(), Map.of(), List.of(pendingClaim));
        var item = item(ITEM_ID, "docs", PortalNavigationItemType.FOLDER, folderLocation("/docs"));

        assertThatThrownBy(() -> rule.validate(item, ENV_ID, ctx))
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("/docs");
    }

    @Test
    void validate_throws_when_a_pending_update_in_the_same_batch_targets_the_same_parent_and_segment() {
        // A create at (PARENT_ID, "docs") collides with a pending update moving another item to the same slot.
        var pendingUpdateClaim = new PendingSegmentClaim(OTHER_ID, PARENT_ID, "docs");
        var ctx = new CreateValidationContext(List.of(), Map.of(), Map.of(), Map.of(), List.of(pendingUpdateClaim));
        var newItem = item(ITEM_ID, "docs", PortalNavigationItemType.FOLDER, folderLocation("/docs"));

        assertThatThrownBy(() -> rule.validate(newItem, ENV_ID, ctx))
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("/docs");
    }

    @Test
    void validate_falls_back_to_segment_as_location_when_automation_metadata_absent() {
        navigationItemsQueryService.storage().add(existingFolder(OTHER_ID, PARENT_ID, "help"));

        assertThatThrownBy(() ->
            rule.validate(item(ITEM_ID, "help", PortalNavigationItemType.LINK, null), ENV_ID, CreateValidationContext.empty())
        )
            .isInstanceOf(PathConflictException.class)
            .hasMessageContaining("Link entry at [help]");
    }

    private static CreatePortalNavigationItem item(
        PortalNavigationItemId id,
        String segment,
        PortalNavigationItemType type,
        AutomationMetadata meta
    ) {
        return CreatePortalNavigationItem.builder()
            .id(id)
            .title(segment != null ? segment : "n/a")
            .segment(segment)
            .type(type)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .parentId(PARENT_ID)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .automationMetadata(meta)
            .build();
    }

    private static PortalNavigationFolder existingFolder(PortalNavigationItemId id, PortalNavigationItemId parentId, String segment) {
        return PortalNavigationFolder.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(segment)
            .segment(segment)
            .parentId(parentId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .visibility(PortalVisibility.PUBLIC)
            .published(true)
            .build();
    }

    private static AutomationMetadata folderLocation(String location) {
        return new AutomationMetadata(AutomationMetadata.ReferenceType.PORTAL, "portal-id", null, Optional.of(location), Optional.empty());
    }
}
