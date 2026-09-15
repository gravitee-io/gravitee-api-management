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
package io.gravitee.apim.infra.crud_service.subscription_form;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of SubscriptionFormCrudService, writing the environment-default
 * subscription form through the {@code PortalNavigationItem}/{@code PortalPageContent} model
 * (PORTAL-164) instead of the retired {@code subscription_forms} table/collection. The one
 * remaining caller of this interface, {@code CreateDefaultSubscriptionFormUseCase}, keeps working
 * unchanged — only the storage underneath changed.
 *
 * @author Gravitee.io Team
 */
@Component
public class SubscriptionFormCrudServiceImpl implements SubscriptionFormCrudService {

    private final EnvironmentRepository environmentRepository;
    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalPageContentCrudService pageContentCrudService;

    public SubscriptionFormCrudServiceImpl(
        @Lazy EnvironmentRepository environmentRepository,
        PortalNavigationItemCrudService navigationItemCrudService,
        PortalPageContentCrudService pageContentCrudService
    ) {
        this.environmentRepository = environmentRepository;
        this.navigationItemCrudService = navigationItemCrudService;
        this.pageContentCrudService = pageContentCrudService;
    }

    @Override
    public SubscriptionForm create(SubscriptionForm subscriptionForm) {
        var environmentId = subscriptionForm.getEnvironmentId();
        var organizationId = findOrganizationId(environmentId);

        var content = pageContentCrudService.create(
            new GraviteeMarkdownPageContent(PortalPageContentId.random(), organizationId, environmentId, subscriptionForm.getGmdContent())
        );

        var auditInfo = AuditInfo.builder().organizationId(organizationId).environmentId(environmentId).build();
        var createItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.forSubscriptionFormDefault(auditInfo))
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .portalPageContentId(content.getId())
            .validationConstraints(subscriptionForm.getValidationConstraints())
            .published(subscriptionForm.isEnabled())
            .visibility(PortalVisibility.PUBLIC)
            .build();
        var createdItem = navigationItemCrudService.create(PortalNavigationItem.from(createItem, organizationId, environmentId, null));

        return toSubscriptionForm((PortalNavigationSubscriptionForm) createdItem, subscriptionForm);
    }

    private String findOrganizationId(String environmentId) {
        try {
            return environmentRepository
                .findById(environmentId)
                .map(Environment::getOrganizationId)
                .orElseThrow(() -> new TechnicalDomainException("Unknown environment [%s]".formatted(environmentId)));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find environment [%s]".formatted(environmentId), e);
        }
    }

    private SubscriptionForm toSubscriptionForm(PortalNavigationSubscriptionForm navigationItem, SubscriptionForm source) {
        return SubscriptionForm.builder()
            .id(SubscriptionFormId.of(navigationItem.getId().toString()))
            .environmentId(navigationItem.getEnvironmentId())
            .gmdContent(source.getGmdContent())
            .enabled(Boolean.TRUE.equals(navigationItem.getPublished()))
            .validationConstraints(navigationItem.getValidationConstraints())
            .build();
    }
}
