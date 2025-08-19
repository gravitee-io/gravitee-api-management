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

import static org.junit.jupiter.api.Assertions.*;

import inmemory.PortalPageCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageLocator;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SetHomepageUseCaseTest {

    private PortalPageCrudServiceInMemory crudService;
    private SetHomepageUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = new PortalPageCrudServiceInMemory();
        useCase = new SetHomepageUseCase(crudService);
    }

    @Test
    void should_set_homepage_when_page_exists() {
        PageLocator locator = new PageLocator(true);
        PortalPage page = PortalPage.create(locator, new GraviteeMarkdown("home"));
        crudService.initWith(java.util.List.of(page));
        SetHomepageUseCase.Input input = new SetHomepageUseCase.Input(locator);
        SetHomepageUseCase.Output output = useCase.execute(input);
        assertEquals(page, output.page());
        assertEquals(page, crudService.getHomepage());
    }

    @Test
    void should_fail_when_page_does_not_exist() {
        PageLocator locator = new PageLocator(true);
        SetHomepageUseCase.Input input = new SetHomepageUseCase.Input(locator);
        assertThrows(Exception.class, () -> useCase.execute(input));
        assertNull(crudService.getHomepage());
    }
}
