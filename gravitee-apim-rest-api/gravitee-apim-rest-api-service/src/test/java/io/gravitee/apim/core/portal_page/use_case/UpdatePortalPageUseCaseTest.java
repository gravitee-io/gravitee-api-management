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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalPageContextCrudServiceInMemory;
import inmemory.PortalPageCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.exception.PortalPageSpecificationException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdatePortalPageUseCaseTest {

    private final PortalPageCrudServiceInMemory portalPageCrudService = new PortalPageCrudServiceInMemory();
    private final PortalPageContextCrudServiceInMemory portalPageContextCrudService = new PortalPageContextCrudServiceInMemory();

    private final UpdatePortalPageUseCase updatePortalPageUseCase = new UpdatePortalPageUseCase(
        portalPageCrudService,
        portalPageContextCrudService
    );

    @BeforeEach
    void setUp() {
        portalPageCrudService.reset();
        portalPageContextCrudService.reset();
    }

    @Test
    void should_update_page_content_when_page_exists() {
        PageId pageId = PageId.random();
        PortalPage existingPage = new PortalPage(pageId, new GraviteeMarkdown("Old content"));
        portalPageCrudService.initWith(List.of(existingPage));

        String updatedContent = "Updated content";
        var result = updatePortalPageUseCase.execute(new UpdatePortalPageUseCase.Input("env", pageId.toString(), updatedContent));

        assertThat(result).isNotNull();
        assertThat(result.portalPage().page().getPageContent().content()).isEqualTo(updatedContent);
    }

    @Test
    void should_return_throw_when_page_does_not_exist() {
        PageId pageId = PageId.random();

        assertThatThrownBy(() -> updatePortalPageUseCase.execute(new UpdatePortalPageUseCase.Input("env", pageId.toString(), "New content"))
            )
            .isInstanceOf(PortalPageSpecificationException.class);
    }
}
