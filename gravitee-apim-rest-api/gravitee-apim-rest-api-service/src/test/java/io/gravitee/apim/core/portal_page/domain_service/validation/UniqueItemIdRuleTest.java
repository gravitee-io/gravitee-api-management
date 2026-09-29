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
import io.gravitee.apim.core.portal_page.exception.ItemAlreadyExistsException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UniqueItemIdRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String ORG_ID = "org-1";
    private static final PortalNavigationItemId ITEM_ID = PortalNavigationItemId.of("11111111-1111-1111-1111-111111111a11");

    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private UniqueItemIdRule rule;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        rule = new UniqueItemIdRule(navigationItemsQueryService);
    }

    @Test
    void does_not_apply_when_item_has_no_id() {
        assertThat(rule.appliesTo(pageCreateItem(null))).isFalse();
    }

    @Test
    void an_existing_item_with_the_same_id_conflicts() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(pageCreateItem(ITEM_ID), ORG_ID, ENV_ID, null));

        assertThatThrownBy(() -> rule.validate(pageCreateItem(ITEM_ID), ENV_ID, CreateValidationContext.empty())).isInstanceOf(
            ItemAlreadyExistsException.class
        );
    }

    @Test
    void an_existing_item_declared_ignorable_does_not_conflict() {
        navigationItemsQueryService.storage().add(PortalNavigationItem.from(pageCreateItem(ITEM_ID), ORG_ID, ENV_ID, null));
        var ctx = new CreateValidationContext(List.of(), Map.of(), Map.of(), Map.of(), List.of(), Set.of(), Set.of(ITEM_ID));

        assertThatCode(() -> rule.validate(pageCreateItem(ITEM_ID), ENV_ID, ctx)).doesNotThrowAnyException();
    }

    private static CreatePortalNavigationItem pageCreateItem(PortalNavigationItemId id) {
        var builder = CreatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE)
            .title("Getting Started")
            .segment("getting-started")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN);
        if (id != null) {
            builder.id(id);
        }
        return builder.build();
    }
}
