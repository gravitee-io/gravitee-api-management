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

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.aFolder;
import static fixtures.core.model.PortalNavigationItemFixtures.aPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetApiPortalNavigationItemUseCaseTest {

    private static final String API_ID = "api-a";
    private static final String OTHER_API_ID = "api-b";

    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
    private final GetApiPortalNavigationItemUseCase useCase = new GetApiPortalNavigationItemUseCase(
        new ApiOwnedNavigationDomainService(queryService, new PortalNavigationItemsCrudServiceInMemory()),
        new PortalNavigationItemSourceDomainServiceInMemory()
    );

    @Test
    void should_return_an_item_owned_by_the_api() {
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).build();
        queryService.initWith(List.of(page));

        assertThat(get(page.getId())).isEqualTo(page);
    }

    @Test
    void should_return_an_unpublished_private_item_of_an_api_that_is_not_listed() {
        var draft = aPage("Draft", null)
            .toBuilder()
            .reference(ownedBy(API_ID))
            .published(false)
            .visibility(PortalVisibility.PRIVATE)
            .build();
        queryService.initWith(List.of(draft));

        assertThat(get(draft.getId())).isEqualTo(draft);
    }

    @Test
    void should_return_the_stored_parent() {
        var folder = aFolder("Auth").toBuilder().reference(ownedBy(API_ID)).build();
        var page = aPage("Setup", folder.getId()).toBuilder().reference(ownedBy(API_ID)).build();
        queryService.initWith(List.of(folder, page));

        assertThat(get(page.getId()).getParentId()).isEqualTo(folder.getId());
    }

    @Test
    void should_mask_the_sensitive_data_of_a_source() {
        var source = PortalNavigationItemSource.builder()
            .sourceType("github")
            .sourceConfiguration(PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA)
            .build();
        var page = aPage("Overview", null).toBuilder().reference(ownedBy(API_ID)).source(source).build();
        queryService.initWith(List.of(page));

        assertThat(get(page.getId()).getSource().getSourceConfiguration()).isEqualTo(
            PortalNavigationItemSourceDomainServiceInMemory.SENSITIVE_DATA_REPLACEMENT
        );
    }

    @Test
    void should_reject_an_item_owned_by_another_api() {
        var foreign = aPage("Other overview", null).toBuilder().reference(ownedBy(OTHER_API_ID)).build();
        queryService.initWith(List.of(foreign));

        assertThatThrownBy(() -> get(foreign.getId())).isInstanceOf(PortalNavigationItemNotFoundException.class);
    }

    @Test
    void should_reject_a_portal_owned_item() {
        var section = aFolder("APIs");
        queryService.initWith(List.of(section));

        assertThatThrownBy(() -> get(section.getId())).isInstanceOf(PortalNavigationItemNotFoundException.class);
    }

    private PortalNavigationItem get(PortalNavigationItemId itemId) {
        return useCase.execute(new GetApiPortalNavigationItemUseCase.Input(ENV_ID, API_ID, itemId)).item();
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
