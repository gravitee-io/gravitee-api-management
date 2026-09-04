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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.exception.AgentTermsAndConditionsNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetAgentTermsAndConditionsUseCase {

    private final PortalNavigationApiVisibilityDomainService portalNavigationApiVisibilityDomainService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalPageContentQueryService portalPageContentQueryService;

    public Output execute(Input input) {
        if (!portalNavigationApiVisibilityDomainService.isApiVisibleToUser(input.environmentId(), input.apiId(), input.userId())) {
            throw new ApiNotFoundException(input.apiId());
        }

        var agent = findPublishedAgent(input.environmentId(), input.apiId()).orElseThrow(() ->
            new AgentTermsAndConditionsNotFoundException(input.apiId())
        );
        if (!agent.isTermsAndConditionsEnabled()) {
            throw new AgentTermsAndConditionsNotFoundException(input.apiId());
        }
        if (agent.getTermsAndConditionsPageContentId() == null) {
            throw new AgentTermsAndConditionsNotFoundException(input.apiId());
        }

        var content = portalPageContentQueryService
            .findById(agent.getTermsAndConditionsPageContentId())
            .orElseThrow(() -> new AgentTermsAndConditionsNotFoundException(input.apiId()));
        if (isBlankContent(content)) {
            throw new AgentTermsAndConditionsNotFoundException(input.apiId());
        }

        return new Output(content);
    }

    private Optional<PortalNavigationAgent> findPublishedAgent(String environmentId, String agentId) {
        return portalNavigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .published(true)
                    .root(false)
                    .type(PortalNavigationItemType.AGENT)
                    .build()
            )
            .stream()
            .filter(PortalNavigationAgent.class::isInstance)
            .map(PortalNavigationAgent.class::cast)
            .filter(item -> agentId.equals(item.getAgentId()))
            .findFirst();
    }

    private static boolean isBlankContent(PortalPageContent<?> content) {
        if (!(content instanceof GraviteeMarkdownPageContent markdown)) {
            return true;
        }
        var value = markdown.getContent() == null ? null : markdown.getContent().value();
        return value == null || value.isBlank();
    }

    @Builder
    public record Input(String environmentId, String apiId, @Nullable String userId) {}

    public record Output(PortalPageContent<?> content) {}
}
