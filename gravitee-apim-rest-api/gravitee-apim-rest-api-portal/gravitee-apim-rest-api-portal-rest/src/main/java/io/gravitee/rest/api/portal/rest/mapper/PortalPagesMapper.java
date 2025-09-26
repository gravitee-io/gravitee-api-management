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

import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageUseCase;
import io.gravitee.rest.api.portal.rest.model.PortalPageResponse;
import io.gravitee.rest.api.portal.rest.model.PortalPageWithDetails;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PortalPagesMapper {

    private PortalPagesMapper() {
    }

    public static PortalPageResponse map(GetPortalPageUseCase.Output output) {
        PortalPageResponse response = new PortalPageResponse();
        response.setPages(map(output.pages()));
        return response;
    }

    public static List<PortalPageWithDetails> map(List<PortalPageWithViewDetails> portalPagesWithViewDetails) {
        if (portalPagesWithViewDetails == null) {
            return List.of();
        }
        return portalPagesWithViewDetails.stream().filter(Objects::nonNull).map(PortalPagesMapper::map).collect(Collectors.toList());
    }

    public static PortalPageWithDetails map(PortalPageWithViewDetails page) {
        if (page == null) {
            return null;
        }
        var dto = new PortalPageWithDetails();
        dto.setId(page.page().getId().toString());
        dto.setContent(page.page().getPageContent().content());
        dto.setType("GRAVITEE_MARKDOWN");
        dto.setContext(map(page.viewDetails().context()));
        dto.setPublished(page.viewDetails().published());
        return dto;
    }

    private static PortalPageWithDetails.ContextEnum map(io.gravitee.apim.core.portal_page.model.PortalViewContext portalViewContext) {
        return PortalPageWithDetails.ContextEnum.valueOf(portalViewContext.name());
    }
}
