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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.use_case.GetHomepageUseCase;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPagesMapperTest {

    @Test
    void map_portal_page_with_view_details_should_return_response() {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("Sample Content"));
        PortalPageView view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        PortalPageWithViewDetails input = new PortalPageWithViewDetails(page, view);
        var homepage = new GetHomepageUseCase.Output(input);
        var portalPage = PortalPagesMapper.INSTANCE.map(homepage);
        assertThat(portalPage).isNotNull();
        assertThat(portalPage.getContent()).isEqualTo("Sample Content");
        assertThat(portalPage.getType()).isEqualTo(
            io.gravitee.rest.api.management.v2.rest.model.PortalPageResponse.TypeEnum.GRAVITEE_MARKDOWN
        );
        assertThat(portalPage.getId()).isEqualTo(page.getId().toString());
        assertThat(portalPage.getContext()).isEqualTo(
            io.gravitee.rest.api.management.v2.rest.model.PortalPageResponse.ContextEnum.HOMEPAGE
        );
    }
}
