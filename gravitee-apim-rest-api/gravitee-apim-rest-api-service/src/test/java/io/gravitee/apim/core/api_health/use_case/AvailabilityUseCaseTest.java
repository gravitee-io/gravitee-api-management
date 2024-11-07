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
package io.gravitee.apim.core.api_health.use_case;

import static fixtures.core.model.ApiFixtures.MY_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.withPrecision;

import fakes.FakeApiHealthQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_health.model.AvailabilityHealthCheck;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvailabilityUseCaseTest {

    public static final String ENV_ID = "environment-id";
    private final FakeApiHealthQueryService apiHealthQueryService = new FakeApiHealthQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();

    AvailabilityUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new AvailabilityUseCase(apiCrudServiceInMemory, apiHealthQueryService);
    }

    @AfterEach
    void tearDown() {
        apiHealthQueryService.reset();
        apiCrudServiceInMemory.reset();
    }

    @Test
    void nominal_case() {
        // Given
        var ctx = new ExecutionContext(GraviteeContext.getExecutionContext().getOrganizationId(), ENV_ID);
        var input = new AvailabilityUseCase.Input(ctx, Instant.now(), Instant.now(), "MyApiID", "gateway");
        var definition = io.gravitee.definition.model.v4.Api
            .builder()
            .type(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .build();
        apiCrudServiceInMemory.initWith(List.of(Api.builder().id("MyApiID").apiDefinitionHttpV4(definition).environmentId(ENV_ID).build()));
        apiHealthQueryService.availabilityHealthCheck = new AvailabilityHealthCheck(.12f, Map.of("gw", .12f));

        // When
        var output = cut.execute(input).blockingGet();

        // Then
        assertThat(output).isNotNull();
        assertThat(output.global()).isCloseTo(.12f, withPrecision(.0001f));
        assertThat(output.byField()).containsEntry("gw", .12f).containsOnlyKeys("gw");
    }

    @Test
    void not_found() {
        // Given
        var ctx = new ExecutionContext(GraviteeContext.getExecutionContext().getOrganizationId(), ENV_ID);
        var input = new AvailabilityUseCase.Input(ctx, Instant.now(), Instant.now(), "MyApiID", "gateway");
        var definition = io.gravitee.definition.model.v4.Api
            .builder()
            .type(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .build();
        apiCrudServiceInMemory.initWith(List.of(Api.builder().id("MyApiID").apiDefinitionHttpV4(definition).environmentId(ENV_ID).build()));

        // When
        var output = cut.execute(input).blockingGet();

        // Then
        assertThat(output).isNull();
    }

    @Test
    void should_throw_if_no_api_does_not_belong_to_current_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        var ctx = new ExecutionContext(GraviteeContext.getExecutionContext().getOrganizationId(), "other-env");
        assertThatThrownBy(() ->
                cut.execute(new AvailabilityUseCase.Input(ctx, Instant.now(), Instant.now(), MY_API, "gateway")).blockingGet()
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        var ctx = new ExecutionContext(GraviteeContext.getExecutionContext().getOrganizationId(), ENV_ID);
        assertThatThrownBy(() ->
                cut.execute(new AvailabilityUseCase.Input(ctx, Instant.now(), Instant.now(), MY_API, "gateway")).blockingGet()
            )
            .isInstanceOf(TcpProxyNotSupportedException.class)
            .hasMessage("TCP Proxy not supported");
    }
}
