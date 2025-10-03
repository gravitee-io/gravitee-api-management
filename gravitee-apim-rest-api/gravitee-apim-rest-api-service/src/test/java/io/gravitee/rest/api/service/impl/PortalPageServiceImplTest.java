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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalPageServiceImplTest {

    @Mock
    private PortalPageCrudService portalPageCrudService;

    @Mock
    private PortalPageContextCrudService portalPageContextCrudService;

    private PortalPageServiceImpl cut;

    @BeforeEach
    public void before() {
        cut = new PortalPageServiceImpl(portalPageCrudService, portalPageContextCrudService);
    }

    @Nested
    class createDefaultPortalPage {

        @Test
        @SneakyThrows
        void should_do_nothing_when_default_page_is_already_created() {
            when(
                portalPageContextCrudService.findAllIdsByContextTypeAndEnvironmentId(eq(PortalViewContext.HOMEPAGE), anyString())
            ).thenReturn(List.of(PageId.of("11111111-1111-1111-1111-111111111111")));

            cut.createDefaultPortalHomePage("envId");

            verify(portalPageContextCrudService, times(0)).create(any(), any(), anyString());
            verifyNoInteractions(portalPageCrudService);
        }

        @Test
        @SneakyThrows
        void should_create_context_and_page_when_no_default_page_is_created() {
            String envId = "envId";
            String portalPageId = "22222222-2222-2222-2222-222222222222";
            when(portalPageContextCrudService.findAllIdsByContextTypeAndEnvironmentId(PortalViewContext.HOMEPAGE, envId)).thenReturn(
                Collections.emptyList()
            );
            when(portalPageCrudService.create(any(PortalPage.class))).thenReturn(
                new PortalPage(PageId.of(portalPageId), new GraviteeMarkdown("content"))
            );

            cut.createDefaultPortalHomePage(envId);

            ArgumentCaptor<PageId> pageIdCaptor = ArgumentCaptor.forClass(PageId.class);
            ArgumentCaptor<PortalPageView> viewCaptor = ArgumentCaptor.forClass(PortalPageView.class);
            verify(portalPageContextCrudService).create(pageIdCaptor.capture(), viewCaptor.capture(), eq(envId));

            assertThat(pageIdCaptor.getValue().toString()).isEqualTo(portalPageId);
            assertThat(viewCaptor.getValue()).isNotNull();
            assertThat(viewCaptor.getValue().context()).isEqualTo(PortalViewContext.HOMEPAGE);
            assertThat(viewCaptor.getValue().published()).isTrue();

            verify(portalPageCrudService, times(1)).create(any(PortalPage.class));
        }
    }
}
