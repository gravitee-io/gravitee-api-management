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
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.PortalPageCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageFactory;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetHomepageUseCaseTest {

    private PortalPageCrudServiceInMemory crudService;
    private GetHomepageUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService = new PortalPageCrudServiceInMemory();
        useCase = new GetHomepageUseCase(crudService);
    }

    @Test
    void should_return_homepage_when_exists() {
        PortalPage homepage = PortalPageFactory.create(PageId.random(), new GraviteeMarkdown("home"));
        crudService.initWith(java.util.List.of(homepage));
        crudService.initWithContext(PortalViewContext.HOMEPAGE, homepage);
        GetHomepageUseCase.Output output = useCase.execute(new GetHomepageUseCase.Input("DEFAULT"));
        assertThat(output.pages()).hasSize(1).containsExactly(homepage);
    }

    @Test
    void should_fail_when_homepage_does_not_exist() {
        assertThrows(Exception.class, () -> useCase.execute(new GetHomepageUseCase.Input("DEFAULT")));
    }
}
