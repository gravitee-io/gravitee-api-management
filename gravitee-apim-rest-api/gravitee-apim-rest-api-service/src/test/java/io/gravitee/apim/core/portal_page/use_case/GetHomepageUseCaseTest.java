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

import inmemory.PortalPageContextCrudServiceInMemory;
import inmemory.PortalPageCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.domain_service.CheckContextExistsDomainService;
import io.gravitee.apim.core.portal_page.domain_service.GetHomepageContextDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageFactory;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetHomepageUseCaseTest {

    private PortalPageCrudServiceInMemory portalPageCrudService;
    private PortalPageContextCrudServiceInMemory portalPageContextCrudService;
    private GetHomepageUseCase useCase;

    @BeforeEach
    void setUp() {
        portalPageCrudService = new PortalPageCrudServiceInMemory();
        portalPageContextCrudService = new PortalPageContextCrudServiceInMemory();
        GetHomepageContextDomainService getHomepageContextDomainService = new GetHomepageContextDomainService(
            portalPageContextCrudService,
            portalPageCrudService
        );
        CheckContextExistsDomainService checkContextExistsDomainService = new CheckContextExistsDomainService(portalPageContextCrudService);

        useCase = new GetHomepageUseCase(getHomepageContextDomainService, checkContextExistsDomainService);
    }

    @Test
    void should_return_homepage_when_exists() {
        PageId pageId = PageId.random();
        PortalPage homepage = PortalPageFactory.create(pageId, new GraviteeMarkdown("home"));
        portalPageCrudService.initWith(java.util.List.of(homepage));
        String environmentId = "DEFAULT";
        var ctx = PortalPageContext
            .builder()
            .pageId(pageId.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(environmentId)
            .build();
        portalPageContextCrudService.initWith(java.util.List.of(ctx));
        GetHomepageUseCase.Output output = useCase.execute(new GetHomepageUseCase.Input(environmentId));
        assertThat(output.pages()).hasSize(1).containsExactly(homepage);
    }

    @Test
    void should_fail_when_homepage_does_not_exist() {
        assertThrows(Exception.class, () -> useCase.execute(new GetHomepageUseCase.Input("DEFAULT")));
    }
}
