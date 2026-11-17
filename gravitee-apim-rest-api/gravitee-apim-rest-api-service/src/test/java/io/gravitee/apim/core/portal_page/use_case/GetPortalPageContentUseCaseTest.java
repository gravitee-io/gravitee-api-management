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
package io.gravitee.apim.core.portal_page.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static fixtures.core.model.PortalPageContentFixtures.ENVIRONMENT_ID;
import static fixtures.core.model.PortalPageContentFixtures.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import fixtures.core.model.PortalPageContentFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalPageContentUseCaseTest {

    private static final PortalPageContentId CONTENT_ID = PortalPageContentId.of("00000000-0000-0000-0000-000000000001");

    private GetPortalPageContentUseCase useCase;
    private PortalPageContentQueryServiceInMemory queryService;
    private PortalNavigationItemsQueryServiceInMemory navigation;

    @BeforeEach
    void setUp() {
        queryService = new PortalPageContentQueryServiceInMemory();
        navigation = new PortalNavigationItemsQueryServiceInMemory();
        var page = PortalNavigationItemFixtures.aPage("Welcome", null)
            .toBuilder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENVIRONMENT_ID)
            .portalPageContentId(CONTENT_ID)
            .build();
        page.markAsRoot();
        navigation.initWith(List.of(page));
        useCase = new GetPortalPageContentUseCase(
            PortalNavigationUseCaseTestFixtures.access(
                ORGANIZATION_ID,
                ENVIRONMENT_ID,
                navigation,
                queryService,
                new ApiCrudServiceInMemory()
            )
        );

        queryService.initWith(PortalPageContentFixtures.samplePortalPageContents());
    }

    @Test
    void should_reject_missing_content_even_when_the_navigation_association_is_accessible() {
        queryService.reset();

        assertThatThrownBy(() ->
            useCase.execute(new GetPortalPageContentUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, CONTENT_ID, PORTAL_REFERENCE))
        ).isInstanceOf(PageContentNotFoundException.class);
    }

    @Test
    void should_not_return_existing_content_associated_with_another_portal_in_the_same_environment() {
        navigation
            .storage()
            .getFirst()
            .changeOwner(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")));

        assertThatThrownBy(() ->
            useCase.execute(new GetPortalPageContentUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, CONTENT_ID, PORTAL_REFERENCE))
        ).isInstanceOf(PageContentNotFoundException.class);
    }

    @Test
    void should_return_portal_page_content_when_found() {
        var result = useCase.execute(new GetPortalPageContentUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, CONTENT_ID, PORTAL_REFERENCE));

        assertThat(result.content()).isInstanceOf(GraviteeMarkdownPageContent.class);
        assertThat(result.content().getId()).isEqualTo(CONTENT_ID);
        assertThat(((GraviteeMarkdownPageContent) result.content()).getContent().value()).isEqualTo(
            "# Welcome\n\nThis is a sample page content."
        );
    }

    @Test
    void should_throw_when_content_not_found() {
        var unknownId = PortalPageContentId.of("00000000-0000-0000-0000-000000000002");

        assertThatThrownBy(() ->
            useCase.execute(new GetPortalPageContentUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, unknownId, PORTAL_REFERENCE))
        )
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found")
            .extracting(ex -> ((PageContentNotFoundException) ex).getId())
            .isEqualTo(unknownId.json());
    }

    @Test
    void should_throw_when_content_belongs_to_another_environment() {
        queryService.initWith(
            List.of(
                PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                    CONTENT_ID,
                    ORGANIZATION_ID,
                    "other-env",
                    PortalPageContentFixtures.CONTENT
                )
            )
        );

        assertThatThrownBy(() ->
            useCase.execute(new GetPortalPageContentUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, CONTENT_ID, PORTAL_REFERENCE))
        )
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found")
            .extracting(ex -> ((PageContentNotFoundException) ex).getId())
            .isEqualTo(CONTENT_ID.json());
    }

    @Test
    void should_throw_when_content_belongs_to_another_organization() {
        queryService.initWith(
            List.of(
                PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                    CONTENT_ID,
                    "other-org",
                    ENVIRONMENT_ID,
                    PortalPageContentFixtures.CONTENT
                )
            )
        );

        assertThatThrownBy(() ->
            useCase.execute(new GetPortalPageContentUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, CONTENT_ID, PORTAL_REFERENCE))
        )
            .isInstanceOf(PageContentNotFoundException.class)
            .hasMessage("Page content not found")
            .extracting(ex -> ((PageContentNotFoundException) ex).getId())
            .isEqualTo(CONTENT_ID.json());
    }
}
