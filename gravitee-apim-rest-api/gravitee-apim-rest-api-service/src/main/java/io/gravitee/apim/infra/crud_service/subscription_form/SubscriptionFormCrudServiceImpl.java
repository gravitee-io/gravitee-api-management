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
 * the form definition, so the GMD shares the same content machinery as every other portal page. The two
 * writes are not transactional: a page content created for a row that could not be written is deleted
 * again, but a row update failing after its content was updated leaves the definition ahead of the
 * constraints derived from it until the next successful write.</p>
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
        EnvironmentCrudService environmentCrudService,
        PortalPageContentCrudService pageContentCrudService,
        PortalPageContentQueryService pageContentQueryService
    ) {
        this.subscriptionFormRepository = subscriptionFormRepository;
        this.environmentCrudService = environmentCrudService;
        this.pageContentCrudService = pageContentCrudService;
        this.pageContentQueryService = pageContentQueryService;
    }

    /**
     * Always creates a new page content for the definition: any {@code portalPageContentId} carried by
     * the given form is ignored.
     */
    @Override
    public SubscriptionForm create(SubscriptionForm subscriptionForm) {
        var gmdContent = subscriptionForm.getGmdContent();
        var contentId = createPageContent(subscriptionForm.getEnvironmentId(), gmdContent);

        var toCreate = subscriptionFormAdapter.toRepository(subscriptionForm);
        if (toCreate.getId() == null) {
            toCreate.setId(SubscriptionFormId.random().toString());
        }
        toCreate.setPortalPageContentId(contentId.toString());

        try {
            var result = subscriptionFormRepository.create(toCreate);
            return subscriptionFormAdapter.toEntity(result, gmdContent);
        } catch (TechnicalException e) {
            deleteQuietly(contentId, e);
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
        // A legacy form still stores its definition inline (the boot-time migration did not reach it):
        // move the content out on this first write, exactly as the upgrader would have.
        var migratingLegacyContent = contentId == null;
        if (migratingLegacyContent) {
            contentId = createPageContent(subscriptionForm.getEnvironmentId(), gmdContent);
        } else {
            updatePageContent(subscriptionForm.getId(), contentId, gmdContent);
        }

        var toUpdate = subscriptionFormAdapter.toRepository(subscriptionForm);
        toUpdate.setPortalPageContentId(contentId.toString());

        try {
            var result = subscriptionFormRepository.update(toUpdate);
            return subscriptionFormAdapter.toEntity(result, gmdContent);
        } catch (TechnicalException e) {
            if (migratingLegacyContent) {
                deleteQuietly(contentId, e);
            }
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to update a SubscriptionForm with id: %s",
                    subscriptionForm.getId().toString()
                ),
                e
            );
        }
    }

    private PortalPageContentId createPageContent(String environmentId, GraviteeMarkdown gmdContent) {
        var environment = environmentCrudService.get(environmentId);
        var created = pageContentCrudService.create(
            new GraviteeMarkdownPageContent(PortalPageContentId.random(), environment.getOrganizationId(), environmentId, gmdContent)
        );
        return created.getId();
    }

    /** Compensates a failed row write without masking its cause when the cleanup itself fails. */
    private void deleteQuietly(PortalPageContentId contentId, Exception cause) {
        try {
            pageContentCrudService.delete(contentId);
        } catch (RuntimeException cleanupFailure) {
            cause.addSuppressed(cleanupFailure);
        }
    }

    private void updatePageContent(SubscriptionFormId subscriptionFormId, PortalPageContentId contentId, GraviteeMarkdown gmdContent) {
        var content = pageContentQueryService
            .findById(contentId)
            .orElseThrow(() ->
                new TechnicalDomainException(
                    String.format("SubscriptionForm %s references a missing page content: %s", subscriptionFormId, contentId)
                )
            );
        content.update(UpdatePortalPageContent.builder().content(gmdContent.value()).build());
        pageContentCrudService.update(content);
    }
}
