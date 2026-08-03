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

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalLinkUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String PORTAL_ID = "11111111-1111-1111-1111-111111111111";
    private static final String LINK_HRID = "external-docs";
    private static final PortalNavigationItemId LINK_ID = PortalNavigationItemId.of(
        HRIDToUUID.portalLink().context(AUDIT_INFO).portal(PORTAL_ID).hrid(LINK_HRID).id()
    );

    private final PortalNavigationItemsQueryServiceInMemory queryService = new PortalNavigationItemsQueryServiceInMemory();
    private GetPortalLinkUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPortalLinkUseCase(queryService);
    }

    @AfterEach
    void tearDown() {
        queryService.reset();
    }

    @Test
    void should_return_the_link_when_found() {
        queryService.initWith(List.of(aLink()));

        var output = useCase.execute(new GetPortalLinkUseCase.Input(AUDIT_INFO, LINK_ID));

        assertThat(output.link().getId()).isEqualTo(LINK_ID);
        assertThat(output.link().getTitle()).isEqualTo("External Docs");
        assertThat(output.link().getUrl()).isEqualTo("https://docs.example.com");
    }

    @Test
    void should_throw_when_not_found() {
        var throwable = catchThrowable(() -> useCase.execute(new GetPortalLinkUseCase.Input(AUDIT_INFO, LINK_ID)));

        assertThat(throwable).isInstanceOf(PortalLinkNotFoundException.class);
    }

    @Test
    void should_throw_when_the_id_resolves_to_a_foreign_non_link_item() {
        queryService.initWith(
            List.of(
                PortalNavigationFolder.builder()
                    .id(LINK_ID)
                    .organizationId(AUDIT_INFO.organizationId())
                    .environmentId(AUDIT_INFO.environmentId())
                    .title("external-docs")
                    .segment("external-docs")
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .build()
            )
        );

        var throwable = catchThrowable(() -> useCase.execute(new GetPortalLinkUseCase.Input(AUDIT_INFO, LINK_ID)));

        assertThat(throwable).isInstanceOf(PortalLinkNotFoundException.class);
    }

    private static PortalNavigationLink aLink() {
        return PortalNavigationLink.builder()
            .id(LINK_ID)
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId(AUDIT_INFO.environmentId())
            .title("External Docs")
            .segment(LINK_HRID)
            .area(PortalArea.TOP_NAVBAR)
            .order(3)
            .url("https://docs.example.com")
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }
}
