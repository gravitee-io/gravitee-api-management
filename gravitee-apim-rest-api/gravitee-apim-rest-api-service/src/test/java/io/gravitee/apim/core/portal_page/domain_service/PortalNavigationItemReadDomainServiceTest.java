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
package io.gravitee.apim.core.portal_page.domain_service;

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.PORTAL_REFERENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.ApiPortalSearchQueryServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortalNavigationItemReadDomainServiceTest {

    private final PortalNavigationItemsQueryServiceInMemory queryService = spy(new PortalNavigationItemsQueryServiceInMemory());
    private final PortalNavigationAccessDomainService accessService = mock(PortalNavigationAccessDomainService.class);
    private final PortalNavigationItemReadDomainService reader = new PortalNavigationItemReadDomainService(
        queryService,
        List.of(),
        new PortalNavigationItemSourceDomainServiceInMemory(),
        new ApiPortalSearchQueryServiceInMemory(),
        new ApiOwnedNavigationDomainService(queryService, new PortalNavigationItemsCrudServiceInMemory()),
        new PortalNavigationItemRenderingDomainService(queryService),
        accessService
    );

    @Test
    void should_preserve_environment_wide_portal_reads_without_resolving_a_default_portal() {
        var first = PortalNavigationItemFixtures.aFolder("First portal");
        var second = PortalNavigationItemFixtures.aFolder("Second portal");
        second.changeOwner(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")));
        var shared = PortalNavigationItemFixtures.aFolder("Shared API documentation");
        shared.changeOwner(new NavigationItemReference.ApiReference("api-id"));
        queryService.initWith(List.of(first, second, shared));
        var criteria = PortalNavigationItemQueryCriteria.builder().environmentId(ENV_ID).area(PortalArea.TOP_NAVBAR).build();

        var items = reader.read(criteria, PortalNavigationItemViewerContext.forPortal(false), false);

        assertThat(items).containsExactlyInAnyOrder(first, second);
        verifyNoInteractions(accessService);
    }

    @Test
    void should_filter_a_scoped_read_without_mutating_the_callers_criteria() {
        var first = PortalNavigationItemFixtures.aFolder("First portal");
        var second = PortalNavigationItemFixtures.aFolder("Second portal");
        second.changeOwner(new NavigationItemReference.PortalReference(PortalId.of("22222222-2222-2222-2222-222222222222")));
        var foreignChild = PortalNavigationItemFixtures.aPage("Foreign owner", first.getId());
        foreignChild.changeOwner(second.getReference());
        queryService.initWith(List.of(first, second, foreignChild));
        var criteria = PortalNavigationItemQueryCriteria.builder()
            .environmentId(ENV_ID)
            .area(PortalArea.TOP_NAVBAR)
            .reference(PORTAL_REFERENCE)
            .build();

        var items = reader.read(criteria, PortalNavigationItemViewerContext.forConsole(), true);

        assertThat(items).containsExactly(first);
        assertThat(criteria.getParentId()).isNull();
        assertThat(criteria.getReference()).isEqualTo(PORTAL_REFERENCE);
        assertThat(criteria.getPublished()).isNull();
        assertThat(criteria.getVisibility()).isNull();
        verify(queryService, never()).findByIdAndEnvironmentId(any(), any());
    }
}
