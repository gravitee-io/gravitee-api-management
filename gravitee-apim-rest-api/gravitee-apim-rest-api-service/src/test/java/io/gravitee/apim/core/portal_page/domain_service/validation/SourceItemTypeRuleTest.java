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

import fixtures.core.model.PortalNavigationItemFixtures;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageSource;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SourceItemTypeRuleTest {

    private static final String ENV_ID = "env-id";

    private final SourceItemTypeRule rule = new SourceItemTypeRule();

    private static PortalPageSource aSource() {
        return PortalPageSource.builder().sourceType("github-fetcher").sourceConfiguration("{}").build();
    }

    @Nested
    class Create {

        private CreatePortalNavigationItem anItem(PortalNavigationItemType type, PortalPageSource source) {
            return CreatePortalNavigationItem.builder().type(type).title("Item").order(0).source(source).build();
        }

        @Test
        void should_not_apply_when_no_source() {
            assertThat(rule.appliesTo(anItem(PortalNavigationItemType.LINK, null))).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "FOLDER" })
        void should_accept_source_on(PortalNavigationItemType type) {
            var item = anItem(type, aSource());

            assertThat(rule.appliesTo(item)).isTrue();
            assertThatCode(() -> rule.validate(item, ENV_ID, CreateValidationContext.empty())).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = PortalNavigationItemType.class, names = { "LINK", "API", "API_PRODUCT" })
        void should_reject_source_on(PortalNavigationItemType type) {
            var item = anItem(type, aSource());

            assertThat(rule.appliesTo(item)).isTrue();
            assertThatThrownBy(() -> rule.validate(item, ENV_ID, CreateValidationContext.empty()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining(type.name());
        }
    }

    @Nested
    class Update {

        private UpdatePortalNavigationItem anUpdate(PortalNavigationItemType type, PortalPageSource source) {
            return UpdatePortalNavigationItem.builder().type(type).title("Item").order(0).source(source).build();
        }

        static Stream<PortalNavigationItem> allowedItems() {
            return Stream.of(PortalNavigationItemFixtures.aPage("A page", null), PortalNavigationItemFixtures.aFolder("A folder"));
        }

        static Stream<PortalNavigationItem> rejectedItems() {
            return Stream.of(
                PortalNavigationItemFixtures.aLink(),
                PortalNavigationItemFixtures.anApi(),
                PortalNavigationItemFixtures.anApiProduct()
            );
        }

        @Test
        void should_not_apply_when_no_source() {
            var existing = PortalNavigationItemFixtures.aLink();

            assertThat(rule.appliesTo(anUpdate(existing.getType(), null), existing)).isFalse();
        }

        @ParameterizedTest
        @MethodSource("allowedItems")
        void should_accept_source_on(PortalNavigationItem existing) {
            var update = anUpdate(existing.getType(), aSource());

            assertThat(rule.appliesTo(update, existing)).isTrue();
            assertThatCode(() -> rule.validate(update, existing, UpdateValidationContext.empty())).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @MethodSource("rejectedItems")
        void should_reject_source_on(PortalNavigationItem existing) {
            var update = anUpdate(existing.getType(), aSource());

            assertThat(rule.appliesTo(update, existing)).isTrue();
            assertThatThrownBy(() -> rule.validate(update, existing, UpdateValidationContext.empty()))
                .isInstanceOf(InvalidPortalNavigationItemDataException.class)
                .hasMessageContaining(existing.getType().name());
        }
    }
}
