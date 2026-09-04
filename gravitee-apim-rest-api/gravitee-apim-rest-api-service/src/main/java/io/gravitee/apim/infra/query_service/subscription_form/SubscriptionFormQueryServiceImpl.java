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
package io.gravitee.apim.infra.query_service.subscription_form;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of SubscriptionFormQueryService, reading the environment-default
 * subscription form through the {@code PortalNavigationItem}/{@code PortalPageContent} model
 * (PORTAL-164) instead of the retired {@code subscription_forms} table/collection. Every caller of
 * this interface (the Get use cases, {@code SubscriptionValidationServiceImpl}, the still-live
 * management-v2 REST resources) keeps working unchanged — only the storage underneath changed.
 *
 * @author Gravitee.io Team
 */
@Component
public class SubscriptionFormQueryServiceImpl implements SubscriptionFormQueryService {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService pageContentQueryService;

    public SubscriptionFormQueryServiceImpl(
        PortalNavigationItemsQueryService navigationItemsQueryService,
        PortalPageContentQueryService pageContentQueryService
    ) {
        this.navigationItemsQueryService = navigationItemsQueryService;
        this.pageContentQueryService = pageContentQueryService;
    }

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String environmentId, SubscriptionFormId subscriptionFormId) {
        return findDefaultForEnvironmentId(environmentId).filter(form -> form.getId().equals(subscriptionFormId));
    }

    @Override
    public Optional<SubscriptionForm> findDefaultForEnvironmentId(String environmentId) {
        return navigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, PortalArea.SUBSCRIPTION_FORM)
            .stream()
            .findFirst()
            .filter(PortalNavigationSubscriptionForm.class::isInstance)
            .map(PortalNavigationSubscriptionForm.class::cast)
            .map(this::toSubscriptionForm);
    }

    private SubscriptionForm toSubscriptionForm(PortalNavigationSubscriptionForm navigationItem) {
        var content = pageContentQueryService
            .findById(navigationItem.getPortalPageContentId())
            .orElseThrow(() -> new PageContentNotFoundException(navigationItem.getPortalPageContentId().toString()));
        if (!(content instanceof GraviteeMarkdownPageContent gmdContent)) {
            throw new TechnicalDomainException(
                "Subscription form navigation item [%s] references non-GRAVITEE_MARKDOWN content".formatted(navigationItem.getId())
            );
        }
        return SubscriptionForm.builder()
            .id(SubscriptionFormId.of(navigationItem.getId().toString()))
            .environmentId(navigationItem.getEnvironmentId())
            .gmdContent(gmdContent.getContent())
            .enabled(Boolean.TRUE.equals(navigationItem.getPublished()))
            .validationConstraints(navigationItem.getValidationConstraints())
            .build();
    }
}
