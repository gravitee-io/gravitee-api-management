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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
class SubscriptionFormResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";

    @Autowired
    private SubscriptionFormQueryServiceInMemory subscriptionFormQueryService;

    @Override
    protected String contextPath() {
        return "subscription-form";
    }

    @BeforeEach
    void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        GraviteeContext.cleanContext();
        subscriptionFormQueryService.reset();
        super.tearDown();
    }

    @Test
    void should_get_subscription_form_content() {
        SubscriptionForm enabledForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENV_ID).enabled(true).build();
        subscriptionFormQueryService.initWith(List.of(enabledForm));

        final Response response = target().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var result = response.readEntity(io.gravitee.rest.api.portal.rest.model.SubscriptionForm.class);
        assertNotNull(result);
        assertEquals(enabledForm.getGmdContent().value(), result.getGmdContent());
    }

    @Test
    void should_return_404_when_form_not_found() {
        subscriptionFormQueryService.initWith(List.of());

        final Response response = target().request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    void should_return_404_when_form_disabled() {
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().environmentId(ENV_ID).enabled(false).build();
        subscriptionFormQueryService.initWith(List.of(disabledForm));

        final Response response = target().request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
}
