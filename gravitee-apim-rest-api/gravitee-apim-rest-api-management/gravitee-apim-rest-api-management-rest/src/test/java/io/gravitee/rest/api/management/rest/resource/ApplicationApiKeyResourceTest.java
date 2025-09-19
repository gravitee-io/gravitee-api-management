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
import static org.mockito.Mockito.reset;

import fixtures.core.model.ApiKeyFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
public class ApplicationApiKeyResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-app";
    private static final String APIKEY_ID = "my-apikey";

    @Autowired
    private ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/apikeys/" + APIKEY_ID;
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
    public void revoke_should_return_http_400_if_application_doesnt_use_shared_api_key() {
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.EXCLUSIVE).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(ApiKeyFixtures.anApiKey().toBuilder().id(APIKEY_ID).key("my-api-key-value").applicationId(APPLICATION_ID).build())
        );

        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
    }

    @Test
    public void revoke_should_return_http_400_if_application_doesnt_match_api_key() {
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.SHARED).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(ApiKeyFixtures.anApiKey().toBuilder().id(APIKEY_ID).key("my-api-key-value").applicationId("another-app").build())
        );

        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    public void revoke_should_revoke_and_return_http_204() {
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.SHARED).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .key("my-api-key-value")
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of())
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
}
