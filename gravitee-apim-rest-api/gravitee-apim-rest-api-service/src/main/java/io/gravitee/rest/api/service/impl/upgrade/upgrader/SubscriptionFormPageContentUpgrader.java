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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.SUBSCRIPTION_FORM_PAGE_CONTENT_UPGRADER;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.SubscriptionForm;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Moves each subscription form's definition out of the inline {@code gmd_content} column into a
 * {@link GraviteeMarkdownPageContent}, and links the row to it through {@code portal_page_content_id}.
 *
 * <p>Idempotent: rows that already reference a page content are skipped, so re-running after a partial
 * failure only migrates what is still missing. A row whose migration fails keeps its inline content
 * and is still served from it (see {@code SubscriptionFormQueryServiceImpl}) until the next attempt.</p>
 *
 * @author Gravitee.io Team
 */
@Component
@CustomLog
public class SubscriptionFormPageContentUpgrader implements Upgrader {

    private final SubscriptionFormRepository subscriptionFormRepository;
    private final EnvironmentRepository environmentRepository;
    private final PortalPageContentCrudService pageContentCrudService;

    public SubscriptionFormPageContentUpgrader(
        @Lazy SubscriptionFormRepository subscriptionFormRepository,
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy PortalPageContentCrudService pageContentCrudService
    ) {
        this.subscriptionFormRepository = subscriptionFormRepository;
        this.environmentRepository = environmentRepository;
        this.pageContentCrudService = pageContentCrudService;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        var forms = subscriptionFormRepository.findAll();
        int migrated = 0;

        for (var form : forms) {
            if (form.getPortalPageContentId() == null && migrate(form)) {
                migrated++;
            }
        }

        log.info("Subscription form page content upgrader completed. Migrated {}/{} forms.", migrated, forms.size());
        return true;
    }

    private boolean migrate(SubscriptionForm form) throws TechnicalException {
        if (form.getGmdContent() == null) {
            log.warn("Skipping subscription form [{}] migration: it has neither inline content nor a page content", form.getId());
            return false;
        }

        var environmentId = form.getEnvironmentId();
        var organizationId = environmentRepository.findById(environmentId).map(Environment::getOrganizationId).orElse(null);
        if (organizationId == null) {
            log.warn("Skipping subscription form [{}] migration: environment [{}] no longer exists", form.getId(), environmentId);
            return false;
        }

        try {
            var content = pageContentCrudService.create(
                new GraviteeMarkdownPageContent(
                    PortalPageContentId.random(),
                    organizationId,
                    environmentId,
                    GraviteeMarkdown.of(form.getGmdContent())
                )
            );
            form.setPortalPageContentId(content.getId().toString());
            form.setGmdContent(null);
            subscriptionFormRepository.update(form);
            return true;
        } catch (Exception e) {
            log.error("Failed to move subscription form [{}] content to a page content", form.getId(), e);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_PAGE_CONTENT_UPGRADER;
    }
}
