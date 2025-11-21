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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePortalNavigationItemUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";

    private PortalNavigationItemCrudService portalNavigationItemCrudService;
    private PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private UpdatePortalNavigationItemUseCase useCase;

    @BeforeEach
    void setUp() {
        portalNavigationItemCrudService = mock(PortalNavigationItemCrudService.class);
        portalNavigationItemsQueryService = mock(PortalNavigationItemsQueryService.class);
        useCase = new UpdatePortalNavigationItemUseCase(portalNavigationItemCrudService, portalNavigationItemsQueryService);
    }

    @Test
    void should_update_the_portal_navigation_item() {
        var existingItem = new PortalNavigationFolder(
            PortalNavigationItemId.of(ITEM_ID),
            ORG_ID,
            ENV_ID,
            "old name",
            PortalArea.TOP_NAVBAR,
            1
        );
        when(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(ITEM_ID))).thenReturn(
            existingItem
        );

        when(portalNavigationItemCrudService.update(existingItem)).thenReturn(existingItem);

        var input = new UpdatePortalNavigationItemUseCase.Input(
            ORG_ID,
            ENV_ID,
            CreatePortalNavigationItem.builder().id(PortalNavigationItemId.of(ITEM_ID)).title("new name").build()
        );
        var result = useCase.execute(input);

        assertThat(result.updatedItem()).isEqualTo(existingItem);
        assertThat(result.updatedItem().getTitle()).isEqualTo("new name");
    }

    @Test
    void should_throw_exception_if_item_not_found() {
        when(portalNavigationItemsQueryService.findByIdAndEnvironmentId(ENV_ID, PortalNavigationItemId.of(ITEM_ID))).thenReturn(null);

        var input = new UpdatePortalNavigationItemUseCase.Input(
            ORG_ID,
            ENV_ID,
            CreatePortalNavigationItem.builder().id(PortalNavigationItemId.of(ITEM_ID)).title("new name").build()
        );
        var throwable = catchThrowable(() -> useCase.execute(input));

        assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Portal navigation item [" + ITEM_ID + "] not found.");
    }
}
