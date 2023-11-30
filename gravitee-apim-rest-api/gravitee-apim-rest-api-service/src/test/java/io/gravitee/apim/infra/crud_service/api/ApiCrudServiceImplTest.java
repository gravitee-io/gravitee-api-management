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
package io.gravitee.apim.infra.crud_service.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApiCrudServiceImplTest {

    ApiRepository apiRepository;

    ApiCrudServiceImpl service;

    private static final String API_ID = "api-id";

    @BeforeEach
    void setUp() {
        apiRepository = mock(ApiRepository.class);

        service = new ApiCrudServiceImpl(apiRepository);
    }

    @Nested
    class Get {

        @Test
        void should_get_an_api() throws TechnicalException {
            var existingApi = io.gravitee.repository.management.model.Api
                .builder()
                .id(API_ID)
                .definitionVersion(DefinitionVersion.V4)
                .definition(
                    """
                    {"id":"my-api","name":"My Api","type":"proxy","apiVersion":"1.0.0","definitionVersion":"4.0.0","tags":["tag1"],"listeners":[{"type":"http","entrypoints":[{"type":"http-proxy","qos":"auto","configuration":{}}],"paths":[{"path":"/http_proxy"}]}],"endpointGroups":[{"name":"default-group","type":"http-proxy","loadBalancer":{"type":"round-robin"},"sharedConfiguration":{},"endpoints":[{"name":"default-endpoint","type":"http-proxy","secondary":false,"weight":1,"inheritConfiguration":true,"configuration":{"target":"https://api.gravitee.io/echo"},"services":{}}],"services":{}}],"analytics":{"enabled":false},"flowExecution":{"mode":"default","matchRequired":false},"flows":[]}
                    """
                )
                .apiLifecycleState(ApiLifecycleState.PUBLISHED)
                .lifecycleState(LifecycleState.STARTED)
                .build();
            when(apiRepository.findById(API_ID)).thenReturn(Optional.of(existingApi));

            var result = service.get(API_ID);
            Assertions
                .assertThat(result)
                .extracting(Api::getId, Api::getApiLifecycleState, Api::getLifecycleState, Api::getDefinitionVersion)
                .containsExactly(
                    API_ID,
                    io.gravitee.apim.core.api.model.Api.ApiLifecycleState.PUBLISHED,
                    io.gravitee.apim.core.api.model.Api.LifecycleState.STARTED,
                    DefinitionVersion.V4
                );
        }

        @Test
        void should_throw_exception_if_api_not_found() throws TechnicalException {
            when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

            Assertions.assertThatThrownBy(() -> service.get(API_ID)).isInstanceOf(ApiNotFoundException.class);
        }
    }
}
