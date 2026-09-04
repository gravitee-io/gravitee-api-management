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
package io.gravitee.apim.core.subscription_form.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import java.nio.charset.StandardCharsets;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Creates the default (unpublished) subscription form for an environment when none exists.
 * Idempotent: no-op if a {@code SUBSCRIPTION_FORM} navigation item is already present for the
 * environment, published or not.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class CreateDefaultSubscriptionFormUseCase {

    private static final String DEFAULT_FORM_TEMPLATE_PATH = "templates/default-subscription-form.md";

    private final EnvironmentCrudService environmentCrudService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalPageContentCrudService pageContentCrudService;
    private final SubscriptionFormSchemaGenerator schemaGenerator;

    public void execute(String environmentId) {
        var alreadyExists = !navigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalArea(environmentId, PortalArea.SUBSCRIPTION_FORM)
            .isEmpty();
        if (alreadyExists) {
            return;
        }

        var organizationId = environmentCrudService.get(environmentId).getOrganizationId();
        var gmd = GraviteeMarkdown.of(loadDefaultFormContent());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schemaGenerator.generate(gmd));

        var content = pageContentCrudService.create(
            new GraviteeMarkdownPageContent(PortalPageContentId.random(), organizationId, environmentId, gmd)
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
            .validationConstraints(constraints)
            .published(false)
            .visibility(PortalVisibility.PUBLIC)
            .build();
        navigationItemCrudService.create(PortalNavigationItem.from(createItem, organizationId, environmentId, null));

        log.info("Created default subscription form for environment [{}]", environmentId);
    }

    private String loadDefaultFormContent() {
        try (final var is = CreateDefaultSubscriptionFormUseCase.class.getClassLoader().getResourceAsStream(DEFAULT_FORM_TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Could not load default subscription form template: " + DEFAULT_FORM_TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Could not load default subscription form template", e);
        }
    }
}
