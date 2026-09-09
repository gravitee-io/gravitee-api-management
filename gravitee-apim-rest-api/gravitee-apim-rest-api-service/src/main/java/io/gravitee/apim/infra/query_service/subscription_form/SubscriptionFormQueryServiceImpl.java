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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import io.gravitee.apim.infra.adapter.SubscriptionFormAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of SubscriptionFormQueryService.
 *
 * <p>Reads the {@code subscription_forms} row and resolves its definition from the referenced
 * {@link GraviteeMarkdownPageContent}. Rows that still carry their GMD inline (not yet migrated by
 * {@code SubscriptionFormPageContentUpgrader}) are served from that inline content.</p>
 *
 * @author Gravitee.io Team
 */
@Component
public class SubscriptionFormQueryServiceImpl implements SubscriptionFormQueryService {

    private static final SubscriptionFormAdapter subscriptionFormAdapter = SubscriptionFormAdapter.INSTANCE;

    private final SubscriptionFormRepository subscriptionFormRepository;
    private final PortalPageContentQueryService pageContentQueryService;

    public SubscriptionFormQueryServiceImpl(
        @Lazy SubscriptionFormRepository subscriptionFormRepository,
        PortalPageContentQueryService pageContentQueryService
    ) {
        this.subscriptionFormRepository = subscriptionFormRepository;
        this.pageContentQueryService = pageContentQueryService;
    }

    @Override
    public Optional<SubscriptionForm> findByIdAndEnvironmentId(String environmentId, SubscriptionFormId subscriptionFormId) {
        try {
            return subscriptionFormRepository.findByIdAndEnvironmentId(subscriptionFormId.toString(), environmentId).map(this::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to find a SubscriptionForm with id: %s in environment: %s",
                    subscriptionFormId,
                    environmentId
                ),
                e
            );
        }
    }

    @Override
    public List<SubscriptionForm> findAllByEnvironmentId(String environmentId) {
        try {
            return subscriptionFormRepository.findAllByEnvironmentId(environmentId).stream().map(this::toEntity).toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to list the SubscriptionForms of environment: %s", environmentId),
                e
            );
        }
    }

    @Override
    public Optional<SubscriptionForm> findByApiId(String environmentId, String apiId) {
        try {
            return subscriptionFormRepository.findByEnvironmentIdAndApiId(environmentId, apiId).map(this::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurred while trying to find the SubscriptionForm of API %s in environment: %s",
                    apiId,
                    environmentId
                ),
                e
            );
        }
    }

    @Override
    public Optional<SubscriptionForm> findDefaultForEnvironmentId(String environmentId) {
        try {
            return subscriptionFormRepository.findDefaultByEnvironmentId(environmentId).map(this::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while trying to find the default SubscriptionForm of environment: %s", environmentId),
                e
            );
        }
    }

    private SubscriptionForm toEntity(io.gravitee.repository.management.model.SubscriptionForm form) {
        return subscriptionFormAdapter.toEntity(form, resolveContent(form));
    }

    private GraviteeMarkdown resolveContent(io.gravitee.repository.management.model.SubscriptionForm form) {
        if (form.getPortalPageContentId() != null) {
            return loadContent(form.getId(), PortalPageContentId.of(form.getPortalPageContentId()));
        }
        if (form.getGmdContent() != null) {
            return GraviteeMarkdown.of(form.getGmdContent());
        }
        throw new TechnicalDomainException(
            String.format("SubscriptionForm %s has neither inline content nor a page content", form.getId())
        );
    }

    private GraviteeMarkdown loadContent(String subscriptionFormId, PortalPageContentId contentId) {
        var content = pageContentQueryService
            .findById(contentId)
            .orElseThrow(() ->
                new TechnicalDomainException(
                    String.format("SubscriptionForm %s references a missing page content: %s", subscriptionFormId, contentId)
                )
            );
        if (!(content instanceof GraviteeMarkdownPageContent gmdPageContent)) {
            throw new TechnicalDomainException(
                String.format(
                    "SubscriptionForm %s references page content %s of type %s instead of GRAVITEE_MARKDOWN",
                    subscriptionFormId,
                    contentId,
                    content.getType()
                )
            );
        }
        return gmdPageContent.getContent();
    }
}
