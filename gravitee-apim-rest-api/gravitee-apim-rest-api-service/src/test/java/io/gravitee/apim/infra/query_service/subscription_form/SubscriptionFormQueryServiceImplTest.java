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
package io.gravitee.apim.infra.query_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormQueryServiceImplTest {

    private static final String ENV_ID = PortalNavigationItemFixtures.ENV_ID;

    PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    PortalPageContentQueryServiceInMemory pageContentQueryService;
    SubscriptionFormQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();
        pageContentQueryService = new PortalPageContentQueryServiceInMemory();
        service = new SubscriptionFormQueryServiceImpl(navigationItemsQueryService, pageContentQueryService);
    }

    @Nested
    class FindDefaultForEnvironmentId {

        @Test
        void should_return_subscription_form_when_found() {
            var contentId = PortalPageContentId.random();
            var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                contentId,
                "org-id",
                ENV_ID,
                "<gmd-input name=\"company\" label=\"Company\" required=\"true\"/>"
            );
            pageContentQueryService.initWith(List.of(content));
            var navItem = PortalNavigationItemFixtures.aSubscriptionForm("00000000-0000-0000-0000-000000000030", contentId)
                .toBuilder()
                .published(true)
                .build();
            navigationItemsQueryService.storage().add(navItem);

            var result = service.findDefaultForEnvironmentId(ENV_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(SubscriptionFormId.of("00000000-0000-0000-0000-000000000030"));
            assertThat(result.get().getEnvironmentId()).isEqualTo(ENV_ID);
            assertThat(result.get().getGmdContent()).isEqualTo(
                GraviteeMarkdown.of("<gmd-input name=\"company\" label=\"Company\" required=\"true\"/>")
            );
            assertThat(result.get().isEnabled()).isTrue();
        }

        @Test
        void should_return_empty_when_no_subscription_form_item_exists() {
            var result = service.findDefaultForEnvironmentId(ENV_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_empty_for_a_different_environment() {
            var contentId = PortalPageContentId.random();
            pageContentQueryService.initWith(
                List.of(PortalPageContentFixtures.aGraviteeMarkdownPageContent(contentId, "org-id", ENV_ID, "content"))
            );
            navigationItemsQueryService
                .storage()
                .add(PortalNavigationItemFixtures.aSubscriptionForm("00000000-0000-0000-0000-000000000031", contentId));

            var result = service.findDefaultForEnvironmentId("other-env");

            assertThat(result).isEmpty();
        }
    }
}
