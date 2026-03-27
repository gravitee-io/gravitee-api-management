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

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionFormElResolverInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
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

    @Autowired
    private SubscriptionFormQueryServiceInMemory subscriptionFormQueryService;

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
                    .build()
            )
        );
    }

    @AfterEach
    void cleanUp() {
        GraviteeContext.cleanContext();
        subscriptionFormQueryService.reset();
        subscriptionFormElResolver.reset();
        portalNavigationItemsQueryService.reset();
    }

    @Test
    void should_return_200_with_subscription_form_and_resolved_options() {
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .environmentId(ENV_ID)
            .enabled(true)
            .gmdContent(GraviteeMarkdown.of("<gmd-select fieldKey=\"env\" options=\"{#api.metadata['envs']}:Prod,Test\"/>"))
            .build();
        subscriptionFormQueryService.initWith(List.of(form));
        subscriptionFormElResolver.withResolved(Map.of("{#api.metadata['envs']}", List.of("Dev", "Staging", "Prod")));

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var result = response.readEntity(SubscriptionForm.class);
        assertThat(result).isNotNull();
        assertThat(result.getGmdContent()).isEqualTo(form.getGmdContent().value());
        assertThat(result.getResolvedOptions()).containsEntry("env", List.of("Dev", "Staging", "Prod"));
    }

    @Test
    void should_return_404_when_form_not_found() {
        subscriptionFormQueryService.initWith(List.of());

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
                    .build()
            )
        );
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENV_ID).enabled(true).build();
        subscriptionFormQueryService.initWith(List.of(form));

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.getFirst().getCode()).isEqualTo("errors.notFound");
    }

    @Test
    void should_return_404_when_form_disabled() {
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENV_ID).enabled(false).build();
        subscriptionFormQueryService.initWith(List.of(form));

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_return_fallback_options_when_el_resolver_has_no_resolved_values() {
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .environmentId(ENV_ID)
            .enabled(true)
            .gmdContent(GraviteeMarkdown.of("<gmd-select fieldKey=\"plan\" options=\"{#api.metadata['plans']}:Free,Pro\"/>"))
            .build();
        subscriptionFormQueryService.initWith(List.of(form));
        // no resolved values → should fall back to "Free,Pro"

        Response response = target(API_ID + "/subscription-form").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var result = response.readEntity(SubscriptionForm.class);
        assertThat(result.getResolvedOptions()).containsEntry("plan", List.of("Free", "Pro"));
    }
}
