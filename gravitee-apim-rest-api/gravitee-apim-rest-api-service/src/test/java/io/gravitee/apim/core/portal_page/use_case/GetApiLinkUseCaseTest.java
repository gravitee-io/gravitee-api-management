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

import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetApiLinkUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String API_ID = "00000000-0000-0000-0000-0000000000a1";

    private final PortalNavigationItemsCrudServiceInMemory navItemCrud = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navItemQuery = new PortalNavigationItemsQueryServiceInMemory(
        navItemCrud.storage()
    );

    private GetApiLinkUseCase useCase;

    @BeforeEach
    void setUp() {
        navItemCrud.reset();
        useCase = new GetApiLinkUseCase(navItemQuery);
    }

    @Test
    void returns_an_api_attached_link() {
        var linkId = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs");
        navItemCrud.create(apiLinkRow(linkId));

        var output = useCase.execute(new GetApiLinkUseCase.Input(AUDIT_INFO, linkId));

        assertThat(output.link().getId()).isEqualTo(linkId);
    }

    @Test
    void throws_when_the_link_does_not_exist() {
        var linkId = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "missing");

        var throwable = catchThrowable(() -> useCase.execute(new GetApiLinkUseCase.Input(AUDIT_INFO, linkId)));

        assertThat(throwable).isInstanceOf(PortalLinkNotFoundException.class);
    }

    @Test
    void throws_when_the_link_is_attached_to_a_portal_rather_than_an_api() {
        var linkId = PortalNavigationItemId.forApiLink(AUDIT_INFO, API_ID, "external-docs");
        navItemCrud.create(portalLinkRow(linkId));

        var throwable = catchThrowable(() -> useCase.execute(new GetApiLinkUseCase.Input(AUDIT_INFO, linkId)));

        assertThat(throwable).isInstanceOf(PortalLinkNotFoundException.class);
    }

    private PortalNavigationLink apiLinkRow(PortalNavigationItemId id) {
        return linkRow(id, new AutomationMetadata(AutomationMetadata.ReferenceType.API, API_ID, null, Optional.empty(), Optional.empty()));
    }

    private PortalNavigationLink portalLinkRow(PortalNavigationItemId id) {
        return linkRow(
            id,
            new AutomationMetadata(AutomationMetadata.ReferenceType.PORTAL, "some-portal-id", null, Optional.empty(), Optional.empty())
        );
    }

    private PortalNavigationLink linkRow(PortalNavigationItemId id, AutomationMetadata automationMetadata) {
        return PortalNavigationLink.builder()
            .id(id)
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("external-docs")
            .segment("external-docs")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .url("https://example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .automationMetadata(automationMetadata)
            .build();
    }
}
