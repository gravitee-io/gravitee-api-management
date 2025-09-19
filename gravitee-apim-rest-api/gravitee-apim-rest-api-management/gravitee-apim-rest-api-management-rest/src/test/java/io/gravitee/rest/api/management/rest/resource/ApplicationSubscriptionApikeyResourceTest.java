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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import fixtures.core.model.ApiKeyFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class ApplicationSubscriptionApikeyResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String APIKEY_ID = "my-apikey";

    @Autowired
    private ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/subscriptions/" + SUBSCRIPTION_ID + "/apikeys/" + APIKEY_ID;
    }

    @After
    public void tearDown() {
        Stream.of(apiKeyCrudServiceInMemory, applicationCrudServiceInMemory, subscriptionCrudServiceInMemory).forEach(
            InMemoryAlternative::reset
        );

        reset(apiKeyCrudServiceInMemory);
        GraviteeContext.cleanContext();
    }

    @Test
    public void delete_should_revoke_and_return_http_204() {
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
        );
        subscriptionCrudServiceInMemory.initWith(
            List.of(SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION_ID).applicationId(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .key("my-api-key-value")
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID))
                    .build()
            )
        );

        Response response = envTarget().request().delete();

        Assertions.assertThat(apiKeyCrudServiceInMemory.storage())
            .extracting(
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::getId,
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::isRevoked
            )
            .containsExactly(tuple(APIKEY_ID, true));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
    }

    @Test
    public void delete_should_return_http_400_when_apikey_on_another_subscription() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION_ID).applicationId(APPLICATION_ID).build())
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.EXCLUSIVE).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of("another-subscription"))
                    .build()
            )
        );

        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        Assertions.assertThat(apiKeyCrudServiceInMemory.storage())
            .extracting(
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::getId,
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::isRevoked
            )
            .containsExactly(tuple(APIKEY_ID, false));
    }

    @Test
    public void delete_should_return_http_500_on_exception() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION_ID).applicationId(APPLICATION_ID).build())
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.EXCLUSIVE).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID))
                    .build()
            )
        );
        doThrow(TechnicalManagementException.class).when(apiKeyCrudServiceInMemory).update(any());

        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void delete_should_return_http_404_if_application_not_found() {
        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void delete_should_return_http_400_if_application_found_has_shared_apiKey_mode() {
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.SHARED).id(APPLICATION_ID).build())
        );
        subscriptionCrudServiceInMemory.initWith(
            List.of(SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION_ID).applicationId(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID))
                    .build()
            )
        );

        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
    }
}
