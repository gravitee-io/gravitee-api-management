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
import io.gravitee.apim.core.portal.domain_service.navigation.PortalNavigationValidator.PendingUpdate;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.exception.ConflictingNavigationItemStateException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DescendantVisibilityRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";
    private static final PortalNavigationItemId ROOT_ID = PortalNavigationItemId.of("11111111-1111-1111-1111-111111111a11");
    private static final PortalNavigationItemId CHILD_ID = PortalNavigationItemId.of("22222222-2222-2222-2222-2222222222a2");
    private static final PortalNavigationItemId GRANDCHILD_ID = PortalNavigationItemId.of("33333333-3333-3333-3333-3333333333a3");

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private DescendantVisibilityRule rule;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        rule = new DescendantVisibilityRule(navigationItemsQueryService);
    }

    @Test
    void appliesTo_is_false_when_update_is_not_switching_to_private() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        assertThat(rule.appliesTo(toPublic(), existing)).isFalse();
        assertThat(rule.appliesTo(withoutVisibility(), existing)).isFalse();
    }

    @Test
    void appliesTo_is_false_when_existing_is_already_private() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PRIVATE);
        assertThat(rule.appliesTo(toPrivate(), existing)).isFalse();
    }

    @Test
    void appliesTo_is_true_on_actual_transition_from_public_to_private() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        assertThat(rule.appliesTo(toPrivate(), existing)).isTrue();
    }

    @Test
    void validate_passes_when_no_descendants_exist() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        assertThatCode(() -> rule.validate(toPrivate(), existing, UpdateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void validate_passes_when_every_descendant_is_already_private() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.storage().add(folder(CHILD_ID, ROOT_ID, PortalVisibility.PRIVATE));
        navigationItemsQueryService.storage().add(folder(GRANDCHILD_ID, CHILD_ID, PortalVisibility.PRIVATE));

        assertThatCode(() -> rule.validate(toPrivate(), existing, UpdateValidationContext.empty())).doesNotThrowAnyException();
    }

    @Test
    void validate_throws_when_a_direct_child_is_public() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.storage().add(folder(CHILD_ID, ROOT_ID, PortalVisibility.PUBLIC));

        assertThatThrownBy(() -> rule.validate(toPrivate(), existing, UpdateValidationContext.empty()))
            .isInstanceOf(ConflictingNavigationItemStateException.class)
            .hasMessageContaining(ROOT_ID.toString());
    }

    @Test
    void validate_throws_when_a_grandchild_is_public_even_if_the_direct_child_is_private() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        navigationItemsQueryService.storage().add(folder(CHILD_ID, ROOT_ID, PortalVisibility.PRIVATE));
        navigationItemsQueryService.storage().add(folder(GRANDCHILD_ID, CHILD_ID, PortalVisibility.PUBLIC));

        assertThatThrownBy(() -> rule.validate(toPrivate(), existing, UpdateValidationContext.empty())).isInstanceOf(
            ConflictingNavigationItemStateException.class
        );
    }

    @Test
    void validate_passes_when_public_descendant_is_being_made_private_in_the_same_batch() {
        var existing = folder(ROOT_ID, null, PortalVisibility.PUBLIC);
        var child = folder(CHILD_ID, ROOT_ID, PortalVisibility.PUBLIC);
        navigationItemsQueryService.storage().add(child);

        var ctx = new UpdateValidationContext(
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(CHILD_ID, new PendingUpdate(toPrivate(), child)),
            List.of()
        );

        assertThatCode(() -> rule.validate(toPrivate(), existing, ctx)).doesNotThrowAnyException();
    }

    private static UpdatePortalNavigationItem toPrivate() {
        return UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.FOLDER).visibility(PortalVisibility.PRIVATE).build();
    }

    private static UpdatePortalNavigationItem toPublic() {
        return UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.FOLDER).visibility(PortalVisibility.PUBLIC).build();
    }

    private static UpdatePortalNavigationItem withoutVisibility() {
        return UpdatePortalNavigationItem.builder().type(PortalNavigationItemType.FOLDER).build();
    }

    private static PortalNavigationItem folder(PortalNavigationItemId id, PortalNavigationItemId parentId, PortalVisibility visibility) {
        return PortalNavigationFolder.builder()
            .id(id)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(id.toString())
            .segment(id.toString())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(visibility)
            .parentId(parentId)
            .build();
    }
}
