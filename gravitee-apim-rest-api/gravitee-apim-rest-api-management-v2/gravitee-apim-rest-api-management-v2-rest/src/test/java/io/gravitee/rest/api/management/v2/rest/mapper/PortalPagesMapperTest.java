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
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageUseCase;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPagesMapperTest {

    @Test
    void should_map_portal_page_with_view_details_to_rest_model() {
        var pageId = PageId.of("123e4567-e89b-12d3-a456-426614174000");
        var page = new PortalPage(pageId, new GraviteeMarkdown("markdown content"));
        var view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        var withDetails = new PortalPageWithViewDetails(page, view);

        var rest = PortalPagesMapper.INSTANCE.map(withDetails);

        assertThat(rest).isNotNull();
        assertThat(rest.getId()).isEqualTo(pageId.toString());
        assertThat(rest.getContent()).isEqualTo("markdown content");
        assertThat(rest.getType()).isEqualTo(PortalPageWithDetails.TypeEnum.GRAVITEE_MARKDOWN);
        assertThat(rest.getContext()).isEqualTo(PortalPageWithDetails.ContextEnum.HOMEPAGE);
        assertThat(rest.getPublished()).isEqualTo(view.published());
    }

    @Test
    void should_map_list_of_pages_to_portal_page_response() {
        var page1 = new PortalPage(PageId.of("11111111-1111-1111-1111-111111111111"), new GraviteeMarkdown("content1"));
        var view1 = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        var withDetails1 = new PortalPageWithViewDetails(page1, view1);

        var page2 = new PortalPage(PageId.of("22222222-2222-2222-2222-222222222222"), new GraviteeMarkdown("content2"));
        var view2 = new PortalPageView(PortalViewContext.HOMEPAGE, false);
        var withDetails2 = new PortalPageWithViewDetails(page2, view2);

        var response = PortalPagesMapper.INSTANCE.map(java.util.List.of(withDetails1, withDetails2));

        assertThat(response).isNotNull();
        assertThat(response.getPages()).hasSize(2);
        assertThat(response.getPages().get(0).getId()).isEqualTo(page1.getId().toString());
        assertThat(response.getPages().get(1).getId()).isEqualTo(page2.getId().toString());
    }

    @Test
    void should_map_getPortalPage_usecase_output_to_portal_page_response() {
        var page = new PortalPage(PageId.of("33333333-3333-3333-3333-333333333333"), new GraviteeMarkdown("content3"));
        var view = new PortalPageView(PortalViewContext.HOMEPAGE, false);
        var withDetails = new PortalPageWithViewDetails(page, view);

        var output = new GetPortalPageUseCase.Output(java.util.List.of(withDetails));

        var response = PortalPagesMapper.INSTANCE.map(output);

        assertThat(response).isNotNull();
        assertThat(response.getPages()).hasSize(1);
        assertThat(response.getPages().getFirst().getId()).isEqualTo(page.getId().toString());
    }
}
