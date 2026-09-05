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

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.SUBSCRIPTION_FORM_TO_PORTAL_NAVIGATION_ITEM_UPGRADER;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.infra.adapter.PortalNavigationItemAdapter;
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
 * Migrates each legacy {@code subscription_forms} row into a {@code PortalPageContent}
 * (GRAVITEE_MARKDOWN) plus a {@code portal_navigation_items} row (area SUBSCRIPTION_FORM), so
 * the environment-default subscription form is served through the same nav-item machinery as
 * every other portal concept. The legacy table is left untouched here — nothing reads from the
 * new rows until the cutover that retires the legacy aggregate.
 *
 * <p>Idempotent per environment: an environment that already has a SUBSCRIPTION_FORM-area item
 * is skipped, so re-running after a partial failure only migrates what is still missing.</p>
 *
 * @author Gravitee.io Team
 */
@Component
@CustomLog
public class SubscriptionFormsToPortalNavigationItemsUpgrader implements Upgrader {

    private final SubscriptionFormRepository subscriptionFormRepository;
    private final EnvironmentRepository environmentRepository;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalPageContentCrudService pageContentCrudService;
    private final PortalNavigationItemDomainService portalNavigationItemDomainService;

    public SubscriptionFormsToPortalNavigationItemsUpgrader(
        @Lazy SubscriptionFormRepository subscriptionFormRepository,
        @Lazy EnvironmentRepository environmentRepository,
        PortalNavigationItemsQueryService portalNavigationItemsQueryService,
        PortalPageContentCrudService pageContentCrudService,
        PortalNavigationItemDomainService portalNavigationItemDomainService
    ) {
        this.subscriptionFormRepository = subscriptionFormRepository;
        this.environmentRepository = environmentRepository;
        this.portalNavigationItemsQueryService = portalNavigationItemsQueryService;
        this.pageContentCrudService = pageContentCrudService;
        this.portalNavigationItemDomainService = portalNavigationItemDomainService;
    }

    @Override
    public int getOrder() {
        return SUBSCRIPTION_FORM_TO_PORTAL_NAVIGATION_ITEM_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        var forms = subscriptionFormRepository.findAll();
        int migrated = 0;

        for (var form : forms) {
            if (migrateIfNeeded(form)) {
                migrated++;
            }
        }

        log.info("Subscription form to portal navigation item upgrader completed. Migrated {}/{} forms.", migrated, forms.size());
        return true;
    }

    private boolean migrateIfNeeded(SubscriptionForm form) throws TechnicalException {
        var environmentId = form.getEnvironmentId();
        var alreadyMigrated = !portalNavigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, PortalArea.SUBSCRIPTION_FORM)
            .isEmpty();
        if (alreadyMigrated) {
            return false;
        }

        var organizationId = environmentRepository.findById(environmentId).map(Environment::getOrganizationId).orElse(null);
        if (organizationId == null) {
            log.warn("Skipping subscription form migration: environment [{}] no longer exists", environmentId);
            return false;
        }

        try {
            createPageContentAndNavigationItem(form, organizationId, environmentId);
            return true;
        } catch (Exception e) {
            log.error("Failed to migrate subscription form for environment [{}]", environmentId, e);
            return false;
        }
    }

    private void createPageContentAndNavigationItem(SubscriptionForm form, String organizationId, String environmentId) {
        var content = pageContentCrudService.create(
            new GraviteeMarkdownPageContent(
                PortalPageContentId.random(),
                organizationId,
                environmentId,
                GraviteeMarkdown.of(form.getGmdContent())
            )
        );

        var auditInfo = AuditInfo.builder().organizationId(organizationId).environmentId(environmentId).build();
        var createItem = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.forSubscriptionFormDefault(auditInfo))
            .type(PortalNavigationItemType.SUBSCRIPTION_FORM)
            .title("Subscription Form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .portalPageContentId(content.getId())
            .validationConstraints(PortalNavigationItemAdapter.parseFieldConstraintsJson(form.getValidationConstraints()))
            .published(form.isEnabled())
            .visibility(PortalVisibility.PUBLIC)
            .build();

        portalNavigationItemDomainService.create(organizationId, environmentId, createItem);
    }
}
