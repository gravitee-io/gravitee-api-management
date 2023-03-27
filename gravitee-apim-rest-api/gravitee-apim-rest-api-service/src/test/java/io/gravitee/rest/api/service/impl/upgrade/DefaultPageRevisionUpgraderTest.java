/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.PageService;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPageRevisionUpgraderTest {

    @Mock
    private PageService pageService;

    @Mock
    private PageRevisionService pageRevisionService;

    @InjectMocks
    private final DefaultPageRevisionUpgrader upgrader = new DefaultPageRevisionUpgrader();

    @Test
    public void shouldCreateRevision() {
        Page<PageEntity> pages = mock(Page.class);
        when(pages.getContent()).thenReturn(List.of(new PageEntity()), List.of(new PageEntity()), List.of());
        when(pageService.findAll(any())).thenReturn(pages);
        when(pageService.shouldHaveRevision(any())).thenReturn(true);
        when(pageRevisionService.findAll(any())).thenReturn(new Page<>(List.of(), 1, 0, 0));
        upgrader.upgrade();
        verify(pageRevisionService, times(1)).create(any());
    }
}
