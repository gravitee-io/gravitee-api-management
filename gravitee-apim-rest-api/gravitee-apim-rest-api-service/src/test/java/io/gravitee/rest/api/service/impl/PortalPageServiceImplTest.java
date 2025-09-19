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

import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.PortalPage;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
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
    private PortalPageRepository portalPageRepository;

    @Mock
    private PortalPageContextRepository portalPageContextRepository;

    private PortalPageServiceImpl cut;

    @BeforeEach
    public void before() {
        cut = new PortalPageServiceImpl(portalPageRepository, portalPageContextRepository);
    }

    @Nested
    class createDefaultPortalPage {

        @Test
        @SneakyThrows
        void should_do_nothing_when_default_page_is_already_created() {
            when(
                portalPageContextRepository.findAllByContextTypeAndEnvironmentId(eq(PortalPageContextType.HOMEPAGE), anyString())
            ).thenReturn(List.of(PortalPageContext.builder().build()));

            cut.createDefaultPortalHomePage("envId");

            verify(portalPageContextRepository, times(0)).create(any());
            verifyNoInteractions(portalPageRepository);
        }

        @Test
        @SneakyThrows
        void should_create_context_and_page_when_no_default_page_is_created() {
            String envId = "envId";
            String portalPageId = "portalPageId";
            when(portalPageContextRepository.findAllByContextTypeAndEnvironmentId(PortalPageContextType.HOMEPAGE, envId)).thenReturn(
                Collections.emptyList()
            );
            when(portalPageRepository.create(any(PortalPage.class))).thenReturn(PortalPage.builder().id(portalPageId).build());

            cut.createDefaultPortalHomePage(envId);

            ArgumentCaptor<PortalPageContext> portalPageContextCaptor = ArgumentCaptor.forClass(PortalPageContext.class);
            verify(portalPageContextRepository).create(portalPageContextCaptor.capture());

            ArgumentCaptor<PortalPage> portalPageCaptor = ArgumentCaptor.forClass(PortalPage.class);
            verify(portalPageRepository).create(portalPageCaptor.capture());

            var portalPage = portalPageCaptor.getValue();
            assertThat(portalPage)
                .hasFieldOrPropertyWithValue("environmentId", envId)
                .hasFieldOrPropertyWithValue("name", "Default Portal Page")
                .hasNoNullFieldsOrProperties();

            assertThat(portalPageContextCaptor.getValue())
                .hasFieldOrPropertyWithValue("pageId", portalPageId)
                .hasFieldOrPropertyWithValue("contextType", PortalPageContextType.HOMEPAGE)
                .hasFieldOrPropertyWithValue("environmentId", envId)
                .hasFieldOrPropertyWithValue("published", true)
                .hasNoNullFieldsOrProperties();
        }
    }
}
