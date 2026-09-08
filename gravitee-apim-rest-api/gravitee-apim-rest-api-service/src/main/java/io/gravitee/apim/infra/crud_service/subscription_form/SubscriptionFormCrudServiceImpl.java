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

import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.infra.adapter.SubscriptionFormAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of SubscriptionFormCrudService.
 *
 * <p>A subscription form is persisted as two records: the {@code subscription_forms} row (identity,
 * environment, enabled flag, validation constraints) and a {@link GraviteeMarkdownPageContent} holding
 * the form definition, so the GMD shares the same content machinery as every other portal page.</p>
 *
 * @author Gravitee.io Team
 */
@Component
public class SubscriptionFormCrudServiceImpl implements SubscriptionFormCrudService {

    private static final SubscriptionFormAdapter subscriptionFormAdapter = SubscriptionFormAdapter.INSTANCE;

    private final SubscriptionFormRepository subscriptionFormRepository;
    private final EnvironmentCrudService environmentCrudService;
    private final PortalPageContentCrudService pageContentCrudService;
    private final PortalPageContentQueryService pageContentQueryService;

    public SubscriptionFormCrudServiceImpl(
        @Lazy SubscriptionFormRepository subscriptionFormRepository,
        @Lazy EnvironmentCrudService environmentCrudService,
        @Lazy PortalPageContentCrudService pageContentCrudService,
        @Lazy PortalPageContentQueryService pageContentQueryService
    ) {
        this.subscriptionFormRepository = subscriptionFormRepository;
        this.environmentCrudService = environmentCrudService;
        this.pageContentCrudService = pageContentCrudService;
        this.pageContentQueryService = pageContentQueryService;
    }

    @Override
    public SubscriptionForm create(SubscriptionForm subscriptionForm) {
        var gmdContent = subscriptionForm.getGmdContent();
        var content = createPageContent(subscriptionForm.getEnvironmentId(), gmdContent);

        var toCreate = subscriptionFormAdapter.toRepository(subscriptionForm);
        if (toCreate.getId() == null) {
            toCreate.setId(SubscriptionFormId.random().toString());
        }
        toCreate.setPortalPageContentId(content.getId().toString());

        try {
            var result = subscriptionFormRepository.create(toCreate);
            return subscriptionFormAdapter.toEntity(result, gmdContent);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to create a SubscriptionForm for env: %s", toCreate.getEnvironmentId()),
                e
            );
        }
    }

    @Override
    public SubscriptionForm update(SubscriptionForm subscriptionForm) {
        var gmdContent = subscriptionForm.getGmdContent();
        var contentId = subscriptionForm.getPortalPageContentId();
        if (contentId == null) {
            // Legacy form whose definition is still stored inline (the boot-time migration did not reach
            // it): move the content out on first write, exactly as the upgrader would have.
            contentId = createPageContent(subscriptionForm.getEnvironmentId(), gmdContent).getId();
        } else {
            updatePageContent(contentId, gmdContent);
        }

        var toUpdate = subscriptionFormAdapter.toRepository(subscriptionForm);
        toUpdate.setPortalPageContentId(contentId.toString());

        try {
            var result = subscriptionFormRepository.update(toUpdate);
            return subscriptionFormAdapter.toEntity(result, gmdContent);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to update a SubscriptionForm with id: %s",
                    subscriptionForm.getId().toString()
                ),
                e
            );
        }
    }

    private GraviteeMarkdownPageContent createPageContent(String environmentId, GraviteeMarkdown gmdContent) {
        var environment = environmentCrudService.get(environmentId);
        var created = pageContentCrudService.create(
            new GraviteeMarkdownPageContent(PortalPageContentId.random(), environment.getOrganizationId(), environmentId, gmdContent)
        );
        return (GraviteeMarkdownPageContent) created;
    }

    private void updatePageContent(PortalPageContentId contentId, GraviteeMarkdown gmdContent) {
        var content = pageContentQueryService.findById(contentId).orElseThrow(() -> new PageContentNotFoundException(contentId.toString()));
        content.update(UpdatePortalPageContent.builder().content(gmdContent.value()).build());
        pageContentCrudService.update(content);
    }
}
