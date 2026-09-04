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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionFormElResolverInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.SubscriptionForm;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiSubscriptionFormResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";
    private static final String API_ID = "my-api-id";
    private static final String CONTENT_ID = "00000000-0000-0000-0000-000000000040";

    @Autowired
    private PortalPageContentQueryServiceInMemory portalPageContentQueryService;

    @Autowired
    private SubscriptionFormElResolverInMemory subscriptionFormElResolver;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @BeforeEach
    void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        portalNavigationItemsQueryService.initWith(
            List.of(
                PortalNavigationApi.builder()
                    .id(PortalNavigationItemId.random())
                    .organizationId("DEFAULT")
                    .environmentId(ENV_ID)
                    .title("Nav for " + API_ID)
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .apiId(API_ID)
                    .published(true)
                    .visibility(PortalVisibility.PUBLIC)
                    .segment(PortalNavigationItem.slugify("Nav for " + API_ID).value())
                    .build()
            )
        );
    }

    @AfterEach
    void cleanUp() {
        GraviteeContext.cleanContext();
        portalPageContentQueryService.reset();
        subscriptionFormElResolver.reset();
        portalNavigationItemsQueryService.reset();
    }

    @Test
    void should_return_200_with_resolved_options_only_no_content() {
        portalPageContentQueryService.initWith(
            List.of(dynamicSelectContent("<gmd-select fieldKey=\"env\" options=\"{#api.metadata['envs']}:Prod,Test\"/>"))
        );
        addPublishedSubscriptionFormNavItem();
        subscriptionFormElResolver.withResolved(Map.of("{#api.metadata['envs']}", List.of("Dev", "Staging", "Prod")));

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var result = response.readEntity(SubscriptionForm.class);
        assertThat(result).isNotNull();
        assertThat(result.getResolvedOptions()).containsEntry("env", List.of("Dev", "Staging", "Prod"));
    }

    @Test
    void should_return_404_when_form_not_found() {
        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_return_404_api_not_found_when_api_not_visible_in_portal_navigation() {
        portalNavigationItemsQueryService.initWith(
            List.of(
                PortalNavigationApi.builder()
                    .id(PortalNavigationItemId.random())
                    .organizationId("DEFAULT")
                    .environmentId(ENV_ID)
                    .title("Private " + API_ID)
                    .area(PortalArea.TOP_NAVBAR)
                    .order(0)
                    .apiId(API_ID)
                    .published(true)
                    .visibility(PortalVisibility.PRIVATE)
                    .segment(PortalNavigationItem.slugify("Private " + API_ID).value())
                    .build()
            )
        );
        portalPageContentQueryService.initWith(List.of(dynamicSelectContent("<p/>")));
        addPublishedSubscriptionFormNavItem();

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getCode()).isEqualTo("errors.notFound");
    }

    @Test
    void should_return_404_when_no_form_is_published() {
        portalPageContentQueryService.initWith(List.of(dynamicSelectContent("<p/>")));
        portalNavigationItemsQueryService.storage().add(subscriptionFormNavItem(false));

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_return_fallback_options_when_el_resolver_has_no_resolved_values() {
        portalPageContentQueryService.initWith(
            List.of(dynamicSelectContent("<gmd-select fieldKey=\"plan\" options=\"{#api.metadata['plans']}:Free,Pro\"/>"))
        );
        addPublishedSubscriptionFormNavItem();
        // no resolved values → should fall back to "Free,Pro"

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var result = response.readEntity(SubscriptionForm.class);
        assertThat(result.getResolvedOptions()).containsEntry("plan", List.of("Free", "Pro"));
    }

    private void addPublishedSubscriptionFormNavItem() {
        portalNavigationItemsQueryService.storage().add(subscriptionFormNavItem(true));
    }

    private PortalNavigationSubscriptionForm subscriptionFormNavItem(boolean published) {
        return PortalNavigationSubscriptionForm.builder()
            .id(PortalNavigationItemId.random())
            .organizationId("DEFAULT")
            .environmentId(ENV_ID)
            .title("Subscription Form")
            .segment("subscription-form")
            .area(PortalArea.SUBSCRIPTION_FORM)
            .order(0)
            .portalPageContentId(PortalPageContentId.of(CONTENT_ID))
            .validationConstraints(SubscriptionFormFieldConstraints.empty())
            .published(published)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private GraviteeMarkdownPageContent dynamicSelectContent(String content) {
        return new GraviteeMarkdownPageContent(PortalPageContentId.of(CONTENT_ID), "DEFAULT", ENV_ID, GraviteeMarkdown.of(content));
    }
}
