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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageUseCase;
import io.gravitee.rest.api.portal.rest.model.PortalPageResponse;
import io.gravitee.rest.api.portal.rest.model.PortalPageWithDetails;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalPagesMapper {
    PortalPagesMapper INSTANCE = Mappers.getMapper(PortalPagesMapper.class);

    default PortalPageWithDetails.ContextEnum map(PortalViewContext portalViewContext) {
        return portalViewContext == null ? null : PortalPageWithDetails.ContextEnum.valueOf(portalViewContext.name());
    }

    default String map(PageId pageId) {
        return pageId == null ? null : pageId.toString();
    }

    default String map(Date date) {
        if (date == null) return null;
        return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(date.getTime()));
    }

    @Mapping(target = "id", source = "page.id")
    @Mapping(target = "content", source = "page.pageContent.content")
    @Mapping(target = "type", constant = "GRAVITEE_MARKDOWN")
    @Mapping(target = "context", source = "viewDetails.context")
    @Mapping(target = "createdAt", source = "page.createdAt")
    @Mapping(target = "updatedAt", source = "page.updatedAt")
    PortalPageWithDetails map(PortalPageWithViewDetails page);

    default PortalPageResponse map(List<PortalPageWithViewDetails> portalPagesWithViewDetails) {
        PortalPageResponse response = new PortalPageResponse();
        response.setPages(portalPagesWithViewDetails == null ? List.of() : portalPagesWithViewDetails.stream().map(this::map).toList());
        return response;
    }

    PortalPageResponse map(GetPortalPageUseCase.Output page);
}
