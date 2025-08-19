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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.PortalPageCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageLocator;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreatePortalPageUseCaseTest {

    private PortalPageCrudServiceInMemory crudService;
    private CreatePortalPageUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = new PortalPageCrudServiceInMemory();
        useCase = new CreatePortalPageUseCase(crudService);
    }

    @Test
    void should_create_page_when_valid_and_unique() {
        CreatePortalPageUseCase.Input input = new CreatePortalPageUseCase.Input(UUID.randomUUID(), true, new GraviteeMarkdown("content"));
        CreatePortalPageUseCase.Output output = useCase.execute(input);
        PortalPage page = output.page();
        assertNotNull(page);
        assertNotNull(crudService.getByLocator(PageLocator.HOMEPAGE));
        assertEquals("content", page.pageContent().content());
    }

    @Test
    void should_fail_when_content_is_empty() {
        CreatePortalPageUseCase.Input input = new CreatePortalPageUseCase.Input(UUID.randomUUID(), true, new GraviteeMarkdown("   "));
        assertThrows(Exception.class, () -> useCase.execute(input));
        PortalPage page = crudService.getByLocator(new PageLocator(true));
        assertNull(page);
    }

    @Test
    void should_fail_when_locator_is_not_unique() {
        crudService.initWith(java.util.List.of(PortalPage.create(PageLocator.HOMEPAGE, new GraviteeMarkdown("existing content"))));
        CreatePortalPageUseCase.Input input = new CreatePortalPageUseCase.Input(UUID.randomUUID(), true, new GraviteeMarkdown("content"));
        assertThrows(Exception.class, () -> useCase.execute(input));
    }
}
