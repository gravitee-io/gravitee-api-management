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

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageElement;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalLink;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.use_case.CreatePortalPageUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageUseCase;
import io.gravitee.rest.api.management.v2.rest.model.CreateGraviteeMarkdownPage;
import io.gravitee.rest.api.management.v2.rest.model.CreateLinkPage;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalPageRequest;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalPagesMapper {
    PortalPagesMapper INSTANCE = Mappers.getMapper(PortalPagesMapper.class);

    default PortalPageWithDetails.ContextEnum map(PortalViewContext portalViewContext) {
        return PortalPageWithDetails.ContextEnum.valueOf(portalViewContext.toString());
    }

    default String map(PageId pageId) {
        return pageId.toString();
    }

    @Mapping(target = "id", source = "page.id")
    @Mapping(target = "content", source = "page.pageContent.content")
    @Mapping(target = "type", constant = "GRAVITEE_MARKDOWN")
    @Mapping(target = "context", source = "viewDetails.context")
    @Mapping(target = "published", source = "viewDetails.published")
    PortalPageWithDetails map(PortalPageWithViewDetails page);

    default io.gravitee.rest.api.management.v2.rest.model.PortalPagesResponse map(
        List<PortalPageWithViewDetails> portalPagesWithViewDetails
    ) {
        io.gravitee.rest.api.management.v2.rest.model.PortalPagesResponse response =
            new io.gravitee.rest.api.management.v2.rest.model.PortalPagesResponse();
        response.pages(portalPagesWithViewDetails.stream().map(this::map).toList());
        return response;
    }

    io.gravitee.rest.api.management.v2.rest.model.PortalPagesResponse map(GetPortalPageUseCase.Output page);

    @Mapping(target = "environmentId", source = "environmentId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "context", source = "request.context")
    @Mapping(target = "order", source = "request.order")
    @Mapping(target = "element", source = "request", qualifiedByName = "graviteeMarkdownToElement")
    CreatePortalPageUseCase.Input map(CreateGraviteeMarkdownPage request, String environmentId);

    @Mapping(target = "environmentId", source = "environmentId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "context", source = "request.context")
    @Mapping(target = "order", source = "request.order")
    @Mapping(target = "element", source = "request", qualifiedByName = "linkToElement")
    CreatePortalPageUseCase.Input map(CreateLinkPage request, String environmentId);

    default CreatePortalPageUseCase.Input map(CreatePortalPageRequest request, String environmentId) {
        if (request == null || environmentId == null) {
            return null;
        }

        return switch (request.getType()) {
            case GRAVITEE_MARKDOWN -> map((CreateGraviteeMarkdownPage) request, environmentId);
            case LINK -> map((CreateLinkPage) request, environmentId);
            case SECTION -> throw new UnsupportedOperationException("Section pages are not supported");
        };
    }

    @Named("graviteeMarkdownToElement")
    @SuppressWarnings("unused")
    default PageElement<?> graviteeMarkdownToElement(CreateGraviteeMarkdownPage request) {
        if (request == null) {
            return null;
        }
        return new PortalPage(PageId.random(), new GraviteeMarkdown(request.getContent()));
    }

    @Named("linkToElement")
    @SuppressWarnings("unused")
    default PageElement<?> linkToElement(CreateLinkPage request) {
        if (request == null) {
            return null;
        }
        try {
            URL url = request.getUrl().toURL();
            return new PortalLink(PageId.random(), url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
