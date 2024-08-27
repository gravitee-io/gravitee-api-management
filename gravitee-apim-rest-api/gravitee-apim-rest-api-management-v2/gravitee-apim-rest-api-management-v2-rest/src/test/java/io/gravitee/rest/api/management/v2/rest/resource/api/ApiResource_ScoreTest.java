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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;

import assertions.MAPIAssertions;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringTriggerResponse;
import io.gravitee.rest.api.management.v2.rest.model.ScoringStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiResource_ScoreTest extends ApiResourceTest {

    @Inject
    ApiCrudServiceInMemory apiCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_score";
    }

    @AfterEach
    @Override
    public void tearDown() {
        Stream.of(apiCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_404_if_not_found() {
        final Response response = rootTarget().request().post(null);
        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);

        MAPIAssertions.assertThat(response).hasStatus(NOT_FOUND_404).asError().hasHttpStatus(NOT_FOUND_404).hasMessage("Api not found.");
    }

    @Test
    void should_return_202_response() {
        apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().id(API).build()));

        final Response response = rootTarget().request().post(null);

        MAPIAssertions
            .assertThat(response)
            .hasStatus(ACCEPTED_202)
            .asEntity(ApiScoringTriggerResponse.class)
            .isEqualTo(ApiScoringTriggerResponse.builder().status(ScoringStatus.PENDING).build());
    }
}
